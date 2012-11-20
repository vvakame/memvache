package net.vvakame.memvache;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.vvakame.memvache.internal.SniffFuture;
import net.vvakame.memvache.internal.strategy.AggressiveQueryCacheStrategy;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

/**
 * Memvache core delegate.
 * @author vvakame
 */
public class MemvacheDelegate implements ApiProxy.Delegate<Environment> {

	static final ThreadLocal<MemvacheDelegate> localThis = new ThreadLocal<MemvacheDelegate>();

	final static Settings settings = Settings.getInstance();

	boolean queryCacheEnabled = true;

	final ApiProxy.Delegate<Environment> parent;


	/**
	 * 現在のスレッドに紐付いている {@link MemvacheDelegate} を取得する。
	 * @return スレッドに紐付く {@link MemvacheDelegate}
	 * @author vvakame
	 */
	public static MemvacheDelegate get() {
		return localThis.get();
	}

	/**
	 * {@link MemvacheDelegate}を{@link ApiProxy}に設定する。
	 * <p>
	 * 現在{@link ApiProxy}に設定されている
	 * {@link com.google.apphosting.api.ApiProxy.Delegate}が
	 * {@link MemvacheDelegate}だった場合は何もしない。
	 * </p>
	 * 
	 * @return 新たに作成した{@link MemvacheDelegate}か、 既に適用済みだった場合は元々設定されていた
	 *         {@link MemvacheDelegate}
	 */
	public static MemvacheDelegate install() {
		@SuppressWarnings("unchecked")
		Delegate<Environment> originalDelegate = ApiProxy.getDelegate();
		if (originalDelegate instanceof MemvacheDelegate == false) {
			MemvacheDelegate newDelegate = new MemvacheDelegate(originalDelegate);
			ApiProxy.setDelegate(newDelegate);
			localThis.set(newDelegate);
			return newDelegate;
		} else {
			return (MemvacheDelegate) originalDelegate;
		}
	}

	/**
	 * {@link MemvacheDelegate}を{@link ApiProxy}からはずす。
	 * 
	 * @param originalDelegate
	 *            元々設定されていた{@link com.google.apphosting.api.ApiProxy.Delegate}.
	 *            {@link MemvacheDelegate#getParent()}を使用すると良い。
	 */
	public static void uninstall(Delegate<Environment> originalDelegate) {
		localThis.set(null);
		ApiProxy.setDelegate(originalDelegate);
	}

	/**
	 * {@link MemvacheDelegate}を{@link ApiProxy}からはずす。
	 */
	public void uninstall() {
		localThis.set(null);
		ApiProxy.setDelegate(parent);
	}

	/**
	 * Queryをキャッシュする動作を現在処理中のリクエストに限り停止する。
	 * @author vvakame
	 */
	public static void queryCacheDisable() {
		get().queryCacheEnabled = false;
	}

	/**
	 * Queryをキャッシュする動作を現在処理中のリクエストに限り再開する。
	 * @author vvakame
	 */
	public static void queryCacheEnable() {
		get().queryCacheEnabled = true;
	}

	@Override
	public Future<byte[]> makeAsyncCall(Environment env, final String service, final String method,
			final byte[] requestBytes, ApiConfig config) {

		final AggressiveQueryCacheStrategy visitor = new AggressiveQueryCacheStrategy();
		byte[] data = visitor.preProcess(service, method, requestBytes);
		if (data != null) {
			return createFuture(data);
		}

		Future<byte[]> future =
				getParent().makeAsyncCall(env, service, method, requestBytes, config);
		return new SniffFuture<byte[]>(future) {

			@Override
			public void processDate(byte[] data) {
				visitor.postProcess(service, method, requestBytes, data);
			}
		};
	}

	@Override
	public byte[] makeSyncCall(Environment env, String service, String method, byte[] requestBytes)
			throws ApiProxyException {

		final AggressiveQueryCacheStrategy visitor = new AggressiveQueryCacheStrategy();
		byte[] data = visitor.preProcess(service, method, requestBytes);
		if (data != null) {
			return data;
		}

		byte[] response = getParent().makeSyncCall(env, service, method, requestBytes);
		visitor.postProcess(service, method, requestBytes, response);

		return response;
	}

	/**
	 * Namespaceがセット済みの {@link MemcacheService} を取得する。
	 * @return {@link MemcacheService}
	 * @author vvakame
	 */
	public static MemcacheService getMemcache() {
		return MemcacheServiceFactory.getMemcacheService("memvache");
	}

	/**
	 * 指定したデータを処理結果として返す {@link Future} を作成し返す。
	 * @param data 処理結果データ
	 * @return {@link Future}
	 * @author vvakame
	 */
	Future<byte[]> createFuture(final byte[] data) {
		return new Future<byte[]>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}

			@Override
			public byte[] get() {
				return data;
			}

			@Override
			public byte[] get(long timeout, TimeUnit unit) {
				return data;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean isDone() {
				return true;
			}
		};
	}

	/**
	 * 指定されたKindが予約済または除外指定のKindかどうかを調べて返す。
	 * @param kind 調べるKind
	 * @return 処理対象外か否か
	 * @author vvakame
	 */
	public static boolean isIgnoreKind(String kind) {
		if (kind.startsWith("__")) {
			return true;
		} else if (settings.getIgnoreKinds().contains(kind)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * the constructor.
	 * 
	 * @param delegate
	 * @category constructor
	 */
	MemvacheDelegate(Delegate<Environment> delegate) {
		this.parent = delegate;
	}

	@Override
	public void log(Environment env, LogRecord logRecord) {
		getParent().log(env, logRecord);
	}

	@Override
	public void flushLogs(Environment env) {
		getParent().flushLogs(env);
	}

	@Override
	public List<Thread> getRequestThreads(Environment env) {
		return getParent().getRequestThreads(env);
	}

	/**
	 * @return the parent
	 * @category accessor
	 */
	public ApiProxy.Delegate<Environment> getParent() {
		return parent;
	}
}
