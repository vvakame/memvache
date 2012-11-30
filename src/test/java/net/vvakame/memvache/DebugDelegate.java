package net.vvakame.memvache;

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;


import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheBatchIncrementRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheBatchIncrementResponse;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheFlushRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheFlushResponse;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheGetRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheGetResponse;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheIncrementRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheIncrementResponse;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheSetRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheSetResponse;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;
import com.google.apphosting.api.DatastorePb.BeginTransactionRequest;
import com.google.apphosting.api.DatastorePb.CommitResponse;
import com.google.apphosting.api.DatastorePb.DeleteRequest;
import com.google.apphosting.api.DatastorePb.DeleteResponse;
import com.google.apphosting.api.DatastorePb.GetRequest;
import com.google.apphosting.api.DatastorePb.GetResponse;
import com.google.apphosting.api.DatastorePb.PutRequest;
import com.google.apphosting.api.DatastorePb.PutResponse;
import com.google.apphosting.api.DatastorePb.Query;
import com.google.apphosting.api.DatastorePb.QueryResult;
import com.google.apphosting.api.DatastorePb.Transaction;

/**
 * デバッグ用の {@link Delegate}。<br>
 * service, method と データのダンプを出力する。
 * @author vvakame
 */
public class DebugDelegate implements Delegate<Environment> {

	static final Logger logger = Logger.getLogger(DebugDelegate.class.getName());

	Delegate<Environment> parent;


	/**
	 * {@link DebugDelegate} をインストールする。
	 * @return インストールした {@link DebugDelegate}
	 * @author vvakame
	 */
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

	/**
	 * {@link DebugDelegate} をアンインストールする。
	 * @param originalDelegate 元の {@link Delegate}
	 * @author vvakame
	 */
	public static void uninstall(Delegate<Environment> originalDelegate) {
		ApiProxy.setDelegate(originalDelegate);
	}

	/**
	 * {@link DebugDelegate} をアンインストールする。
	 * @author vvakame
	 */
	public void uninstall() {
		ApiProxy.setDelegate(parent);
	}

	/**
	 * the constructor.
	 * @param parent
	 * @category constructor
	 */
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
	public Future<byte[]> makeAsyncCall(Environment env, final String service, final String method,
			final byte[] request, ApiConfig apiConfig) {

		preProcess(service, method, request);

		Future<byte[]> future = parent.makeAsyncCall(env, service, method, request, apiConfig);
		return new SniffFuture<byte[]>(future) {

			@Override
			public byte[] processDate(byte[] data) {
				postProcess(service, method, request, data);
				return null;
			}
		};
	}

	@Override
	public byte[] makeSyncCall(Environment env, String service, String method, byte[] request)
			throws ApiProxyException {

		preProcess(service, method, request);
		byte[] response = parent.makeSyncCall(env, service, method, request);
		postProcess(service, method, request, response);

		return response;
	}

	void postProcess(String service, String method, byte[] request, byte[] response) {
		logger.info("post packageName=" + service + ", methodName=" + method + ", dataSize="
				+ response.length);
		visitor.postProcess(service, method, request, response);
	}

	void preProcess(String service, String method, byte[] request) {
		logger.info("pre  packageName=" + service + ", methodName=" + method + ", dataSize="
				+ request.length);
		visitor.preProcess(service, method, request);
	}


