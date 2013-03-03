package net.vvakame.memvache.test;

//@javax.annotation.Generated(value = { "slim3-gen", "@VERSION@" }, date = "2013-03-03 23:16:44")
/** */
// mvn clean test が通るように
public final class TestKindMeta extends
		org.slim3.datastore.ModelMeta<net.vvakame.memvache.test.TestKind> {

	/** */
	public final org.slim3.datastore.CoreUnindexedAttributeMeta<net.vvakame.memvache.test.TestKind, byte[]> data =
			new org.slim3.datastore.CoreUnindexedAttributeMeta<net.vvakame.memvache.test.TestKind, byte[]>(
					this, "data", "data", byte[].class);

	/** */
	public final org.slim3.datastore.CoreAttributeMeta<net.vvakame.memvache.test.TestKind, com.google.appengine.api.datastore.Key> key =
			new org.slim3.datastore.CoreAttributeMeta<net.vvakame.memvache.test.TestKind, com.google.appengine.api.datastore.Key>(
					this, "__key__", "key", com.google.appengine.api.datastore.Key.class);

	/** */
	public final org.slim3.datastore.StringAttributeMeta<net.vvakame.memvache.test.TestKind> keyStr =
			new org.slim3.datastore.StringAttributeMeta<net.vvakame.memvache.test.TestKind>(this,
					"keyStr", "keyStr");

	/** */
	public final org.slim3.datastore.StringAttributeMeta<net.vvakame.memvache.test.TestKind> str =
			new org.slim3.datastore.StringAttributeMeta<net.vvakame.memvache.test.TestKind>(this,
					"str", "str");

	/** */
	public final org.slim3.datastore.StringCollectionAttributeMeta<net.vvakame.memvache.test.TestKind, java.util.List<java.lang.String>> strList =
			new org.slim3.datastore.StringCollectionAttributeMeta<net.vvakame.memvache.test.TestKind, java.util.List<java.lang.String>>(
					this, "strList", "strList", java.util.List.class);

	private static final TestKindMeta slim3_singleton = new TestKindMeta();


	/**
	 * @return the singleton
	 */
	public static TestKindMeta get() {
		return slim3_singleton;
	}

	/** */
	public TestKindMeta() {
		super("TestKind", net.vvakame.memvache.test.TestKind.class);
	}

	@Override
	public net.vvakame.memvache.test.TestKind entityToModel(
			com.google.appengine.api.datastore.Entity entity) {
		net.vvakame.memvache.test.TestKind model = new net.vvakame.memvache.test.TestKind();
		model.setData(blobToBytes((com.google.appengine.api.datastore.Blob) entity
			.getProperty("data")));
		model.setKey(entity.getKey());
		model.setKeyStr((java.lang.String) entity.getProperty("keyStr"));
		model.setStr((java.lang.String) entity.getProperty("str"));
		model.setStrList(toList(java.lang.String.class, entity.getProperty("strList")));
		return model;
	}

	@Override
	public com.google.appengine.api.datastore.Entity modelToEntity(java.lang.Object model) {
		net.vvakame.memvache.test.TestKind m = (net.vvakame.memvache.test.TestKind) model;
		com.google.appengine.api.datastore.Entity entity = null;
		if (m.getKey() != null) {
			entity = new com.google.appengine.api.datastore.Entity(m.getKey());
		} else {
			entity = new com.google.appengine.api.datastore.Entity(kind);
		}
		entity.setUnindexedProperty("data", bytesToBlob(m.getData()));
		entity.setProperty("keyStr", m.getKeyStr());
		entity.setProperty("str", m.getStr());
		entity.setProperty("strList", m.getStrList());
		return entity;
	}

	@Override
	protected com.google.appengine.api.datastore.Key getKey(Object model) {
		net.vvakame.memvache.test.TestKind m = (net.vvakame.memvache.test.TestKind) model;
		return m.getKey();
	}

	@Override
	protected void setKey(Object model, com.google.appengine.api.datastore.Key key) {
		validateKey(key);
		net.vvakame.memvache.test.TestKind m = (net.vvakame.memvache.test.TestKind) model;
		m.setKey(key);
	}

	@Override
	protected long getVersion(Object model) {
		throw new IllegalStateException(
				"The version property of the model(net.vvakame.memvache.test.TestKind) is not defined.");
	}

	@Override
	protected void assignKeyToModelRefIfNecessary(
			com.google.appengine.api.datastore.AsyncDatastoreService ds, java.lang.Object model) {
	}

	@Override
	protected void incrementVersion(Object model) {
	}

	@Override
	protected void prePut(Object model) {
	}

	@Override
	protected void postGet(Object model) {
	}

	@Override
	public String getSchemaVersionName() {
		return "slim3.schemaVersion";
	}

	@Override
	public String getClassHierarchyListName() {
		return "slim3.classHierarchyList";
	}

	@Override
	protected boolean isCipherProperty(String propertyName) {
		return false;
	}

	@Override
	protected void modelToJson(org.slim3.datastore.json.JsonWriter writer, java.lang.Object model,
			int maxDepth, int currentDepth) {
		net.vvakame.memvache.test.TestKind m = (net.vvakame.memvache.test.TestKind) model;
		writer.beginObject();
		org.slim3.datastore.json.Default encoder0 = new org.slim3.datastore.json.Default();
		if (m.getData() != null) {
			writer.setNextPropertyName("data");
			encoder0.encode(writer, new com.google.appengine.api.datastore.ShortBlob(m.getData()));
		}
		if (m.getKey() != null) {
			writer.setNextPropertyName("key");
			encoder0.encode(writer, m.getKey());
		}
		if (m.getKeyStr() != null) {
			writer.setNextPropertyName("keyStr");
			encoder0.encode(writer, m.getKeyStr());
		}
		if (m.getStr() != null) {
			writer.setNextPropertyName("str");
			encoder0.encode(writer, m.getStr());
		}
		if (m.getStrList() != null) {
			writer.setNextPropertyName("strList");
			writer.beginArray();
			for (java.lang.String v : m.getStrList()) {
				encoder0.encode(writer, v);
			}
			writer.endArray();
		}
		writer.endObject();
	}

	@Override
	protected net.vvakame.memvache.test.TestKind jsonToModel(
			org.slim3.datastore.json.JsonRootReader rootReader, int maxDepth, int currentDepth) {
		net.vvakame.memvache.test.TestKind m = new net.vvakame.memvache.test.TestKind();
		org.slim3.datastore.json.JsonReader reader = null;
		org.slim3.datastore.json.Default decoder0 = new org.slim3.datastore.json.Default();
		reader = rootReader.newObjectReader("data");
		if (m.getData() != null) {
			m.setData(decoder0.decode(reader,
					new com.google.appengine.api.datastore.ShortBlob(m.getData())).getBytes());
		} else {
			com.google.appengine.api.datastore.ShortBlob v =
					decoder0.decode(reader, (com.google.appengine.api.datastore.ShortBlob) null);
			if (v != null) {
				m.setData(v.getBytes());
			} else {
				m.setData(null);
			}
		}
		reader = rootReader.newObjectReader("key");
		m.setKey(decoder0.decode(reader, m.getKey()));
		reader = rootReader.newObjectReader("keyStr");
		m.setKeyStr(decoder0.decode(reader, m.getKeyStr()));
		reader = rootReader.newObjectReader("str");
		m.setStr(decoder0.decode(reader, m.getStr()));
		reader = rootReader.newObjectReader("strList");
		{
			java.util.ArrayList<java.lang.String> elements =
					new java.util.ArrayList<java.lang.String>();
			org.slim3.datastore.json.JsonArrayReader r = rootReader.newArrayReader("strList");
			if (r != null) {
				reader = r;
				int n = r.length();
				for (int i = 0; i < n; i++) {
					r.setIndex(i);
					java.lang.String v = decoder0.decode(reader, (java.lang.String) null);
					if (v != null) {
						elements.add(v);
					}
				}
				m.setStrList(elements);
			}
		}
		return m;
	}
}
