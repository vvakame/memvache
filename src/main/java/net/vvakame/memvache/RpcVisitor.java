package net.vvakame.memvache;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheBatchIncrementRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheBatchIncrementResponse;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheDeleteRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheDeleteResponse;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheFlushRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheFlushResponse;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheGetRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheGetResponse;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheIncrementRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheIncrementResponse;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheSetRequest;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheSetResponse;
import com.google.appengine.api.memcache.MemcacheServicePb.MemcacheStatsRequest;
import com.google.appengine.api.search.SearchServicePb.DeleteDocumentRequest;
import com.google.appengine.api.search.SearchServicePb.DeleteDocumentResponse;
import com.google.appengine.api.search.SearchServicePb.IndexDocumentRequest;
import com.google.appengine.api.search.SearchServicePb.IndexDocumentResponse;
import com.google.appengine.api.search.SearchServicePb.SearchRequest;
import com.google.appengine.api.search.SearchServicePb.SearchResponse;
import com.google.apphosting.api.DatastorePb.AllocateIdsRequest;
import com.google.apphosting.api.DatastorePb.AllocateIdsResponse;
import com.google.apphosting.api.DatastorePb.BeginTransactionRequest;
import com.google.apphosting.api.DatastorePb.CommitResponse;
import com.google.apphosting.api.DatastorePb.DeleteRequest;
import com.google.apphosting.api.DatastorePb.DeleteResponse;
import com.google.apphosting.api.DatastorePb.GetRequest;
import com.google.apphosting.api.DatastorePb.GetResponse;
import com.google.apphosting.api.DatastorePb.NextRequest;
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
public abstract class RpcVisitor implements Strategy {

	static final Logger logger = Logger.getLogger(RpcVisitor.class.getName());

	static boolean debug = false;


