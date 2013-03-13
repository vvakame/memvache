package net.vvakame.memvache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.apphosting.api.DatastorePb.CommitResponse;
import com.google.apphosting.api.DatastorePb.DeleteRequest;
import com.google.apphosting.api.DatastorePb.GetRequest;
import com.google.apphosting.api.DatastorePb.GetResponse;
import com.google.apphosting.api.DatastorePb.GetResponse.Entity;
import com.google.apphosting.api.DatastorePb.PutRequest;
import com.google.apphosting.api.DatastorePb.PutResponse;
import com.google.apphosting.api.DatastorePb.Transaction;
import com.google.storage.onestore.v3.OnestoreEntity.EntityProto;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;

/**
 * "Datastore への 単一 Entity の Get & Put の置き換え" を実装するクラス。<br>
 * EntityがPutされる時は全てMemcacheに保持してDatastoreへ。<br>
 * EntityがGetされる時はTx有りの時は素通し、それ以外の時はMemcacheを参照して無ければDatastoreへ。
 * @author vvakame
 */
class GetPutCacheStrategy extends RpcVisitor {

	// TODO Asyncを絡めて2回Getした時 pre_Get, pre_Get, post_Get, post_Get という順で動くとバグるのではないか？

	/** オリジナルのリクエストが要求しているKeyの一覧 */
	List<Key> requestKeys;

	/** Memcacheが持っていたEntityのキャッシュ */
	Map<Key, Entity> data;

	Map<Long, Map<Key, Entity>> putUnderTx = new HashMap<Long, Map<Key, Entity>>();


	/**
	 * Getを行う前の動作として、Memcacheから解決できる要素について処理を行う。<br>
	 * Memcacheからの不足分のみでリクエストを再構成する。<br>
	 * もし、Tx下であったら全てを素通しする。<br>
	 * @return 何も処理をしなかった場合 null を返す。キャッシュから全て済ませた場合は {@link Pair} のFirst。requestPbを再構成した時は {@link Pair} のSecond。
	 */
	@Override
	public Pair<byte[], byte[]> pre_datastore_v3_Get(GetRequest requestPb) {
		if (requestPb.getTransaction().hasApp()) {
			// under transaction
			// 操作するEGに対してマークを付けさせるためにDatastoreに素通しする必要がある。
			return null;
		}

		final MemcacheService memcache = MemvacheDelegate.getMemcache();
		requestKeys = PbKeyUtil.toKeys(requestPb.keys());

		// Memcacheにあるものはキャッシュで済ませる
		{
			Map<Key, Object> all = memcache.getAll(requestKeys); // 存在しなかった場合Keyごと無い
			data = new HashMap<Key, Entity>();
			for (Key key : all.keySet()) {
				data.put(key, (Entity) all.get(key));
			}
		}

		// もし全部取れた場合は Get動作を行わず結果を構成して返す。
		if (requestKeys.size() == data.size()) {
			GetResponse responsePb = new GetResponse();
			for (Key key : requestKeys) {
				Entity entity = data.get(key);
				responsePb.addEntity(entity);
			}

			return Pair.response(responsePb.toByteArray());
		}

		// MemcacheにないものだけPbを再構成して投げる
		for (int i = requestKeys.size() - 1; 0 <= i; i--) {
			if (data.containsKey(requestKeys.get(i))) {
				requestPb.removeKey(i);
			}
		}

		return Pair.request(requestPb.toByteArray());
	}

	/**
	 * Getを行った後の動作として、前処理で抜いた分のリクエストと実際にRPCした結果をマージし返す。<br>
	 * また、RPCして得られた結果についてMemcacheにキャッシュを作成する。
	 * @return 処理結果 or null
	 */
	@Override
	public byte[] post_datastore_v3_Get(GetRequest requestPb, GetResponse responsePb) {
		if (requestPb.getTransaction().hasApp()) {
			// under transaction
			return null;
		}

		// Memcacheに蓄える
		Map<Key, Entity> newMap = new HashMap<Key, Entity>();
		List<Reference> keys = requestPb.keys();
		List<Entity> entitys = responsePb.entitys();

		for (int i = 0; i < entitys.size(); i++) {
			Key key = PbKeyUtil.toKey(keys.get(i));
			Entity entity = entitys.get(i);
			newMap.put(key, entity);
		}
		MemcacheService memcache = MemvacheDelegate.getMemcache();
		memcache.putAll(newMap);

		// ここで取れてきているのはキャッシュにないヤツだけなので再構成して返す必要がある
		data.putAll(newMap);
		responsePb.clearEntity();
		for (Key key : requestKeys) {
			responsePb.addEntity(data.get(key));
		}

		return responsePb.toByteArray();
	}

	/**
	 * Putを行った後の動作として、Memcacheにキャッシュを作成する。
	 */
	@Override
	public byte[] post_datastore_v3_Put(PutRequest requestPb, PutResponse responsePb) {
		Transaction tx = requestPb.getTransaction();
		if (tx.hasApp()) {
			// Tx下の場合はDatastoreに反映されるまで、ローカル変数に結果を保持しておく。
			final long handle = tx.getHandle();
			Map<Key, Entity> newMap = extractCache(requestPb);
			if (putUnderTx.containsKey(handle)) {
				Map<Key, Entity> cached = putUnderTx.get(handle);
				cached.putAll(newMap);
			} else {
				putUnderTx.put(handle, newMap);
			}
		} else {
			MemcacheService memcache = MemvacheDelegate.getMemcache();
			Map<Key, Entity> newMap = extractCache(requestPb);
			memcache.putAll(newMap);
		}
		return null;
	}

	private Map<Key, Entity> extractCache(PutRequest requestPb) {
		Map<Key, Entity> newMap = new HashMap<Key, Entity>();
		for (EntityProto proto : requestPb.entitys()) {
			Key key = PbKeyUtil.toKey(proto.getKey());
			Entity entity = new Entity();
			entity.setEntity(proto);
			newMap.put(key, entity);
		}
		return newMap;
	}

	/**
	 * Deleteを行う前の動作として、とりあえずMemcacheからキャッシュを削除する。
	 */
	@Override
	public Pair<byte[], byte[]> pre_datastore_v3_Delete(DeleteRequest requestPb) {
		List<Key> keys = PbKeyUtil.toKeys(requestPb.keys());
		MemcacheService memcache = MemvacheDelegate.getMemcache();
		memcache.deleteAll(keys);

		return null;
	}

	/**
	 * Commitを行った後の動作として、Putした時のキャッシュが存在していればMemcacheにキャッシュを作成する。
	 */
	@Override
	public byte[] post_datastore_v3_Commit(Transaction requestPb, CommitResponse responsePb) {
		final long handle = requestPb.getHandle();
		if (putUnderTx.containsKey(handle)) {
			Map<Key, Entity> map = putUnderTx.get(handle);
			MemvacheDelegate.getMemcache().putAll(map);
			return null;
		} else {
			return null;
		}
	}

	/**
	 * Rollbackを行った後の動作として、Putした時のキャッシュが存在していればなかった事にする。
	 */
	@Override
	public byte[] post_datastore_v3_Rollback(Transaction requestPb, CommitResponse responsePb) {
		final long handle = requestPb.getHandle();
		if (putUnderTx.containsKey(handle)) {
			putUnderTx.remove(handle);
		}
		return null;
	}
}
