package net.vvakame.memvache;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import net.vvakame.memvache.internal.RpcVisitor;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.apphosting.api.DatastorePb.PutRequest;
import com.google.apphosting.api.DatastorePb.Query;
import com.google.storage.onestore.v3.OnestoreEntity.EntityProto;
import com.google.storage.onestore.v3.OnestoreEntity.Path;
import com.google.storage.onestore.v3.OnestoreEntity.Path.Element;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;

/**
 * RPCを行う前に割り込み処理を行う。
 * @author vvakame
 */
class PreProcess extends RpcVisitor<byte[], byte[]> {

	@Override
	public byte[] datastore_v3_Put(byte[] requestBytes) {
		final PutRequest pb = to_datastore_v3_Put(requestBytes);

		final MemcacheService memcache = MemvacheDelegate.getMemcache();
		final Set<String> memcacheKeys = new HashSet<String>();

		for (EntityProto entity : pb.entitys()) {
			final Reference key = entity.getMutableKey();
			final String namespace = key.getNameSpace();
			final Path path = key.getPath();
			for (Element element : path.mutableElements()) {
				final String kind = element.getType();

				if (MemvacheDelegate.isIgnoreKind(kind)) {
					continue;
				}

				StringBuilder builder = new StringBuilder();
				builder.append(namespace).append("@");
				builder.append(kind);

				memcacheKeys.add(builder.toString());
			}
		}
		if (memcacheKeys.size() == 1) {
			memcache.increment(memcacheKeys.iterator().next(), 1, 0L);
		} else if (memcacheKeys.size() != 0) {
			// memcache.incrementAll(memcacheKeys, 1, 0L);
			// TODO is this broken method? ↑
			for (String key : memcacheKeys) {
				memcache.increment(key, 1, 0L);
			}
		}

		return null;
	}

	@Override
	public byte[] datastore_v3_RunQuery(byte[] requestBytes) {
		final Query pb = to_datastore_v3_RunQuery(requestBytes);

		final MemcacheService memcache = MemvacheDelegate.getMemcache();

		final String namespace = pb.getNameSpace();
		final String kind = pb.getKind();
		if (MemvacheDelegate.isIgnoreKind(kind)) {
			return null;
		}

		StringBuilder builder = new StringBuilder();
		builder.append(namespace).append("@");
		builder.append(kind);

		final long counter;
		{
			Object obj = memcache.get(builder.toString());
			if (obj == null) {
				counter = 0;
			} else {
				counter = (Long) obj;
			}
		}
		builder.append("@").append(pb.hashCode());
		builder.append("@").append(counter);

		MemvacheDelegate.logger.log(Level.INFO, builder.toString());

		return (byte[]) memcache.get(builder.toString());
	}
}
