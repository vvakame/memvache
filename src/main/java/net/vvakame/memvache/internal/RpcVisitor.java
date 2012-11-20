package net.vvakame.memvache.internal;

import java.io.IOException;
import java.util.logging.Level;
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
 * RPCの動作にHookするためのVisitor。<br>
 * {@link #preProcess(String, String, byte[])} と {@link #postProcess(String, String, byte[], byte[])} が入り口。
 * @author vvakame
 */
public class RpcVisitor {

	static final Logger logger = Logger.getLogger(RpcVisitor.class.getName());


	/**
	 * あるRPCを行う"前"に呼び出すメソッド。<br>
	 * もし、そのRPCをキャンセルして何らかの処理結果を受け取った事にしたい場合、null以外の値を返す。
	 * @param service
	 * @param method
	 * @param data request内容
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public final byte[] preProcess(final String service, final String method, final byte[] data) {

		if ("datastore_v3".equals(service) && "BeginTransaction".equals(method)) {
			BeginTransactionRequest requestPb = new BeginTransactionRequest();
			requestPb.mergeFrom(data);
			return pre_datastore_v3_BeginTransaction(requestPb);
		} else if ("datastore_v3".equals(service) && "Put".equals(method)) {
			PutRequest requestPb = new PutRequest();
			requestPb.mergeFrom(data);
			return pre_datastore_v3_Put(requestPb);
		} else if ("datastore_v3".equals(service) && "Get".equals(method)) {
			GetRequest requestPb = new GetRequest();
			requestPb.mergeFrom(data);
			return pre_datastore_v3_Get(requestPb);
		} else if ("datastore_v3".equals(service) && "Delete".equals(method)) {
			DeleteRequest requestPb = new DeleteRequest();
			requestPb.mergeFrom(data);
			return pre_datastore_v3_Delete(requestPb);
		} else if ("datastore_v3".equals(service) && "RunQuery".equals(method)) {
			Query requestPb = new Query();
			requestPb.mergeFrom(data);
			return pre_datastore_v3_RunQuery(requestPb);
		} else if ("datastore_v3".equals(service) && "Commit".equals(method)) {
			Transaction requestPb = new Transaction();
			requestPb.mergeFrom(data);
			return pre_datastore_v3_Commit(requestPb);
		} else if ("datastore_v3".equals(service) && "Rollback".equals(method)) {
			Transaction requestPb = new Transaction();
			requestPb.mergeFrom(data);
			return pre_datastore_v3_Rollback(requestPb);
		} else if ("memcache".equals(service) && "Set".equals(method)) {
			try {
				MemcacheSetRequest requestPb = MemcacheSetRequest.parseFrom(data);
				return pre_memcache_Set(requestPb);
			} catch (IOException e) {
				// InvalidProtocolBufferException class was repackaged.
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Get".equals(method)) {
			try {
				MemcacheGetRequest requestPb = MemcacheGetRequest.parseFrom(data);
				return pre_memcache_Get(requestPb);
			} catch (IOException e) {
				// InvalidProtocolBufferException class was repackaged.
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "FlushAll".equals(method)) {
			try {
				MemcacheFlushRequest requestPb = MemcacheFlushRequest.parseFrom(data);
				return pre_memcache_FlushAll(requestPb);
			} catch (IOException e) {
				// InvalidProtocolBufferException class was repackaged.
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "BatchIncrement".equals(method)) {
			try {
				MemcacheBatchIncrementRequest requestPb =
						MemcacheBatchIncrementRequest.parseFrom(data);
				return pre_memcache_BatchIncrement(requestPb);
			} catch (IOException e) {
				// InvalidProtocolBufferException class was repackaged.
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Increment".equals(method)) {
			try {
				MemcacheIncrementRequest requestPb = MemcacheIncrementRequest.parseFrom(data);
				return pre_memcache_Increment(requestPb);
			} catch (IOException e) {
				// InvalidProtocolBufferException class was repackaged.
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else {
			logger.info("unknown service=" + service + ", method=" + method);
		}

		return null;
	}

	/**
	 * あるRPCを行う"後"に呼び出すメソッド。<br>
	 * もっぱら、次以降のリクエストの {@link #preProcess(String, String, byte[])} で何かを返すための仕込み処理を行う。
	 * @param service
	 * @param method
	 * @param request request内容
	 * @param response response内容
	 * @return 何か処理を行ったか否か
	 * @author vvakame
	 */
	public final boolean postProcess(final String service, final String method,
			final byte[] request, final byte[] response) {

		if ("datastore_v3".equals(service) && "BeginTransaction".equals(method)) {
			BeginTransactionRequest requestPb = new BeginTransactionRequest();
			requestPb.mergeFrom(request);
			Transaction responsePb = new Transaction();
			responsePb.mergeFrom(response);
			return post_datastore_v3_BeginTransaction(requestPb, responsePb);
		} else if ("datastore_v3".equals(service) && "Put".equals(method)) {
			PutRequest requestPb = new PutRequest();
			requestPb.mergeFrom(request);
			PutResponse responsePb = new PutResponse();
			responsePb.mergeFrom(response);
			return post_datastore_v3_Put(requestPb, responsePb);
		} else if ("datastore_v3".equals(service) && "Get".equals(method)) {
			GetRequest requestPb = new GetRequest();
			requestPb.mergeFrom(request);
			GetResponse responsePb = new GetResponse();
			responsePb.mergeFrom(response);
			return post_datastore_v3_Get(requestPb, responsePb);
		} else if ("datastore_v3".equals(service) && "Delete".equals(method)) {
			DeleteRequest requestPb = new DeleteRequest();
			requestPb.mergeFrom(request);
			DeleteResponse responsePb = new DeleteResponse();
			responsePb.mergeFrom(response);
			return post_datastore_v3_Delete(requestPb, responsePb);
		} else if ("datastore_v3".equals(service) && "RunQuery".equals(method)) {
			Query requestPb = new Query();
			requestPb.mergeFrom(request);
			QueryResult responsePb = new QueryResult();
			responsePb.mergeFrom(response);
			return post_datastore_v3_RunQuery(requestPb, responsePb);
		} else if ("datastore_v3".equals(service) && "Commit".equals(method)) {
			Transaction requestPb = new Transaction();
			requestPb.mergeFrom(request);
			CommitResponse responsePb = new CommitResponse();
			responsePb.mergeFrom(response);
			return post_datastore_v3_Commit(requestPb, responsePb);
		} else if ("datastore_v3".equals(service) && "Rollback".equals(method)) {
			Transaction requestPb = new Transaction();
			requestPb.mergeFrom(request);
			CommitResponse responsePb = new CommitResponse();
			responsePb.mergeFrom(response);
			return post_datastore_v3_Rollback(requestPb, responsePb);
		} else if ("memcache".equals(service) && "Set".equals(method)) {
			try {
				MemcacheSetRequest requestPb = MemcacheSetRequest.parseFrom(request);
				MemcacheSetResponse responsePb = MemcacheSetResponse.parseFrom(response);
				return post_memcache_Set(requestPb, responsePb);
			} catch (IOException e) {
				// InvalidProtocolBufferException class was repackaged.
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Get".equals(method)) {
			try {
				MemcacheGetRequest requestPb = MemcacheGetRequest.parseFrom(request);
				MemcacheGetResponse responsePb = MemcacheGetResponse.parseFrom(response);
				return post_memcache_Get(requestPb, responsePb);
			} catch (IOException e) {
				// InvalidProtocolBufferException class was repackaged.
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "FlushAll".equals(method)) {
			try {
				MemcacheFlushRequest requestPb = MemcacheFlushRequest.parseFrom(request);
				MemcacheFlushResponse responsePb = MemcacheFlushResponse.parseFrom(response);
				return post_memcache_FlushAll(requestPb, responsePb);
			} catch (IOException e) {
				// InvalidProtocolBufferException class was repackaged.
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "BatchIncrement".equals(method)) {
			try {
				MemcacheBatchIncrementRequest requestPb =
						MemcacheBatchIncrementRequest.parseFrom(request);
				MemcacheBatchIncrementResponse responsePb =
						MemcacheBatchIncrementResponse.parseFrom(response);
				return post_memcache_BatchIncrement(requestPb, responsePb);
			} catch (IOException e) {
				// InvalidProtocolBufferException class was repackaged.
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Increment".equals(method)) {
			try {
				MemcacheIncrementRequest requestPb = MemcacheIncrementRequest.parseFrom(request);
				MemcacheIncrementResponse responsePb =
						MemcacheIncrementResponse.parseFrom(response);
				return post_memcache_Increment(requestPb, responsePb);
			} catch (IOException e) {
				// InvalidProtocolBufferException class was repackaged.
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else {
			logger.info("unknown service=" + service + ", method=" + method);
		}

		return false;
	}

	/**
	 * DatastoreのBeginTransactionの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_datastore_v3_BeginTransaction(BeginTransactionRequest requestPb) {
		return null;
	}

	/**
	 * DatastoreのBeginTransactionの後処理を行う。
	 * @param requestPb
	 * @param responsePb
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_datastore_v3_BeginTransaction(BeginTransactionRequest requestPb,
			Transaction responsePb) {
		return false;
	}

	/**
	 * DatastoreのPutの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_datastore_v3_Put(PutRequest requestPb) {
		return null;
	}

	/**
	 * DatastoreのPutの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_datastore_v3_Put(PutRequest requestPb, PutResponse responsePb) {
		return false;
	}

	/**
	 * DatastoreのGetの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_datastore_v3_Get(GetRequest requestPb) {
		return null;
	}

	/**
	 * DatastoreのGetの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_datastore_v3_Get(GetRequest requestPb, GetResponse responsePb) {
		return false;
	}

	/**
	 * DatastoreのDeleteの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_datastore_v3_Delete(DeleteRequest requestPb) {
		return null;
	}

	/**
	 * DatastoreのDeleteの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_datastore_v3_Delete(DeleteRequest requestPb, DeleteResponse responsePb) {
		return false;
	}

	/**
	 * DatastoreのRunQueryの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_datastore_v3_RunQuery(Query requestPb) {
		return null;
	}

	/**
	 * DatastoreのRunQueryの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_datastore_v3_RunQuery(Query requestPb, QueryResult responsePb) {
		return false;
	}

	/**
	 * DatastoreのCommitの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_datastore_v3_Commit(Transaction requestPb) {
		return null;
	}

	/**
	 * DatastoreのCommitの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_datastore_v3_Commit(Transaction requestPb, CommitResponse responsePb) {
		return false;
	}

	/**
	 * DatastoreのRollbackの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_datastore_v3_Rollback(Transaction requestPb) {
		return null;
	}

	/**
	 * DatastoreのRollbackの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_datastore_v3_Rollback(Transaction requestPb, CommitResponse responsePb) {
		return false;
	}

	/**
	 * MemcacheのSetの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_memcache_Set(MemcacheSetRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのSetの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_memcache_Set(MemcacheSetRequest requestPb, MemcacheSetResponse responsePb) {
		return false;
	}

	/**
	 * MemcacheのGetの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_memcache_Get(MemcacheGetRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのGetの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_memcache_Get(MemcacheGetRequest requestPb, MemcacheGetResponse responsePb) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * MemcacheのFlushAllの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_memcache_FlushAll(MemcacheFlushRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのFlushAllの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_memcache_FlushAll(MemcacheFlushRequest requestPb,
			MemcacheFlushResponse responsePb) {
		return false;
	}

	/**
	 * MemcacheのBatchIncrementの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_memcache_BatchIncrement(MemcacheBatchIncrementRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのBatchIncrementの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_memcache_BatchIncrement(MemcacheBatchIncrementRequest requestPb,
			MemcacheBatchIncrementResponse responsePb) {
		return false;
	}

	/**
	 * MemcacheのIncrementの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] pre_memcache_Increment(MemcacheIncrementRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのIncrementの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理を行ったか否か
	 * @author vvakame
	 */
	public boolean post_memcache_Increment(MemcacheIncrementRequest requestPb,
			MemcacheIncrementResponse responsePb) {
		return false;
	}
}
