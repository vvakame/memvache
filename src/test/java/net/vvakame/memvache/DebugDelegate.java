package net.vvakame.memvache;

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.google.appengine.api.memcache.MemcacheServicePb;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;
import com.google.apphosting.api.DatastorePb;

/**
 * デバッグ用の {@link Delegate}。<br>
 * service, method と データのダンプを出力する。
 * @author vvakame
 */
class DebugDelegate implements Delegate<Environment> {

	static final Logger logger = Logger.getLogger(DebugDelegate.class.getName());

	Delegate<Environment> parent;


	public static DebugDelegate install() {
		@SuppressWarnings("unchecked")
		Delegate<Environment> originalDelegate = ApiProxy.getDelegate();
		if (originalDelegate instanceof DebugDelegate == false) {
			DebugDelegate newDelegate = new DebugDelegate(originalDelegate);
			ApiProxy.setDelegate(newDelegate);
			return newDelegate;
		} else {
			return (DebugDelegate) originalDelegate;
		}
	}

	public static void uninstall(Delegate<Environment> originalDelegate) {
		ApiProxy.setDelegate(originalDelegate);
	}

	public void uninstall() {
		ApiProxy.setDelegate(parent);
	}

	public DebugDelegate(Delegate<Environment> parent) {
		this.parent = parent;
	}

	@Override
	public void flushLogs(Environment env) {
		parent.flushLogs(env);
	}

	@Override
	public List<Thread> getRequestThreads(Environment env) {
		return parent.getRequestThreads(env);
	}

	@Override
	public void log(Environment env, LogRecord log) {
		parent.log(env, log);
	}

	@Override
	public Future<byte[]> makeAsyncCall(Environment env, String service, String method,
			byte[] request, ApiConfig apiConfig) {

		process(service, method, request);

		return parent.makeAsyncCall(env, service, method, request, apiConfig);
	}

	@Override
	public byte[] makeSyncCall(Environment env, String service, String method, byte[] request)
			throws ApiProxyException {

		process(service, method, request);

		return parent.makeSyncCall(env, service, method, request);
	}

	static void process(String service, String method, byte[] request) {

		logger.info("packageName=" + service + ", methodName=" + method + ", dataSize="
				+ request.length);

		if ("datastore_v3".equals(service) && "BeginTransaction".equals(method)) {

			DatastorePb.BeginTransactionRequest requestPb =
					new DatastorePb.BeginTransactionRequest();
			requestPb.mergeFrom(request);

			logger.info(requestPb.toString());
		} else if ("datastore_v3".equals(service) && "Put".equals(method)) {

			DatastorePb.PutRequest requestPb = new DatastorePb.PutRequest();
			requestPb.mergeFrom(request);

			logger.info(requestPb.toString());
		} else if ("datastore_v3".equals(service) && "Get".equals(method)) {

			DatastorePb.GetRequest requestPb = new DatastorePb.GetRequest();
			requestPb.mergeFrom(request);

			logger.info(requestPb.toString());
		} else if ("datastore_v3".equals(service) && "Delete".equals(method)) {

			DatastorePb.DeleteRequest requestPb = new DatastorePb.DeleteRequest();
			requestPb.mergeFrom(request);

			logger.info(requestPb.toString());
		} else if ("datastore_v3".equals(service) && "RunQuery".equals(method)) {

			{
				DatastorePb.Query requestPb = new DatastorePb.Query();
				requestPb.mergeFrom(request);

				logger.info(requestPb.toString());
			}
		} else if ("datastore_v3".equals(service) && "Commit".equals(method)) {

			DatastorePb.Transaction txPb = new DatastorePb.Transaction();
			txPb.mergeFrom(request);

			logger.info(txPb.toString());
		} else if ("datastore_v3".equals(service) && "Rollback".equals(method)) {

			DatastorePb.Transaction txPb = new DatastorePb.Transaction();
			txPb.mergeFrom(request);

			logger.info(txPb.toString());
		} else if ("memcache".equals(service) && "Set".equals(method)) {

			try {
				MemcacheServicePb.MemcacheSetRequest pb =
						MemcacheServicePb.MemcacheSetRequest.parseFrom(request);
				logger.info(pb.toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if ("memcache".equals(service) && "Get".equals(method)) {

			try {
				MemcacheServicePb.MemcacheGetRequest pb =
						MemcacheServicePb.MemcacheGetRequest.parseFrom(request);
				logger.info(pb.toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if ("memcache".equals(service) && "FlushAll".equals(method)) {

			try {
				MemcacheServicePb.MemcacheFlushRequest pb =
						MemcacheServicePb.MemcacheFlushRequest.parseFrom(request);
				logger.info(pb.toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if ("memcache".equals(service) && "BatchIncrement".equals(method)) {
			try {
				MemcacheServicePb.MemcacheBatchIncrementRequest pb =
						MemcacheServicePb.MemcacheBatchIncrementRequest.parseFrom(request);
				logger.info(pb.toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if ("memcache".equals(service) && "Increment".equals(method)) {
			try {
				MemcacheServicePb.MemcacheIncrementRequest pb =
						MemcacheServicePb.MemcacheIncrementRequest.parseFrom(request);
				logger.info(pb.toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
