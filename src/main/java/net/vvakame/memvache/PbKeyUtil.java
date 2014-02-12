package net.vvakame.memvache;

import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyTranslatorPublic;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;

class PbKeyUtil {

	public static List<Key> toKeys(List<Reference> keys) {
		List<Key> rowKeys = new ArrayList<Key>();
		for (Reference reference : keys) {
			rowKeys.add(KeyTranslatorPublic.createFromPb(reference));
		}

		return rowKeys;
	}
}
