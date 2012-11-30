package net.vvakame.memvache;

import java.util.HashMap;
import java.util.Map;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.apphosting.api.DatastorePb.Query;
import com.google.storage.onestore.v3.OnestoreEntity.EntityProto;

class MemcacheKeyUtil {

	public static String createKindKey(StringBuilder builder, String namespace, String kind) {
		builder.append(namespace).append("@");
		builder.append(kind);

		return builder.toString();
	}

	public static String createKindKey(StringBuilder builder, Query requestPb) {
		final String kind = requestPb.getKind();
		final String namespace = requestPb.getNameSpace();

		return createKindKey(builder, namespace, kind);
	}

	public static String createQueryKey(MemcacheService memcache, Query requestPb) {
		StringBuilder builder = new StringBuilder();
		createKindKey(builder, requestPb);

		final long counter;
		{
			Object obj = memcache.get(builder.toString());
			if (obj == null) {
				counter = 0;
			} else {
				counter = (Long) obj;
			}
		}
		builder.append("@").append(requestPb.hashCode());
		builder.append("@").append(counter);

		return builder.toString();
	}

	public static Map<Key, EntityProto> conv(Map<Key, Object> map) {
		Map<Key, EntityProto> newMap = new HashMap<Key, EntityProto>();
		for (Key key : map.keySet()) {
			newMap.put(key, (EntityProto) map.get(key));
		}

		return newMap;
	}
}
