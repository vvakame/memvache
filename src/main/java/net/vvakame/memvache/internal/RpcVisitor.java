package net.vvakame.memvache.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.memcache.MemcacheServicePb;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheBatchIncrementRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheFlushRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheGetRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheIncrementRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheSetRequest;
import com.google.apphosting.api.DatastorePb;
import com.google.apphosting.api.DatastorePb.BeginTransactionRequest;
import com.google.apphosting.api.DatastorePb.DeleteRequest;
import com.google.apphosting.api.DatastorePb.GetRequest;
import com.google.apphosting.api.DatastorePb.PutRequest;
import com.google.apphosting.api.DatastorePb.Query;
import com.google.apphosting.api.DatastorePb.Transaction;

public class RpcVisitor {

	static final Logger logger = Logger.getLogger(RpcVisitor.class.getName());

	public void visit(final String service, final String method,
			final byte[] requestBytes) {

		if ("datastore_v3".equals(service) && "BeginTransaction".equals(method)) {

			DatastorePb.BeginTransactionRequest requestPb = new DatastorePb.BeginTransactionRequest();
			requestPb.mergeFrom(requestBytes);

			datastore_v3_BeginTransaction(requestPb);
		} else if ("datastore_v3".equals(service) && "Put".equals(method)) {

			DatastorePb.PutRequest requestPb = new DatastorePb.PutRequest();
			requestPb.mergeFrom(requestBytes);

			datastore_v3_Put(requestPb);
		} else if ("datastore_v3".equals(service) && "Get".equals(method)) {

			DatastorePb.GetRequest requestPb = new DatastorePb.GetRequest();
			requestPb.mergeFrom(requestBytes);

			datastore_v3_Get(requestPb);
		} else if ("datastore_v3".equals(service) && "Delete".equals(method)) {

			DatastorePb.DeleteRequest requestPb = new DatastorePb.DeleteRequest();
			requestPb.mergeFrom(requestBytes);

			datastore_v3_Delete(requestPb);
		} else if ("datastore_v3".equals(service) && "RunQuery".equals(method)) {

			DatastorePb.Query requestPb = new DatastorePb.Query();
			requestPb.mergeFrom(requestBytes);

			datastore_v3_RunQuery(requestPb);
		} else if ("datastore_v3".equals(service) && "Commit".equals(method)) {

			DatastorePb.Transaction requestPb = new DatastorePb.Transaction();
			requestPb.mergeFrom(requestBytes);

			datastore_v3_Commit(requestPb);
		} else if ("datastore_v3".equals(service) && "Rollback".equals(method)) {

			DatastorePb.Transaction requestPb = new DatastorePb.Transaction();
			requestPb.mergeFrom(requestBytes);

			datastore_v3_Rollback(requestPb);
		} else if ("memcache".equals(service) && "Set".equals(method)) {

			try {
				MemcacheServicePb.MemcacheSetRequest requestPb = MemcacheServicePb.MemcacheSetRequest
						.parseFrom(requestBytes);
				memcache_Set(requestPb);
			} catch (Exception e) {
				logger.log(Level.WARNING, "raise exception at memcache, Set", e);
			}
		} else if ("memcache".equals(service) && "Get".equals(method)) {

			try {
				MemcacheServicePb.MemcacheGetRequest requestPb = MemcacheServicePb.MemcacheGetRequest
						.parseFrom(requestBytes);
				memcache_Get(requestPb);
			} catch (Exception e) {
				logger.log(Level.WARNING, "raise exception at memcache, Get", e);
			}
		} else if ("memcache".equals(service) && "FlushAll".equals(method)) {

			try {
				MemcacheServicePb.MemcacheFlushRequest requestPb = MemcacheServicePb.MemcacheFlushRequest
						.parseFrom(requestBytes);
				memcache_FlushAll(requestPb);
			} catch (Exception e) {
				logger.log(Level.WARNING,
						"raise exception at memcache, FlushAll", e);
			}
		} else if ("memcache".equals(service)
				&& "BatchIncrement".equals(method)) {
			try {
				MemcacheServicePb.MemcacheBatchIncrementRequest requestPb = MemcacheServicePb.MemcacheBatchIncrementRequest
						.parseFrom(requestBytes);
				memcache_BatchIncrement(requestPb);
			} catch (Exception e) {
				logger.log(Level.WARNING,
						"raise exception at memcache, BatchIncrement", e);
			}
		} else if ("memcache".equals(service) && "Increment".equals(method)) {
			try {
				MemcacheServicePb.MemcacheIncrementRequest requestPb = MemcacheServicePb.MemcacheIncrementRequest
						.parseFrom(requestBytes);
				memcache_Increment(requestPb);
			} catch (Exception e) {
				logger.log(Level.WARNING,
						"raise exception at memcache, Increment", e);
			}
		} else {
			logger.info("unknown service=" + service + ", method=" + method);
		}
	}

	public void datastore_v3_BeginTransaction(BeginTransactionRequest pb) {
	}

	public void datastore_v3_Put(PutRequest pb) {
	}

	public void datastore_v3_Get(GetRequest pb) {
	}

	public void datastore_v3_Delete(DeleteRequest pb) {
	}

	public void datastore_v3_RunQuery(Query pb) {
	}

	public void datastore_v3_Commit(Transaction requestPb) {
	}

	public void datastore_v3_Rollback(Transaction requestPb) {
	}

	public void memcache_Set(MemcacheSetRequest requestPb) {
	}

	public void memcache_Get(MemcacheGetRequest requestPb) {
	}

	public void memcache_FlushAll(MemcacheFlushRequest requestPb) {
	}

	public void memcache_BatchIncrement(MemcacheBatchIncrementRequest requestPb) {
	}

	public void memcache_Increment(MemcacheIncrementRequest requestPb) {
	}
}
