package net.vvakame.memvache;

import net.vvakame.memvache.internal.Pair;
import net.vvakame.memvache.internal.RpcVisitor;

/**
 * RPCを行った後の同期的処理に割り込みを行う。
 * @author vvakame
 */
class PostProcessSync extends RpcVisitor<Pair<byte[], byte[]>, byte[]> {

	@Override
	public byte[] datastore_v3_RunQuery(Pair<byte[], byte[]> pair) {

		final byte[] requestBytes = pair.first;
		final byte[] data = pair.second;

		MemvacheDelegate.putQueryCache(requestBytes, data);

		return data;
	}
}