	RpcVisitor visitor = new RpcVisitor() {

		@Override
		public Pair<byte[], byte[]> pre_datastore_v3_BeginTransaction(
				BeginTransactionRequest requestPb) {
			logger.info(requestPb.toString());
			return super.pre_datastore_v3_BeginTransaction(requestPb);
		}

		@Override
		public byte[] post_datastore_v3_BeginTransaction(BeginTransactionRequest requestPb,
				Transaction responsePb) {
			logger.info(responsePb.toString());
			return super.post_datastore_v3_BeginTransaction(requestPb, responsePb);
		}

		@Override
		public Pair<byte[], byte[]> pre_datastore_v3_Put(PutRequest requestPb) {
			logger.info(requestPb.toString());
			return super.pre_datastore_v3_Put(requestPb);
		}

		@Override
		public byte[] post_datastore_v3_Put(PutRequest requestPb, PutResponse responsePb) {
			logger.info(responsePb.toString());
			return super.post_datastore_v3_Put(requestPb, responsePb);
		}

		@Override
		public Pair<byte[], byte[]> pre_datastore_v3_Get(GetRequest requestPb) {
			logger.info(requestPb.toString());
			return super.pre_datastore_v3_Get(requestPb);
		}

		@Override
		public byte[] post_datastore_v3_Get(GetRequest requestPb, GetResponse responsePb) {
			logger.info(responsePb.toString());
			return super.post_datastore_v3_Get(requestPb, responsePb);
		}

		@Override
		public Pair<byte[], byte[]> pre_datastore_v3_Delete(DeleteRequest requestPb) {
			logger.info(requestPb.toString());
			return super.pre_datastore_v3_Delete(requestPb);
		}

		@Override
		public byte[] post_datastore_v3_Delete(DeleteRequest requestPb, DeleteResponse responsePb) {
			logger.info(responsePb.toString());
			return super.post_datastore_v3_Delete(requestPb, responsePb);
		}

		@Override
		public Pair<byte[], byte[]> pre_datastore_v3_RunQuery(Query requestPb) {
			logger.info(requestPb.toString());
			return super.pre_datastore_v3_RunQuery(requestPb);
		}

		@Override
		public byte[] post_datastore_v3_RunQuery(Query requestPb, QueryResult responsePb) {
			logger.info(responsePb.toString());
			return super.post_datastore_v3_RunQuery(requestPb, responsePb);
		}

		@Override
		public Pair<byte[], byte[]> pre_datastore_v3_Commit(Transaction requestPb) {
			logger.info(requestPb.toString());
			return super.pre_datastore_v3_Commit(requestPb);
		}

		@Override
		public byte[] post_datastore_v3_Commit(Transaction requestPb, CommitResponse responsePb) {
			logger.info(responsePb.toString());
			return super.post_datastore_v3_Commit(requestPb, responsePb);
		}

		@Override
		public Pair<byte[], byte[]> pre_datastore_v3_Rollback(Transaction requestPb) {
			logger.info(requestPb.toString());
			return super.pre_datastore_v3_Rollback(requestPb);
		}

		@Override
		public byte[] post_datastore_v3_Rollback(Transaction requestPb, CommitResponse responsePb) {
			logger.info(responsePb.toString());
			return super.post_datastore_v3_Rollback(requestPb, responsePb);
		}

		@Override
		public Pair<byte[], byte[]> pre_memcache_Set(MemcacheSetRequest requestPb) {
			logger.info(requestPb.toString());
			return super.pre_memcache_Set(requestPb);
		}

		@Override
		public byte[] post_memcache_Set(MemcacheSetRequest requestPb, MemcacheSetResponse responsePb) {
			logger.info(responsePb.toString());
			return super.post_memcache_Set(requestPb, responsePb);
		}

		@Override
		public Pair<byte[], byte[]> pre_memcache_Get(MemcacheGetRequest requestPb) {
			logger.info(requestPb.toString());
			return super.pre_memcache_Get(requestPb);
		}

		@Override
		public byte[] post_memcache_Get(MemcacheGetRequest requestPb, MemcacheGetResponse responsePb) {
			logger.info(responsePb.toString());
			return super.post_memcache_Get(requestPb, responsePb);
		}

		@Override
		public Pair<byte[], byte[]> pre_memcache_FlushAll(MemcacheFlushRequest requestPb) {
			logger.info(requestPb.toString());
			return super.pre_memcache_FlushAll(requestPb);
		}

		@Override
		public byte[] post_memcache_FlushAll(MemcacheFlushRequest requestPb,
				MemcacheFlushResponse responsePb) {
			logger.info(responsePb.toString());
			return super.post_memcache_FlushAll(requestPb, responsePb);
		}

		@Override
		public Pair<byte[], byte[]> pre_memcache_BatchIncrement(
				MemcacheBatchIncrementRequest requestPb) {
			logger.info(requestPb.toString());
			return super.pre_memcache_BatchIncrement(requestPb);
		}

		@Override
		public byte[] post_memcache_BatchIncrement(MemcacheBatchIncrementRequest requestPb,
				MemcacheBatchIncrementResponse responsePb) {
			logger.info(responsePb.toString());
			return super.post_memcache_BatchIncrement(requestPb, responsePb);
		}

		@Override
		public Pair<byte[], byte[]> pre_memcache_Increment(MemcacheIncrementRequest requestPb) {
			logger.info(requestPb.toString());
			return super.pre_memcache_Increment(requestPb);
		}

		@Override
		public byte[] post_memcache_Increment(MemcacheIncrementRequest requestPb,
				MemcacheIncrementResponse responsePb) {
			logger.info(responsePb.toString());
			return super.post_memcache_Increment(requestPb, responsePb);
		}
	};


	/**
	 * @param visitor the visitor to set
	 * @category accessor
	 */
	public void setVisitor(RpcVisitor visitor) {
		this.visitor = visitor;
	}
}
