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
import com.google.apphosting.api.DatastorePb.PutRequest;
import com.google.apphosting.api.DatastorePb.Query;
import com.google.apphosting.api.DatastorePb.Transaction;

public class RpcVisitor<P, R> {

	static final Logger logger = Logger.getLogger(RpcVisitor.class
			.getName());

	public R visit(final String service, final String method, final P data) {
		return process(service, method, data);
	}

	R process(final String service, final String method, final P requestPb) {

		if ("datastore_v3".equals(service) && "BeginTransaction".equals(method)) {

			return datastore_v3_BeginTransaction(requestPb);
		} else if ("datastore_v3".equals(service) && "Put".equals(method)) {

			return datastore_v3_Put(requestPb);
		} else if ("datastore_v3".equals(service) && "Get".equals(method)) {

			return datastore_v3_Get(requestPb);
		} else if ("datastore_v3".equals(service) && "Delete".equals(method)) {

			return datastore_v3_Delete(requestPb);
		} else if ("datastore_v3".equals(service) && "RunQuery".equals(method)) {

			return datastore_v3_RunQuery(requestPb);
		} else if ("datastore_v3".equals(service) && "Commit".equals(method)) {

			return datastore_v3_Commit(requestPb);
		} else if ("datastore_v3".equals(service) && "Rollback".equals(method)) {

			return datastore_v3_Rollback(requestPb);
		} else if ("memcache".equals(service) && "Set".equals(method)) {

			return memcache_Set(requestPb);
		} else if ("memcache".equals(service) && "Get".equals(method)) {

			return memcache_Get(requestPb);
		} else if ("memcache".equals(service) && "FlushAll".equals(method)) {

			return memcache_FlushAll(requestPb);
		} else if ("memcache".equals(service)
				&& "BatchIncrement".equals(method)) {

			return memcache_BatchIncrement(requestPb);
		} else if ("memcache".equals(service) && "Increment".equals(method)) {

			return memcache_Increment(requestPb);
		} else {
			logger.info("unknown service=" + service + ", method=" + method);
		}

		return null;
	}

	public R datastore_v3_BeginTransaction(P data) {
		return null;
	}

	public R datastore_v3_Put(P data) {
		return null;
	}

	public R datastore_v3_Get(P data) {
		return null;
	}

	public R datastore_v3_Delete(P data) {
		return null;
	}

	public R datastore_v3_RunQuery(P data) {
		return null;
	}

	public R datastore_v3_Commit(P data) {
		return null;
	}

	public R datastore_v3_Rollback(P data) {
		return null;
	}

	public R memcache_Set(P data) {
		return null;
	}

	public R memcache_Get(P data) {
		return null;
	}

	public R memcache_FlushAll(P data) {
		return null;
	}

	public R memcache_BatchIncrement(P data) {
		return null;
	}

	public R memcache_Increment(P data) {
		return null;
	}

	public static BeginTransactionRequest to_datastore_v3_BeginTransaction(
			byte[] requestBytes) {
		DatastorePb.BeginTransactionRequest requestPb = new DatastorePb.BeginTransactionRequest();
		requestPb.mergeFrom(requestBytes);

		return requestPb;
	}

	public static PutRequest to_datastore_v3_Put(byte[] requestBytes) {
		DatastorePb.PutRequest requestPb = new DatastorePb.PutRequest();
		requestPb.mergeFrom(requestBytes);

		return requestPb;
	}

	public static DeleteRequest to_datastore_v3_Get(byte[] requestBytes) {
		DatastorePb.DeleteRequest requestPb = new DatastorePb.DeleteRequest();
		requestPb.mergeFrom(requestBytes);

		return requestPb;
	}

	public static DeleteRequest to_datastore_v3_Delete(byte[] requestBytes) {
		DatastorePb.DeleteRequest requestPb = new DatastorePb.DeleteRequest();
		requestPb.mergeFrom(requestBytes);

		return requestPb;
	}

	public static Query to_datastore_v3_RunQuery(byte[] requestBytes) {
		DatastorePb.Query requestPb = new DatastorePb.Query();
		requestPb.mergeFrom(requestBytes);

		return requestPb;
	}

	public static Transaction to_datastore_v3_Commit(byte[] requestBytes) {
		DatastorePb.Transaction requestPb = new DatastorePb.Transaction();
		requestPb.mergeFrom(requestBytes);

		return requestPb;
	}

	public static Transaction to_datastore_v3_Rollback(byte[] requestBytes) {
		DatastorePb.Transaction requestPb = new DatastorePb.Transaction();
		requestPb.mergeFrom(requestBytes);

		return requestPb;
	}

	public static MemcacheSetRequest to_memcache_Set(byte[] requestBytes) {
		try {
			MemcacheServicePb.MemcacheSetRequest requestPb = MemcacheServicePb.MemcacheSetRequest
					.parseFrom(requestBytes);
			return requestPb;
		} catch (Exception e) {
			logger.log(Level.WARNING, "raise exception at memcache, Set", e);
		}
		return null;
	}

	public static MemcacheGetRequest to_memcache_Get(byte[] requestBytes) {
		try {
			MemcacheServicePb.MemcacheGetRequest requestPb = MemcacheServicePb.MemcacheGetRequest
					.parseFrom(requestBytes);
			return requestPb;
		} catch (Exception e) {
			logger.log(Level.WARNING, "raise exception at memcache, Get", e);
		}
		return null;
	}

	public static MemcacheFlushRequest to_memcache_FlushAll(byte[] requestBytes) {
		try {
			MemcacheServicePb.MemcacheFlushRequest requestPb = MemcacheServicePb.MemcacheFlushRequest
					.parseFrom(requestBytes);
			return requestPb;
		} catch (Exception e) {
			logger.log(Level.WARNING, "raise exception at memcache, FlushAll",
					e);
		}
		return null;
	}

	public static MemcacheBatchIncrementRequest to_memcache_BatchIncrement(
			byte[] requestBytes) {
		try {
			MemcacheServicePb.MemcacheBatchIncrementRequest requestPb = MemcacheServicePb.MemcacheBatchIncrementRequest
					.parseFrom(requestBytes);
			return requestPb;
		} catch (Exception e) {
			logger.log(Level.WARNING,
					"raise exception at memcache, BatchIncrement", e);
		}
		return null;
	}

	public static MemcacheIncrementRequest to_memcache_Increment(
			byte[] requestBytes) {
		try {
			MemcacheServicePb.MemcacheIncrementRequest requestPb = MemcacheServicePb.MemcacheIncrementRequest
					.parseFrom(requestBytes);
			return requestPb;
		} catch (Exception e) {
			logger.log(Level.WARNING, "raise exception at memcache, Increment",
					e);
		}
		return null;
	}
}
