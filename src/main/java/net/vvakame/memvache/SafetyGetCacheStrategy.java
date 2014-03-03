package net.vvakame.memvache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyTranslatorPublic;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.apphosting.api.DatastorePb.CommitResponse;
import com.google.apphosting.api.DatastorePb.DeleteRequest;
import com.google.apphosting.api.DatastorePb.GetRequest;
import com.google.apphosting.api.DatastorePb.GetResponse;
import com.google.apphosting.api.DatastorePb.PutRequest;
import com.google.apphosting.api.DatastorePb.PutResponse;
import com.google.apphosting.api.DatastorePb.Transaction;

/**
 * "Datastore への 単一 Entity の Get の置き換え" を実装するクラス。<br>
 * EntityがGetされる時はTx有りの時は素通し、それ以外の時はMemcacheを参照して無ければDatastoreへ。<br>
 * EntityがPutされる時はキャッシュを消すのみとします。<br>
 * {@link GetPutCacheStrategy} で動作が不安定な場合がGAE 1.8.9以降あったので、Pbの操作や変換を極力行わないようにしたもの。
 * @author vvakame
 */
public class SafetyGetCacheStrategy extends RpcVisitor {

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

	Map<Long, List<Key>> putUnderTx = new HashMap<Long, List<Key>>();


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
			for (Key key : requestKeys) {
				GetResponse.Entity entity = (GetResponse.Entity) all.get(key);
				if (entity == null || !entity.hasEntity() || !entity.getEntity().hasKey()) {
					continue;
				}
				Key cmpKey = KeyTranslatorPublic.createFromPb(entity.getEntity().getKey());
				if (!key.equals(cmpKey)) {
					// 指定と違うデータが取れることが…？
					logger.warning("invalid state in pre_datastore_v3_Get about " + key.toString()
							+ " - " + cmpKey);
					continue;
				}
				data.put(key, entity);
			}
		}

		// もし全部取れた場合は Get動作を行わず結果を構成して返す。
		if (requestKeys.size() == data.size()) {
			GetResponse responsePb = new GetResponse();
			requestPb.setAllowDeferred(true);
			// toByteArray() を呼んだ時にNPEが発生するのを抑制するために内部的に new ArrayList() させる
			responsePb.mutableEntitys();
			responsePb.mutableDeferreds();
			for (Key key : requestKeys) {
				GetResponse.Entity entity = data.get(key);
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
		List<GetResponse.Entity> entitys = responsePb.entitys();
		for (GetResponse.Entity entity : entitys) {
			Key key;
			if (entity.hasEntity() && entity.getEntity().hasKey()) {
				key = KeyTranslatorPublic.createFromPb(entity.getEntity().getKey());
			} else {
				key = KeyTranslatorPublic.createFromPb(entity.getKey());
			}
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
		// txに関係無くとりあえず消す
		MemcacheService memcache = MemvacheDelegate.getMemcache();
		List<Key> keys = PbKeyUtil.toKeys(responsePb.keys());
		memcache.deleteAll(keys);

		Transaction tx = requestPb.getTransaction();
		if (tx.hasApp()) {
			// Tx下の場合はDatastoreに反映されるまで、ローカル変数に結果を保持しておく。
			final long handle = tx.getHandle();
			if (putUnderTx.containsKey(handle)) {
				List<Key> list = putUnderTx.get(handle);
				list.addAll(keys);
			} else {
				putUnderTx.put(handle, keys);
			}
		}
		return null;
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

	@Override
	public byte[] post_datastore_v3_Commit(Transaction requestPb, CommitResponse responsePb) {
		final long handle = requestPb.getHandle();
		if (putUnderTx.containsKey(handle)) {
			List<Key> keys = putUnderTx.get(handle);
			MemvacheDelegate.getMemcache().deleteAll(keys);
		}
		return null;
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
