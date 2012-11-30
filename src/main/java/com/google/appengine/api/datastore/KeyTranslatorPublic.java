package com.google.appengine.api.datastore;

import com.google.storage.onestore.v3.OnestoreEntity;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;

/**
 * {@link KeyTranslator} のメソッドをPackageを横断して使うためのラッパクラス。
 * @author vvakame
 */
public class KeyTranslatorPublic {

	/**
	 * {@link Reference} から {@link Key} に変換して返す。
	 * @param reference
	 * @return 変換後 {@link Key}
	 * @author vvakame
	 */
	public static Key createFromPb(OnestoreEntity.Reference reference) {
		return KeyTranslator.createFromPb(reference);
	}

	/**
	 * {@link Key} から {@link Reference} に変換して返す。
	 * @param key
	 * @return 変換後 {@link Reference}
	 * @author vvakame
	 */
	public static OnestoreEntity.Reference convertToPb(Key key) {
		return KeyTranslator.convertToPb(key);
	}
}
