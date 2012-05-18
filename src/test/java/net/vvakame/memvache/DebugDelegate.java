package net.vvakame.memvache;

import java.util.List;
import java.util.concurrent.Future;

import com.google.appengine.api.memcache.MemcacheServicePb;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.DatastorePb;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

class DebugDelegate implements Delegate<Environment> {

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
	public Future<byte[]> makeAsyncCall(Environment env,
			String packageName, String methodName, byte[] request,
			ApiConfig apiConfig) {

		process(packageName, methodName, request);

		return parent.makeAsyncCall(env, packageName, methodName, request,
				apiConfig);
	}

	@Override
	public byte[] makeSyncCall(Environment env, String packageName,
			String methodName, byte[] request) throws ApiProxyException {

		process(packageName, methodName, request);

		return parent.makeSyncCall(env, packageName, methodName, request);
	}

	static void process(String packageName, String methodName,
			byte[] request) {

		ProofOfConceptTest.logger.info("packageName=" + packageName + ", methodName="
				+ methodName + ", dataSize=" + request.length);

		if ("datastore_v3".equals(packageName)
				&& "BeginTransaction".equals(methodName)) {

			DatastorePb.BeginTransactionRequest requestPb = new DatastorePb.BeginTransactionRequest();
			requestPb.mergeFrom(request);

			ProofOfConceptTest.logger.info(requestPb.toString());
		} else if ("datastore_v3".equals(packageName)
				&& "Put".equals(methodName)) {

			DatastorePb.PutRequest requestPb = new DatastorePb.PutRequest();
			requestPb.mergeFrom(request);

			ProofOfConceptTest.logger.info(requestPb.toString());
		} else if ("datastore_v3".equals(packageName)
				&& "Get".equals(methodName)) {

			DatastorePb.GetRequest requestPb = new DatastorePb.GetRequest();
			requestPb.mergeFrom(request);

			ProofOfConceptTest.logger.info(requestPb.toString());
		} else if ("datastore_v3".equals(packageName)
				&& "Delete".equals(methodName)) {

			DatastorePb.DeleteRequest requestPb = new DatastorePb.DeleteRequest();
			requestPb.mergeFrom(request);

			ProofOfConceptTest.logger.info(requestPb.toString());
		} else if ("datastore_v3".equals(packageName)
				&& "RunQuery".equals(methodName)) {

			{
				DatastorePb.Query requestPb = new DatastorePb.Query();
				requestPb.mergeFrom(request);

				ProofOfConceptTest.logger.info(requestPb.toString());
			}
		} else if ("datastore_v3".equals(packageName)
				&& "Commit".equals(methodName)) {

			DatastorePb.Transaction txPb = new DatastorePb.Transaction();
			txPb.mergeFrom(request);

			ProofOfConceptTest.logger.info(txPb.toString());
		} else if ("datastore_v3".equals(packageName)
				&& "Rollback".equals(methodName)) {

			DatastorePb.Transaction txPb = new DatastorePb.Transaction();
			txPb.mergeFrom(request);

			ProofOfConceptTest.logger.info(txPb.toString());
		} else if ("memcache".equals(packageName)
				&& "Set".equals(methodName)) {

			try {
				MemcacheServicePb.MemcacheSetRequest pb = MemcacheServicePb.MemcacheSetRequest
						.parseFrom(request);
				ProofOfConceptTest.logger.info(pb.toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if ("memcache".equals(packageName)
				&& "Get".equals(methodName)) {

			try {
				MemcacheServicePb.MemcacheGetRequest pb = MemcacheServicePb.MemcacheGetRequest
						.parseFrom(request);
				ProofOfConceptTest.logger.info(pb.toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if ("memcache".equals(packageName)
				&& "FlushAll".equals(methodName)) {

			try {
				MemcacheServicePb.MemcacheFlushRequest pb = MemcacheServicePb.MemcacheFlushRequest
						.parseFrom(request);
				ProofOfConceptTest.logger.info(pb.toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if ("memcache".equals(packageName)
				&& "BatchIncrement".equals(methodName)) {
			try {
				MemcacheServicePb.MemcacheBatchIncrementRequest pb = MemcacheServicePb.MemcacheBatchIncrementRequest
						.parseFrom(request);
				ProofOfConceptTest.logger.info(pb.toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if ("memcache".equals(packageName)
				&& "Increment".equals(methodName)) {
			try {
				MemcacheServicePb.MemcacheIncrementRequest pb = MemcacheServicePb.MemcacheIncrementRequest
						.parseFrom(request);
				ProofOfConceptTest.logger.info(pb.toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}