package com.google.appengine.api.datastore;

import java.util.Collection;

import com.google.storage.onestore.v3.OnestoreEntity;
import com.google.storage.onestore.v3.OnestoreEntity.EntityProto;

/**
 * {@link EntityTranslator} のメソッドをPackageを横断して使うためのラッパクラス。
 * @author vvakame
 */
public class EntityTranslatorPublic {

	/**
	 * {@link EntityProto} と {@link Projection} のコレクションから {@link Entity} に変換して返す。
	 * @param proto
	 * @param projections
	 * @return 変換後 {@link Entity}
	 * @author vvakame
	 */
	public static Entity createFromPb(OnestoreEntity.EntityProto proto,
			Collection<Projection> projections) {
		return EntityTranslator.createFromPb(proto, projections);
	}

	/**
	 * {@link EntityProto} から {@link Entity} に変換して返す。
	 * @param proto
	 * @return 変換後 {@link Entity}
	 * @author vvakame
	 */
	public static Entity createFromPb(OnestoreEntity.EntityProto proto) {
		return EntityTranslator.createFromPb(proto);
	}

	/**
	 * {@link Entity} から {@link EntityProto} に変換して返す。
	 * @param entity
	 * @return 変換後 {@link EntityProto}
	 * @author vvakame
	 */
	public static OnestoreEntity.EntityProto convertToPb(Entity entity) {
		return EntityTranslator.convertToPb(entity);
	}
}
