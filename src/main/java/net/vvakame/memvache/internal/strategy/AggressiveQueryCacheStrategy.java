package net.vvakame.memvache.internal.strategy;

import java.util.HashSet;
import java.util.Set;

import net.vvakame.memvache.MemvacheDelegate;
import net.vvakame.memvache.Settings;
import net.vvakame.memvache.internal.RpcVisitor;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.apphosting.api.DatastorePb.PutRequest;
import com.google.apphosting.api.DatastorePb.Query;
import com.google.apphosting.api.DatastorePb.QueryResult;
import com.google.storage.onestore.v3.OnestoreEntity.EntityProto;
import com.google.storage.onestore.v3.OnestoreEntity.Path;
import com.google.storage.onestore.v3.OnestoreEntity.Path.Element;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;

/**
 * Datastore への Query をまるごとキャッシュする を実装するクラス。
 * @author vvakame
 */
public class AggressiveQueryCacheStrategy extends RpcVisitor {

	/**
	 * RunQueryが行われた時の前処理として、キャッシュがあればそれを返す。
	 * @param requestPb RunQueryのQueryそのもの
	 * @return キャッシュされていた値 or null
	 * @author vvakame
	 */
	@Override
	public byte[] pre_datastore_v3_RunQuery(Query requestPb) {
		final MemcacheService memcache = MemvacheDelegate.getMemcache();

		final String kind = requestPb.getKind();
		if (MemvacheDelegate.isIgnoreKind(kind)) {
			return null;
		}
		final String namespace = requestPb.getNameSpace();

		StringBuilder builder = new StringBuilder();
		builder.append(namespace).append("@");
		builder.append(kind);

		final long counter;
		{
			Object obj = memcache.get(builder.toString());
			if (obj == null) {
				counter = 0;
			} else {
				counter = (Long) obj;
			}
		}
		builder.append("@").append(requestPb.hashCode());
		builder.append("@").append(counter);

		return (byte[]) memcache.get(builder.toString());
	}

	/**
	 * RunQueryが行われた時の後処理として、キャッシュを作成する。
	 * @param requestPb RunQueryのQueryそのもの
	 * @param responsePb RunQueryのQueryそのもの
	 * @return キャッシュを作成したか、しないか
	 * @author vvakame
	 */
	@Override
	public boolean post_datastore_v3_RunQuery(Query requestPb, QueryResult responsePb) {
		final String kind = requestPb.getKind();
		if (MemvacheDelegate.isIgnoreKind(kind)) {
			return false;
		}
		final MemcacheService memcache = MemvacheDelegate.getMemcache();
		final String namespace = requestPb.getNameSpace();

		StringBuilder builder = new StringBuilder();
		builder.append(namespace).append("@");
		builder.append(kind);

		final long counter;
		{
			Object obj = memcache.get(builder.toString());
			if (obj == null) {
				counter = 0;
			} else {
				counter = (Long) obj;
			}
		}
		builder.append("@").append(requestPb.hashCode());
		builder.append("@").append(counter);

		// 最大5分しかキャッシュしないようにする
		Expiration expiration = Expiration.byDeltaSeconds(Settings.getInstance().getExpireSecond());
		memcache.put(builder.toString(), responsePb.toByteArray(), expiration);
		return true;
	}

	/**
	 * DatastoreにPutされたKindについてカウンタをインクリメントし、Queryのキャッシュを参照不可にする。
	 * @param pb
	 * @return 常に null
	 * @author vvakame
	 */
	@Override
	public byte[] pre_datastore_v3_Put(PutRequest pb) {
		final MemcacheService memcache = MemvacheDelegate.getMemcache();
		final Set<String> memcacheKeys = new HashSet<String>();

		for (EntityProto entity : pb.entitys()) {
			final Reference key = entity.getMutableKey();
			final String namespace = key.getNameSpace();
			final Path path = key.getPath();
			for (Element element : path.mutableElements()) {
				final String kind = element.getType();

				if (MemvacheDelegate.isIgnoreKind(kind)) {
					continue;
				}

				StringBuilder builder = new StringBuilder();
				builder.append(namespace).append("@");
				builder.append(kind);

				memcacheKeys.add(builder.toString());
			}
		}
		// memcache.incrementAll(memcacheKeys, 1, 0L);
		// TODO is this broken method? ↑
		for (String key : memcacheKeys) {
			memcache.increment(key, 1, 0L);
		}

		return null;
	}
}
