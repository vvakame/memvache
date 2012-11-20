package net.vvakame.memvache.internal.strategy;

import net.vvakame.memvache.internal.RpcVisitor;

import com.google.apphosting.api.DatastorePb.DeleteRequest;
import com.google.apphosting.api.DatastorePb.GetRequest;
import com.google.apphosting.api.DatastorePb.PutRequest;
import com.google.apphosting.api.DatastorePb.PutResponse;
import com.google.apphosting.api.DatastorePb.Transaction;

/**
 * Datastore への 単一 Entity の Get & Put の置き換え を実装するクラス。<br>
 * EntityがPutされる時は全てMemcacheに保持してDatastoreへ。<br>
 * EntityがGetされる時はTx有りの時は素通し、それ以外の時はMemcacheを参照して無ければDatastoreへ。
 * @author vvakame
 */
public class GetPutCacheStrategy extends RpcVisitor {

	// Put Get の他に Delete の対応がいるのでは？
	// BeginTransaction, Commit, Rollback
	@Override
	public byte[] pre_datastore_v3_Get(GetRequest requestPb) {
		@SuppressWarnings("unused")
		Transaction transaction = requestPb.getTransaction();
		return null;
	}

	@Override
	public boolean post_datastore_v3_Put(PutRequest requestPb, PutResponse responsePb) {
		return false;
	}

	@Override
	public byte[] pre_datastore_v3_Delete(DeleteRequest requestPb) {
		return null;
	}
}
