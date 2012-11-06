package net.vvakame.memvache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.vvakame.memvache.internal.Pair;
import net.vvakame.memvache.internal.RpcVisitor;

import com.google.apphosting.api.DatastorePb.Query;

/**
 * RPCを行った後の非同期的処理に割り込みを行う。
 * @author vvakame
 */
class PostProcessAsync extends RpcVisitor<Pair<byte[], Future<byte[]>>, Future<byte[]>> {

	@Override
	public Future<byte[]> datastore_v3_RunQuery(Pair<byte[], Future<byte[]>> pair) {

		final byte[] requestBytes = pair.first;
		final Future<byte[]> future = pair.second;

		Query query = to_datastore_v3_RunQuery(requestBytes);
		final String kind = query.getKind();
		if (MemvacheDelegate.isIgnoreKind(kind)) {
			return future;
		}

		return new Future<byte[]>() {

			public void processDate(byte[] data) {
				if (data == null) {
					return;
				}
				MemvacheDelegate.putQueryCache(requestBytes, data);
			}

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return future.cancel(mayInterruptIfRunning);
			}

			@Override
			public byte[] get() throws InterruptedException, ExecutionException {
				byte[] data = future.get();
				processDate(data);
				return data;
			}

			@Override
			public byte[] get(long timeout, TimeUnit unit) throws InterruptedException,
					ExecutionException, TimeoutException {
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
