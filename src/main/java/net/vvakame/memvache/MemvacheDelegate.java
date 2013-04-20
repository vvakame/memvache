package net.vvakame.memvache;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

/**
 * Memvache のコアとなる {@link Delegate}。<br>
 * 1リクエスト中ではStrategyのインスタンス再生成は行わず使いまわす。
 * @author vvakame
 */
public class MemvacheDelegate implements ApiProxy.Delegate<Environment> {

	static final Logger logger = Logger.getLogger(MemvacheDelegate.class.getName());

	static final ThreadLocal<MemvacheDelegate> localThis = new ThreadLocal<MemvacheDelegate>();

	final ApiProxy.Delegate<Environment> parent;

	ThreadLocal<List<Strategy>> strategies = new ThreadLocal<List<Strategy>>();

	static Set<Class<? extends Strategy>> enabledStrategies =
			new LinkedHashSet<Class<? extends Strategy>>();

	static {
		staticInitialize();
	}


	static void staticInitialize() {
		enabledStrategies.clear();
		addStrategy(AggressiveQueryCacheStrategy.class);
		addStrategy(QueryKeysOnlyStrategy.class);
		addStrategy(GetPutCacheStrategy.class);
		RpcVisitor.debug = false;
	}

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
			setupStrategies(newDelegate);
			ApiProxy.setDelegate(newDelegate);
			localThis.set(newDelegate);
			return newDelegate;
		} else {
			MemvacheDelegate delegate = (MemvacheDelegate) originalDelegate;
			setupStrategies(delegate);
			return delegate;
		}
	}

	/**
	 * キャッシュに利用する戦略を追加する。
	 * @param clazz 追加する戦略
	 * @author vvakame
	 */
	public static void addStrategy(Class<? extends Strategy> clazz) {
		enabledStrategies.add(clazz);
	}

	/**
	 * キャッシュに利用する戦略を削除する。
	 * @param clazz 削除する戦略
	 * @author vvakame
	 */
	public static void removeStrategy(Class<? extends Strategy> clazz) {
		enabledStrategies.remove(clazz);
	}

	static void setupStrategies(MemvacheDelegate memvache) {
		List<Strategy> strategies = memvache.strategies.get();
		if (strategies == null) {
			strategies = new ArrayList<Strategy>(3);
			memvache.strategies.set(strategies);
		} else {
			strategies.clear();
		}
		try {
			for (Class<? extends Strategy> clazz : enabledStrategies) {
				strategies.add(clazz.newInstance());
			}
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
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

	@Override
	public Future<byte[]> makeAsyncCall(Environment env, final String service, final String method,
			final byte[] requestBytes, ApiConfig config) {

		return processAsyncCall(env, service, method, requestBytes, config, 0);
	}

	Future<byte[]> processAsyncCall(Environment env, final String service, final String method,
			final byte[] requestBytes, ApiConfig config, int depth) {
		List<Strategy> strategies = this.strategies.get();

		// 適用すべき戦略がなかったら実際のRPCを行う
		if (strategies.size() == depth) {
			return getParent().makeAsyncCall(env, service, method, requestBytes, config);
		}

		final Strategy strategy = strategies.get(depth);

		// responseが生成されていたらそっちを結果として返す
		final Pair<byte[], byte[]> pair = strategy.preProcess(service, method, requestBytes);
		if (pair != null && pair.response != null) {
			return createFuture(pair.response);
		}

		// 次の戦略を適用する。もしリクエストが改変されてたらそっちを渡す。
		Future<byte[]> response;
		if (pair != null && pair.request != null) {
			response = processAsyncCall(env, service, method, pair.request, config, depth + 1);
		} else {
			response = processAsyncCall(env, service, method, requestBytes, config, depth + 1);
		}

		// responseが改変されてたらそっちを結果として返す
		return new SniffFuture<byte[]>(response) {

			@Override
			public byte[] processDate(byte[] data) {
				byte[] modified;
				if (pair != null && pair.request != null) {
					modified = strategy.postProcess(service, method, pair.request, data);
				} else {
					modified = strategy.postProcess(service, method, requestBytes, data);
				}

				if (modified != null) {
					return modified;
				} else {
					return data;
				}
			}
		};
	}

	@Override
	public byte[] makeSyncCall(Environment env, String service, String method, byte[] requestBytes)
			throws ApiProxyException {

		return processSyncCall(env, service, method, requestBytes, 0);
	}

	byte[] processSyncCall(Environment env, String service, String method, byte[] requestBytes,
			int depth) {
		List<Strategy> strategies = this.strategies.get();

		// 適用すべき戦略がなかったら実際のRPCを行う
		if (strategies.size() == depth) {
			return getParent().makeSyncCall(env, service, method, requestBytes);
		}

		Strategy strategy = strategies.get(depth);

		// responseが生成されていたらそっちを結果として返す
		Pair<byte[], byte[]> pair = strategy.preProcess(service, method, requestBytes);
		if (pair != null && pair.response != null) {
			return pair.response;
		}

		// 次の戦略を適用する。もしリクエストが改変されてたらそっちを渡す。
		byte[] response;
		if (pair != null && pair.request != null) {
			response = processSyncCall(env, service, method, pair.request, depth + 1);
		} else {
			response = processSyncCall(env, service, method, requestBytes, depth + 1);
		}

		// responseが改変されてたらそっちを結果として返す
		byte[] modified = strategy.postProcess(service, method, requestBytes, response);
		if (modified != null) {
			return modified;
		} else {
			return response;
		}
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
