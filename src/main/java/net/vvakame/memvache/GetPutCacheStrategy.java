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
public class GetPutCacheStrategy extends RpcVisitor {

	static final int PRIORITY = QueryKeysOnlyStrategy.PRIORITY + 1000;


	@Override
	public int getPriority() {
		return PRIORITY;
	}


	/** オリジナルのリクエストが要求しているKeyの一覧, リクエスト毎 */
	Map<GetRequest, List<Key>> requestKeysMap = new HashMap<GetRequest, List<Key>>();

	/** Memcacheが持っていたEntityのキャッシュ, リクエスト毎 */
	Map<GetRequest, Map<Key, GetResponse.Entity>> dataMap =
			new HashMap<GetRequest, Map<Key, GetResponse.Entity>>();

	/** 同一操作を行ったカウント数, リクエスト毎 */
	Map<GetRequest, Integer> requestCountMap = new HashMap<GetRequest, Integer>();

	Map<Long, Map<Key, GetResponse.Entity>> putUnderTx =
			new HashMap<Long, Map<Key, GetResponse.Entity>>();


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

		List<Key> requestKeys = PbKeyUtil.toKeys(requestPb.keys());
		Map<Key, GetResponse.Entity> data = new HashMap<Key, GetResponse.Entity>();

		// Memcacheにあるものはキャッシュで済ませる
		{
			final MemcacheService memcache = MemvacheDelegate.getMemcache();
			Map<Key, Object> all = memcache.getAll(requestKeys); // 存在しなかった場合Keyごと無い
			for (Key key : all.keySet()) {
				GetResponse.Entity entity = (GetResponse.Entity) all.get(key);
				if (entity != null) {
					data.put(key, entity);
				}
			}
		}

		// もし全部取れた場合は Get動作を行わず結果を構成して返す。
		if (requestKeys.size() == data.size()) {
			GetResponse responsePb = new GetResponse();
			// toByteArray() を呼んだ時にNPEが発生するのを抑制するために内部的に new ArrayList() させる
			responsePb.mutableEntitys();
			responsePb.mutableDeferreds();
			for (Key key : requestKeys) {
				GetResponse.Entity entity = data.get(key);
				if (entity == null) {
					data.remove(key);
					continue;
				}
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

		// post_datastore_v3_Getで渡されるrequestPbは再構成後のものなので
		byte[] reconstructured = requestPb.toByteArray();
		{
			GetRequest reconstRequest = new GetRequest();
			reconstRequest.mergeFrom(reconstructured);
			requestKeysMap.put(reconstRequest, requestKeys);
			dataMap.put(reconstRequest, data);
			Integer count = requestCountMap.get(reconstRequest);
			if (count == null) {
				count = 1;
			} else {
				count += 1;
			}
			requestCountMap.put(reconstRequest, count);
		}

		return Pair.request(reconstructured);
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
		Map<Key, GetResponse.Entity> newMap = new HashMap<Key, GetResponse.Entity>();
		List<Reference> keys = requestPb.keys();
		List<GetResponse.Entity> entitys = responsePb.entitys();

		for (int i = 0; i < entitys.size(); i++) {
			Key key = PbKeyUtil.toKey(keys.get(i));
			GetResponse.Entity entity = entitys.get(i);
			newMap.put(key, entity);
		}
		MemcacheService memcache = MemvacheDelegate.getMemcache();
		memcache.putAll(newMap);

		// ここで取れてきているのはキャッシュにないヤツだけなので再構成して返す必要がある
		Map<Key, GetResponse.Entity> data;
		List<Key> requestKeys;
		{
			Integer count = requestCountMap.get(requestPb);
			if (count == 1) {
				data = dataMap.remove(requestPb);
				requestKeys = requestKeysMap.remove(requestPb);
				requestCountMap.put(requestPb, 0);
			} else {
				data = dataMap.get(requestPb);
				requestKeys = requestKeysMap.get(requestPb);
				requestCountMap.put(requestPb, count - 1);
			}
		}
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
			Map<Key, GetResponse.Entity> newMap = extractCache(requestPb, responsePb);
			if (putUnderTx.containsKey(handle)) {
				Map<Key, GetResponse.Entity> cached = putUnderTx.get(handle);
				cached.putAll(newMap);
			} else {
				putUnderTx.put(handle, newMap);
			}
		} else {
			MemcacheService memcache = MemvacheDelegate.getMemcache();
			Map<Key, GetResponse.Entity> newMap = extractCache(requestPb, responsePb);
			memcache.putAll(newMap);
		}
		return null;
	}

	private Map<Key, GetResponse.Entity> extractCache(PutRequest requestPb, PutResponse responsePb) {
		Map<Key, GetResponse.Entity> newMap = new HashMap<Key, GetResponse.Entity>();
		int size = requestPb.entitySize();
		List<EntityProto> entitys = requestPb.entitys();
		for (int i = 0; i < size; i++) {
			EntityProto proto = entitys.get(i);
			Reference reference = responsePb.getKey(i);
			Key key = PbKeyUtil.toKey(reference);
			GetResponse.Entity entity = new GetResponse.Entity();
			entity.setEntity(proto);
			entity.setKey(reference);
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
			Map<Key, GetResponse.Entity> map = putUnderTx.get(handle);
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
