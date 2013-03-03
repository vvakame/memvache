package net.vvakame.memvache;

import net.vvakame.memvache.test.TestKind;
import net.vvakame.memvache.test.TestKindMeta;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.S3QueryResultList;
import org.slim3.tester.ControllerTestCase;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Document.Builder;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchServiceFactory;

/**
 * {@link RpcVisitor} のテスト 兼 各種RPC発生実験。
 * @author vvakame
 */
public class RpcVisitorTest extends ControllerTestCase {

	DebugDelegate debugDelegate;


	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void datastore_v3_Get_with_Tx() {
		Transaction tx = Datastore.beginTransaction();
		Datastore.getOrNull(Datastore.createKey("Hoge", 1));
		tx.rollback();
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void datastore_v3_Get_without_Tx() {
		Datastore.getOrNull(Datastore.createKey("Hoge", 1));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void datastore_v3_Get_multi() {
		NamespaceManager.set("hoge");
		Datastore.put(new Entity("Hoge", 1));
		Datastore.put(new Entity("Hoge", "s"));
		Datastore.get(Datastore.createKey("Hoge", 1), Datastore.createKey("Hoge", "s"));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void datastore_v3_Get_sameKey_but_otherEntity() {
		Key key1 = Datastore.createKey("hoge", 1);
		Key key1_1 = Datastore.createKey(key1, "hoge", 1);
		Key key1_1_1 = Datastore.createKey(key1_1, "hoge", 1);
		Key key2 = Datastore.createKey("hoge", 2);
		Key key2_1 = Datastore.createKey(key2, "hoge", 1);
		Key key2_1_3 = Datastore.createKey(key2_1, "hoge", 3);
		Datastore.put(new Entity(key1));
		Datastore.put(new Entity(key1_1));
		Datastore.put(new Entity(key1_1_1));
		Datastore.put(new Entity(key2));
		Datastore.put(new Entity(key2_1));
		Datastore.put(new Entity(key2_1_3));
		Datastore.get(key1, key1_1, key1_1_1, key2, key2_1, key2_1_3);

		Query query = new Query(key2_1_3);
		DatastoreServiceFactory.getDatastoreService().prepare(query)
			.asList(FetchOptions.Builder.withDefaults());
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void datastore_v3_Next() {
		Key parentKey = Datastore.createKey("hoge", 1);
		for (int i = 1; i <= 1000; i++) {
			Key key = Datastore.createKey(parentKey, TestKind.class, i);
			TestKind testKind = new TestKind();
			testKind.setKey(key);
			Datastore.put(testKind);
		}

		final TestKindMeta meta = TestKindMeta.get();

		S3QueryResultList<TestKind> list =
				Datastore.query(meta, parentKey).filter(meta.strList.in("hoge"))
					.sort(meta.str.desc).limit(10).asQueryResultList();
		Datastore.query(meta, parentKey).encodedStartCursor(list.getEncodedCursor()).asList()
			.size();
	}

	/**
	 * テストケース。
	 * TODO for vvakame
	 * @author vvakame
	 */
	@Test
	public void search_IndexDocument() {
		Builder builder = Document.newBuilder();
		builder.setId("tmp");
		{
			Field field = Field.newBuilder().setName("key").setText("value").build();
			builder.addField(field);
		}

		IndexSpec indexSpec = IndexSpec.newBuilder().setName("index").build();
		Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

		index.put(builder.build());
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void search_Search() {
		Builder builder = Document.newBuilder();
		builder.setId("tmp");
		{
			Field field = Field.newBuilder().setName("key").setText("value").build();
			builder.addField(field);
		}

		IndexSpec indexSpec = IndexSpec.newBuilder().setName("index").build();
		Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

		index.put(builder.build());

		String queryStr = "key:value";

		com.google.appengine.api.search.Query.Builder newBuilder =
				com.google.appengine.api.search.Query.newBuilder();
		com.google.appengine.api.search.Query query = newBuilder.build(queryStr);

		Results<ScoredDocument> results = index.search(query);
		results.getNumberFound();
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void search_DeleteDocument() {
		Builder builder = Document.newBuilder();
		builder.setId("tmp");
		{
			Field field = Field.newBuilder().setName("key").setText("value").build();
			builder.addField(field);
		}

		IndexSpec indexSpec = IndexSpec.newBuilder().setName("index").build();
		Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

		Document doc = builder.build();

		index.put(doc);

		index.delete(doc.getId());
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		debugDelegate = DebugDelegate.install();
	}

	@Override
	public void tearDown() throws Exception {
		debugDelegate.uninstall();

		super.tearDown();
	}
}
