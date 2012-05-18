package net.vvakame.memvache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.vvakame.memvache.internal.Pair;
import net.vvakame.memvache.internal.RpcVisitor;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;
import com.google.apphosting.api.DatastorePb;
import com.google.apphosting.api.DatastorePb.PutRequest;
import com.google.apphosting.api.DatastorePb.Query;
import com.google.storage.onestore.v3.OnestoreEntity.EntityProto;
import com.google.storage.onestore.v3.OnestoreEntity.Path;
import com.google.storage.onestore.v3.OnestoreEntity.Path.Element;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;

public class MemvacheDelegate implements ApiProxy.Delegate<Environment> {

	static final Logger logger = Logger.getLogger(MemvacheDelegate.class
			.getName());

	final ApiProxy.Delegate<Environment> parent;

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
			MemvacheDelegate newDelegate = new MemvacheDelegate(
					originalDelegate);
			ApiProxy.setDelegate(newDelegate);
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
		ApiProxy.setDelegate(originalDelegate);
	}

	/**
	 * {@link MemvacheDelegate}を{@link ApiProxy}からはずす。
	 */
	public void uninstall() {
		ApiProxy.setDelegate(parent);
	}

	@Override
	public Future<byte[]> makeAsyncCall(Environment env, String service,
			String method, byte[] requestBytes, ApiConfig config) {

		final byte[] data = new PreProcess().visit(service, method,
				requestBytes);
		if (data != null) {
			return createFuture(data);
		}

		Future<byte[]> future = getParent().makeAsyncCall(env, service, method,
				requestBytes, config);
		Future<byte[]> dummy = new PostProcessAsync().visit(service, method,
				Pair.create(requestBytes, future));

		return dummy != null ? dummy : future;
	}

	@Override
	public byte[] makeSyncCall(Environment env, String service, String method,
			byte[] requestBytes) throws ApiProxyException {

		final byte[] data = new PreProcess().visit(service, method,
				requestBytes);
		if (data != null) {
			return data;
		}

		byte[] result = getParent().makeSyncCall(env, service, method,
				requestBytes);

		new PostProcessSync().visit(service, method,
				Pair.create(requestBytes, result));

		return result;
	}

	static class PreProcess extends RpcVisitor<byte[], byte[]> {

		@Override
		public byte[] datastore_v3_Put(byte[] requestBytes) {
			final PutRequest pb = to_datastore_v3_Put(requestBytes);

			final MemcacheService memcache = getMemcache();
			final Set<String> memcacheKeys = new HashSet<String>();

			for (EntityProto entity : pb.entitys()) {
				final Reference key = entity.getMutableKey();
				final String namespace = key.getNameSpace();
				final Path path = key.getPath();
				for (Element element : path.mutableElements()) {

					StringBuilder builder = new StringBuilder();
					builder.append(namespace).append("@");
					builder.append(element.getType());

					memcacheKeys.add(builder.toString());
				}
			}
			if (memcacheKeys.size() == 1) {
				memcache.increment(memcacheKeys.iterator().next(), 1, 0L);
			} else if (memcacheKeys.size() != 0) {
				// memcache.incrementAll(memcacheKeys, 1, 0L);
				// TODO is this broken method? ↑
				for (String key : memcacheKeys) {
					memcache.increment(key, 1, 0L);
				}
			}

			return null;
		}

		@Override
		public byte[] datastore_v3_RunQuery(byte[] requestBytes) {
			final Query pb = to_datastore_v3_RunQuery(requestBytes);

			final MemcacheService memcache = getMemcache();

			final String namespace = pb.getNameSpace();
			final String kind = pb.getKind();

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
			builder.append("@").append(pb.hashCode());
			builder.append("@").append(counter);

			logger.log(Level.INFO, builder.toString());

			return (byte[]) memcache.get(builder.toString());
		}
	}

	static class PostProcessAsync extends
			RpcVisitor<Pair<byte[], Future<byte[]>>, Future<byte[]>> {

		@Override
		public Future<byte[]> datastore_v3_RunQuery(
				Pair<byte[], Future<byte[]>> pair) {

			final byte[] requestBytes = pair.first;
			final Future<byte[]> future = pair.second;

			return new Future<byte[]>() {
				public void processDate(byte[] data) {
					if (data == null) {
						return;
					}
					putQueryCache(requestBytes, data);
				}

				@Override
				public boolean cancel(boolean mayInterruptIfRunning) {
					return future.cancel(mayInterruptIfRunning);
				}

				@Override
				public byte[] get() throws InterruptedException,
						ExecutionException {
					byte[] data = future.get();
					processDate(data);
					return data;
				}

				@Override
				public byte[] get(long timeout, TimeUnit unit)
						throws InterruptedException, ExecutionException,
						TimeoutException {
					byte[] data = future.get(timeout, unit);
					processDate(data);
					return data;
				}

				@Override
				public boolean isCancelled() {
					return future.isCancelled();
				}

				@Override
				public boolean isDone() {
					return future.isDone();
				}
			};
		}
	}

	static class PostProcessSync extends
			RpcVisitor<Pair<byte[], byte[]>, byte[]> {

		@Override
		public byte[] datastore_v3_RunQuery(Pair<byte[], byte[]> pair) {

			final byte[] requestBytes = pair.first;
			final byte[] data = pair.second;

			putQueryCache(requestBytes, data);

			return data;
		}
	}

	static MemcacheService getMemcache() {
		return MemcacheServiceFactory.getMemcacheService("memvache");
	}

	static void putQueryCache(final byte[] requestBytes, final byte[] data) {
		DatastorePb.Query requestPb = new DatastorePb.Query();
		requestPb.mergeFrom(requestBytes);

		final MemcacheService memcache = getMemcache();

		final String namespace = requestPb.getNameSpace();
		final String kind = requestPb.getKind();

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

		memcache.put(builder.toString(), data);
	}

	Future<byte[]> createFuture(final byte[] data) {
		return new Future<byte[]>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}

			@Override
			public byte[] get() throws InterruptedException, ExecutionException {
				return data;
			}

			@Override
			public byte[] get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException,
					TimeoutException {
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