	/**
	 * あるRPCを行う"前"に呼び出すメソッド。<br>
	 * もし、そのRPCをキャンセルして何らかの処理結果を受け取った事にしたい場合、null以外の値を返す。
	 * @param service
	 * @param method
	 * @param request request内容
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	@Override
	public final Pair<byte[], byte[]> preProcess(final String service, final String method,
			final byte[] request) {

		if ("datastore_v3".equals(service) && "BeginTransaction".equals(method)) {
			BeginTransactionRequest requestPb = new BeginTransactionRequest();
			requestPb.mergeFrom(request);
			return pre_datastore_v3_BeginTransaction(requestPb);
		} else if ("datastore_v3".equals(service) && "AllocateIds".equals(method)) {
			AllocateIdsRequest requestPb = new AllocateIdsRequest();
			requestPb.mergeFrom(request);
			return pre_datastore_v3_AllocateIds(requestPb);
		} else if ("datastore_v3".equals(service) && "Put".equals(method)) {
			PutRequest requestPb = new PutRequest();
			requestPb.mergeFrom(request);
			return pre_datastore_v3_Put(requestPb);
		} else if ("datastore_v3".equals(service) && "Get".equals(method)) {
			GetRequest requestPb = new GetRequest();
			requestPb.mergeFrom(request);
			return pre_datastore_v3_Get(requestPb);
		} else if ("datastore_v3".equals(service) && "Delete".equals(method)) {
			DeleteRequest requestPb = new DeleteRequest();
			requestPb.mergeFrom(request);
			return pre_datastore_v3_Delete(requestPb);
		} else if ("datastore_v3".equals(service) && "RunQuery".equals(method)) {
			Query requestPb = new Query();
			requestPb.mergeFrom(request);
			return pre_datastore_v3_RunQuery(requestPb);
		} else if ("datastore_v3".equals(service) && "Next".equals(method)) {
			NextRequest requestPb = new NextRequest();
			requestPb.mergeFrom(request);
			return pre_datastore_v3_Next(requestPb);
		} else if ("datastore_v3".equals(service) && "Commit".equals(method)) {
			Transaction requestPb = new Transaction();
			requestPb.mergeFrom(request);
			return pre_datastore_v3_Commit(requestPb);
		} else if ("datastore_v3".equals(service) && "Rollback".equals(method)) {
			Transaction requestPb = new Transaction();
			requestPb.mergeFrom(request);
			return pre_datastore_v3_Rollback(requestPb);
		} else if ("memcache".equals(service) && "Set".equals(method)) {
			try {
				MemcacheSetRequest requestPb = MemcacheSetRequest.parseFrom(request);
				return pre_memcache_Set(requestPb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Get".equals(method)) {
			try {
				MemcacheGetRequest requestPb = MemcacheGetRequest.parseFrom(request);
				return pre_memcache_Get(requestPb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Delete".equals(method)) {
			try {
				MemcacheDeleteRequest requestPb = MemcacheDeleteRequest.parseFrom(request);
				return pre_memcache_Delete(requestPb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "FlushAll".equals(method)) {
			try {
				MemcacheFlushRequest requestPb = MemcacheFlushRequest.parseFrom(request);
				return pre_memcache_FlushAll(requestPb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "BatchIncrement".equals(method)) {
			try {
				MemcacheBatchIncrementRequest requestPb =
						MemcacheBatchIncrementRequest.parseFrom(request);
				return pre_memcache_BatchIncrement(requestPb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Increment".equals(method)) {
			try {
				MemcacheIncrementRequest requestPb = MemcacheIncrementRequest.parseFrom(request);
				return pre_memcache_Increment(requestPb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Stats".equals(method)) {
			try {
				MemcacheStatsRequest requestPb = MemcacheStatsRequest.parseFrom(request);
				return pre_memcache_Stats(requestPb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("search".equals(service) && "IndexDocument".equals(method)) {
			try {
				IndexDocumentRequest requestPb = IndexDocumentRequest.parseFrom(request);
				return pre_search_IndexDocument(requestPb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("search".equals(service) && "Search".equals(method)) {
			try {
				SearchRequest requestPb = SearchRequest.parseFrom(request);
				return pre_search_Search(requestPb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if ("search".equals(service) && "DeleteDocument".equals(method)) {
			try {
				DeleteDocumentRequest requestPb = DeleteDocumentRequest.parseFrom(request);
				return pre_search_DeleteDocument(requestPb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				throw new IllegalStateException("raise exception at " + service + ", " + method, e);
			}
		} else if (debug) {
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
	@Override
	public final byte[] postProcess(final String service, final String method,
			final byte[] request, final byte[] response) {

		if ("datastore_v3".equals(service) && "BeginTransaction".equals(method)) {
			BeginTransactionRequest requestPb = new BeginTransactionRequest();
			requestPb.mergeFrom(request);
			Transaction responsePb = new Transaction();
			responsePb.mergeFrom(response);
			return post_datastore_v3_BeginTransaction(requestPb, responsePb);
		} else if ("datastore_v3".equals(service) && "AllocateIds".equals(method)) {
			AllocateIdsRequest requestPb = new AllocateIdsRequest();
			requestPb.mergeFrom(request);
			AllocateIdsResponse responsePb = new AllocateIdsResponse();
			responsePb.mergeFrom(response);
			return post_datastore_v3_AllocateIds(requestPb, responsePb);
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
		} else if ("datastore_v3".equals(service) && "Next".equals(method)) {
			NextRequest requestPb = new NextRequest();
			requestPb.mergeFrom(request);
			QueryResult responsePb = new QueryResult();
			responsePb.mergeFrom(response);
			return post_datastore_v3_Next(requestPb, responsePb);
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
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Get".equals(method)) {
			try {
				MemcacheGetRequest requestPb = MemcacheGetRequest.parseFrom(request);
				MemcacheGetResponse responsePb = MemcacheGetResponse.parseFrom(response);
				return post_memcache_Get(requestPb, responsePb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Delete".equals(method)) {
			try {
				MemcacheDeleteRequest requestPb = MemcacheDeleteRequest.parseFrom(request);
				MemcacheDeleteResponse responsePb = MemcacheDeleteResponse.parseFrom(response);
				return post_memcache_Delete(requestPb, responsePb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "FlushAll".equals(method)) {
			try {
				MemcacheFlushRequest requestPb = MemcacheFlushRequest.parseFrom(request);
				MemcacheFlushResponse responsePb = MemcacheFlushResponse.parseFrom(response);
				return post_memcache_FlushAll(requestPb, responsePb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "BatchIncrement".equals(method)) {
			try {
				MemcacheBatchIncrementRequest requestPb =
						MemcacheBatchIncrementRequest.parseFrom(request);
				MemcacheBatchIncrementResponse responsePb =
						MemcacheBatchIncrementResponse.parseFrom(response);
				return post_memcache_BatchIncrement(requestPb, responsePb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Increment".equals(method)) {
			try {
				MemcacheIncrementRequest requestPb = MemcacheIncrementRequest.parseFrom(request);
				MemcacheIncrementResponse responsePb =
						MemcacheIncrementResponse.parseFrom(response);
				return post_memcache_Increment(requestPb, responsePb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("memcache".equals(service) && "Stats".equals(method)) {
			try {
				MemcacheStatsRequest requestPb = MemcacheStatsRequest.parseFrom(request);
				MemcacheSetResponse responsePb = MemcacheSetResponse.parseFrom(response);
				return post_memcache_Stats(requestPb, responsePb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("search".equals(service) && "IndexDocument".equals(method)) {
			try {
				IndexDocumentRequest requestPb = IndexDocumentRequest.parseFrom(request);
				IndexDocumentResponse responsePb = IndexDocumentResponse.parseFrom(response);
				return post_search_IndexDocument(requestPb, responsePb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("search".equals(service) && "Search".equals(method)) {
			try {
				SearchRequest requestPb = SearchRequest.parseFrom(request);
				SearchResponse responsePb = SearchResponse.parseFrom(response);
				return post_search_Search(requestPb, responsePb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if ("search".equals(service) && "DeleteDocument".equals(method)) {
			try {
				DeleteDocumentRequest requestPb = DeleteDocumentRequest.parseFrom(request);
				DeleteDocumentResponse responsePb = DeleteDocumentResponse.parseFrom(response);
				return post_search_DeleteDocument(requestPb, responsePb);
			} catch (com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException e) {
				logger.log(Level.WARNING, "raise exception at " + service + ", " + method, e);
			}
		} else if (debug) {
			logger.info("unknown service=" + service + ", method=" + method);
		}

		return null;
	}

	/**
	 * DatastoreのBeginTransactionの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_datastore_v3_BeginTransaction(BeginTransactionRequest requestPb) {
		return null;
	}

	/**
	 * DatastoreのBeginTransactionの後処理を行う。
	 * @param requestPb
	 * @param responsePb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_datastore_v3_BeginTransaction(BeginTransactionRequest requestPb,
			Transaction responsePb) {
		return null;
	}

	/**
	 * DatastoreのAllocateIdsの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_datastore_v3_AllocateIds(AllocateIdsRequest requestPb) {
		return null;
	}

	/**
	 * DatastoreのAllocateIdsの後処理を行う。
	 * @param requestPb
	 * @param responsePb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_datastore_v3_AllocateIds(AllocateIdsRequest requestPb,
			AllocateIdsResponse responsePb) {
		return null;
	}

	/**
	 * DatastoreのPutの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_datastore_v3_Put(PutRequest requestPb) {
		return null;
	}

	/**
	 * DatastoreのPutの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_datastore_v3_Put(PutRequest requestPb, PutResponse responsePb) {
		return null;
	}

	/**
	 * DatastoreのGetの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_datastore_v3_Get(GetRequest requestPb) {
		return null;
	}

	/**
	 * DatastoreのGetの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_datastore_v3_Get(GetRequest requestPb, GetResponse responsePb) {
		return null;
	}

	/**
	 * DatastoreのDeleteの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_datastore_v3_Delete(DeleteRequest requestPb) {
		return null;
	}

	/**
	 * DatastoreのDeleteの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_datastore_v3_Delete(DeleteRequest requestPb, DeleteResponse responsePb) {
		return null;
	}

	/**
	 * DatastoreのRunQueryの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_datastore_v3_RunQuery(Query requestPb) {
		return null;
	}

	/**
	 * DatastoreのRunQueryの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_datastore_v3_RunQuery(Query requestPb, QueryResult responsePb) {
		return null;
	}

	/**
	 * DatastoreのNextの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_datastore_v3_Next(NextRequest requestPb) {
		return null;
	}

	/**
	 * DatastoreのNextの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_datastore_v3_Next(NextRequest requestPb, QueryResult responsePb) {
		return null;
	}

	/**
	 * DatastoreのCommitの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_datastore_v3_Commit(Transaction requestPb) {
		return null;
	}

	/**
	 * DatastoreのCommitの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_datastore_v3_Commit(Transaction requestPb, CommitResponse responsePb) {
		return null;
	}

	/**
	 * DatastoreのRollbackの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_datastore_v3_Rollback(Transaction requestPb) {
		return null;
	}

	/**
	 * DatastoreのRollbackの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_datastore_v3_Rollback(Transaction requestPb, CommitResponse responsePb) {
		return null;
	}

	/**
	 * MemcacheのSetの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_memcache_Set(MemcacheSetRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのSetの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_memcache_Set(MemcacheSetRequest requestPb, MemcacheSetResponse responsePb) {
		return null;
	}

	/**
	 * MemcacheのGetの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_memcache_Get(MemcacheGetRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのGetの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_memcache_Get(MemcacheGetRequest requestPb, MemcacheGetResponse responsePb) {
		return null;
	}

	/**
	 * MemcacheのDeleteの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_memcache_Delete(MemcacheDeleteRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのDeleteの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_memcache_Delete(MemcacheDeleteRequest requestPb,
			MemcacheDeleteResponse responsePb) {
		return null;
	}

	/**
	 * MemcacheのFlushAllの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_memcache_FlushAll(MemcacheFlushRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのFlushAllの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_memcache_FlushAll(MemcacheFlushRequest requestPb,
			MemcacheFlushResponse responsePb) {
		return null;
	}

	/**
	 * MemcacheのBatchIncrementの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_memcache_BatchIncrement(MemcacheBatchIncrementRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのBatchIncrementの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_memcache_BatchIncrement(MemcacheBatchIncrementRequest requestPb,
			MemcacheBatchIncrementResponse responsePb) {
		return null;
	}

	/**
	 * MemcacheのIncrementの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_memcache_Increment(MemcacheIncrementRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのIncrementの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_memcache_Increment(MemcacheIncrementRequest requestPb,
			MemcacheIncrementResponse responsePb) {
		return null;
	}

	/**
	 * MemcacheのStatsの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_memcache_Stats(MemcacheStatsRequest requestPb) {
		return null;
	}

	/**
	 * MemcacheのStatsの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_memcache_Stats(MemcacheStatsRequest requestPb, MemcacheSetResponse responsePb) {
		return null;
	}

	/**
	 * Search APIのIndexDocumentの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_search_IndexDocument(IndexDocumentRequest requestPb) {
		return null;
	}

	/**
	 * Search APIのIndexDocumentの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_search_IndexDocument(IndexDocumentRequest requestPb,
			IndexDocumentResponse responsePb) {
		return null;
	}

	/**
	 * Search APIのSearchの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_search_Search(SearchRequest requestPb) {
		return null;
	}

	/**
	 * Search APIのSearchの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_search_Search(SearchRequest requestPb, SearchResponse responsePb) {
		return null;
	}

	/**
	 * Search APIのDeleteDocumentの前処理を行う。
	 * @param requestPb
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> pre_search_DeleteDocument(DeleteDocumentRequest requestPb) {
		return null;
	}

	/**
	 * Search APIのDeleteDocumentの後処理を行う。
	 * @param requestPb
	 * @param responsePb 
	 * @return 処理の返り値 or null
	 * @author vvakame
	 */
	public byte[] post_search_DeleteDocument(DeleteDocumentRequest requestPb,
			DeleteDocumentResponse responsePb) {
		return null;
	}
}
