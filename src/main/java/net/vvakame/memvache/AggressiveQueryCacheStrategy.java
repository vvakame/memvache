package net.vvakame.memvache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.apphosting.api.DatastorePb.NextRequest;
import com.google.apphosting.api.DatastorePb.PutRequest;
import com.google.apphosting.api.DatastorePb.Query;
import com.google.apphosting.api.DatastorePb.QueryResult;
import com.google.storage.onestore.v3.OnestoreEntity.EntityProto;
import com.google.storage.onestore.v3.OnestoreEntity.Path;
import com.google.storage.onestore.v3.OnestoreEntity.Path.Element;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;

/**
 * "Datastore への Query をまるごとキャッシュする" を実装するクラス。
 * @author vvakame
 */
class AggressiveQueryCacheStrategy extends RpcVisitor {

	final static Settings settings = Settings.getInstance();


	/**
	 * RunQueryが行われた時の前処理として、キャッシュがあればそれを返す。
	 * @param requestPb RunQueryのQueryそのもの
	 * @return キャッシュされていた値 or null
	 * @author vvakame
	 */
	@Override
	public Pair<byte[], byte[]> pre_datastore_v3_RunQuery(Query requestPb) {
		if (isIgnoreKind(requestPb.getKind())) {
			return null;
		}

		// datastore_v3#Next を回避するためにprefetchSizeが設定されていない場合大きめに設定する。
		if (requestPb.getCount() == 0) {
			requestPb.setCount(1000);
		}

		final MemcacheService memcache = MemvacheDelegate.getMemcache();
		String memcacheKey = MemcacheKeyUtil.createQueryKey(memcache, requestPb);

		QueryResult response = (QueryResult) memcache.get(memcacheKey);
		if (response != null) {
			return Pair.response(response.toByteArray());
		} else {
			return Pair.request(requestPb.toByteArray());
		}
	}

	/**
	 * RunQueryが行われた時の後処理として、キャッシュを作成する。
	 * @param requestPb RunQueryのQueryそのもの
	 * @param responsePb RunQueryのQueryResultそのもの
	 * @return 常に null
	 * @author vvakame
	 */
	@Override
	public byte[] post_datastore_v3_RunQuery(Query requestPb, QueryResult responsePb) {
		if (isIgnoreKind(requestPb.getKind())) {
			return null;
		}

		final MemcacheService memcache = MemvacheDelegate.getMemcache();
		String memcacheKey = MemcacheKeyUtil.createQueryKey(memcache, requestPb);

		// 最大5分しかキャッシュしないようにする
		Expiration expiration = Expiration.byDeltaSeconds(settings.getExpireSecond());
		memcache.put(memcacheKey, responsePb, expiration);
		return null;
	}

	/**
	 * DatastoreにPutされたKindについてカウンタをインクリメントし、Queryのキャッシュを参照不可にする。
	 * @param requestPb
	 * @return 常に null
	 * @author vvakame
	 */
	@Override
	public Pair<byte[], byte[]> pre_datastore_v3_Put(PutRequest requestPb) {
		final MemcacheService memcache = MemvacheDelegate.getMemcache();
		final Set<String> memcacheKeys = new HashSet<String>();

		final StringBuilder builder = new StringBuilder();
		for (EntityProto entity : requestPb.entitys()) {
			final Reference key = entity.getMutableKey();
			final String namespace = key.getNameSpace();
			final Path path = key.getPath();
			// elements が並んでいるのは親Keyなどがある場合
			// 配列の添字の若い方 = より祖先 末尾 = 本体 末尾のKindを見れば無視すべきかわかる
			List<Element> elements = path.mutableElements();
			Element element = elements.get(elements.size() - 1);
			final String kind = element.getType();
			if (isIgnoreKind(kind)) {
				continue;
			}

			builder.setLength(0);
			String memcacheKey = MemcacheKeyUtil.createKindKey(builder, namespace, kind);

			memcacheKeys.add(memcacheKey);
		}
		// memcache.incrementAll(memcacheKeys, 1, 0L);
		// broken method ↑
		for (String key : memcacheKeys) {
			memcache.increment(key, 1, 0L);
		}

		return null;
	}

	/**
	 * 指定されたKindが予約済またはKindlessQueryまたは除外指定のKindかどうかを調べて返す。
	 * @param kind 調べるKind
	 * @return 処理対象外か否か
	 * @author vvakame
	 */
	public static boolean isIgnoreKind(String kind) {
		if (kind.startsWith("__")) {
			return true;
		} else if ("".equals(kind)) {
			return true;
		} else if (settings.getIgnoreKinds().contains(kind)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Pair<byte[], byte[]> pre_datastore_v3_Next(NextRequest requestPb) {
		return super.pre_datastore_v3_Next(requestPb);
	}

	@Override
	public byte[] post_datastore_v3_Next(NextRequest requestPb, QueryResult responsePb) {
		return super.post_datastore_v3_Next(requestPb, responsePb);
	}


	/**
	 * ユーザが行うMemvacheの設定を読み取る。<br>
	 * 主に、 {@link AggressiveQueryCacheStrategy} に影響をおよぼす。
	 * @author vvakame
	 */
	static class Settings {

		static final Logger logger = Logger.getLogger(Settings.class.getName());

		/** 超積極的にクエリをキャッシュした時のMemcache保持秒数 */
		int expireSecond = 300;

		/** Queryをキャッシュ"しない"Kindの一覧 */
		Set<String> ignoreKinds = new HashSet<String>();

		static Settings singleton;


		/**
		 * インスタンスを取得する。
		 * @return インスタンス
		 * @author vvakame
		 */
		public static Settings getInstance() {
			if (singleton == null) {
				singleton = new Settings();
			}
			return singleton;
		}

		Settings() {
			Properties properties = new Properties();
			try {
				InputStream is = Settings.class.getResourceAsStream("/memvache.properties");
				if (is == null) {
					return;
				}
				properties.load(is);

				String expireSecondStr = properties.getProperty("expireSecond");
				if (expireSecondStr != null && !"".equals(expireSecondStr)) {
					expireSecond = Integer.parseInt(expireSecondStr);
				}

				String ignoreKindStr = properties.getProperty("ignoreKind");
				if (ignoreKindStr != null && !"".equals(ignoreKindStr)) {
					ignoreKinds = new HashSet<String>(Arrays.asList(ignoreKindStr.split(",")));
				} else {
					ignoreKinds = new HashSet<String>();
				}
			} catch (IOException e) {
				logger.log(Level.INFO, "", e);
			}
		}

		/**
		 * @return the expireSecond
		 * @category accessor
		 */
		public int getExpireSecond() {
			return expireSecond;
		}

		/**
		 * @param expireSecond the expireSecond to set
		 * @category accessor
		 */
		public void setExpireSecond(int expireSecond) {
			this.expireSecond = expireSecond;
		}

		/**
		 * @return the ignoreKinds
		 * @category accessor
		 */
		public Set<String> getIgnoreKinds() {
			return ignoreKinds;
		}

		/**
		 * @param ignoreKinds the ignoreKinds to set
		 * @category accessor
		 */
		public void setIgnoreKinds(Set<String> ignoreKinds) {
			this.ignoreKinds = ignoreKinds;
		}
	}
}
