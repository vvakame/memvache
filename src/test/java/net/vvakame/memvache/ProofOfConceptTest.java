package net.vvakame.memvache;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.logging.Logger;

import net.vvakame.memvache.meta.SampleModelMeta;
import net.vvakame.memvache.model.SampleModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.memcache.Memcache;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;

public class ProofOfConceptTest {

	static final Logger logger = Logger.getLogger(ProofOfConceptTest.class
			.getName());

	@Test
	public void 普通に操作() {
		SampleModel sample = new SampleModel();

		Datastore.put(sample);
		Datastore.delete(sample.getKey());
	}

	@Test
	public void Tx下で操作してコミット() {
		SampleModel sample = new SampleModel();

		Transaction tx = Datastore.beginTransaction();
		Datastore.put(sample);
		Datastore.delete(sample.getKey());
		tx.commit();
	}

	@Test
	public void Tx下で操作してロールバック() {
		SampleModel sample = new SampleModel();

		Transaction tx = Datastore.beginTransaction();
		Datastore.put(sample);
		Datastore.delete(sample.getKey());
		tx.rollback();
	}

	@Test
	public void XgTx下で操作してコミット() {
		DatastoreService db = DatastoreServiceFactory.getDatastoreService();

		TransactionOptions opt = TransactionOptions.Builder.withXG(true);
		Transaction tx = db.beginTransaction(opt);

		for (int i = 0; i < 5; i++) {
			SampleModel sample = new SampleModel();
			Datastore.put(tx, sample);
		}

		tx.commit();
	}

	@Test
	public void XgTx下でEG構成して操作してコミット() {
		final SampleModelMeta meta = SampleModelMeta.get();

		Key parentKey1;
		Key childKey1_1;
		@SuppressWarnings("unused")
		Key childKey1_2;
		{
			SampleModel model = new SampleModel();
			Datastore.put(model);
			parentKey1 = model.getKey();

			{
				SampleModel child = new SampleModel();
				child.setKey(Datastore.createKey(parentKey1, meta, "1"));
				Datastore.put(child);
				childKey1_1 = child.getKey();
			}
			{
				SampleModel child = new SampleModel();
				child.setKey(Datastore.createKey(parentKey1, meta, "2"));
				Datastore.put(child);
				childKey1_2 = child.getKey();
			}
		}

		Key parentKey2;
		Key childKey2_1;
		@SuppressWarnings("unused")
		Key childKey2_2;
		{
			SampleModel model = new SampleModel();
			Datastore.put(model);
			parentKey2 = model.getKey();

			{
				SampleModel child = new SampleModel();
				child.setKey(Datastore.createKey(parentKey2, meta, "1"));
				Datastore.put(child);
				childKey2_1 = child.getKey();
			}
			{
				SampleModel child = new SampleModel();
				child.setKey(Datastore.createKey(parentKey2, meta, "2"));
				Datastore.put(child);
				childKey2_2 = child.getKey();
			}
		}

		DatastoreService db = DatastoreServiceFactory.getDatastoreService();

		TransactionOptions opt = TransactionOptions.Builder.withXG(true);
		Transaction tx = db.beginTransaction(opt);

		SampleModel child1_1 = Datastore.get(tx, meta, childKey1_1);
		SampleModel child2_1 = Datastore.get(tx, meta, childKey2_1);

		child1_1.setTest("test1");
		child2_1.setTest("test2");

		Datastore.put(child1_1, child2_1);

		tx.commit();
	}

	@Test
	public void Namespace付put() {
		NamespaceManager.set("ns");
		Datastore.put(new SampleModel());
	}

	@Test
	public void query() {
		final SampleModelMeta meta = SampleModelMeta.get();
		Datastore.query(meta).filter(meta.test.equal("test"))
				.sort(meta.key.asc).asKeyList();
	}

	@Test
	public void queryとNamespace() {
		NamespaceManager.set("ns");
		final SampleModelMeta meta = SampleModelMeta.get();
		Datastore.query(meta).filter(meta.test.equal("test"))
				.sort(meta.key.asc).asKeyList();
	}

	@Test
	@Ignore("全部消える")
	public void memcacheのNamespace別削除の実験() {
		Memcache.put("test1", "test1");

		NamespaceManager.set("2");
		Memcache.put("test2", "test2");

		NamespaceManager.set("3");
		Memcache.put("test3", "test3");

		NamespaceManager.set("2");
		Memcache.cleanAll();

		NamespaceManager.set("3");
		Object obj = Memcache.get("test3");

		assertThat(obj, notNullValue());
	}

	LocalServiceTestHelper helper;
	DebugDelegate newDelegate;

	@Before
	public void setUp() throws Exception {

		LocalDatastoreServiceTestConfig dsConfig = new LocalDatastoreServiceTestConfig()
				.setDefaultHighRepJobPolicyUnappliedJobPercentage(0.01f);
		helper = new LocalServiceTestHelper(dsConfig);
		helper.setUp();

		@SuppressWarnings("unchecked")
		Delegate<Environment> delegate = ApiProxy.getDelegate();
		newDelegate = new DebugDelegate(delegate);
		ApiProxy.setDelegate(newDelegate);
	}

	@After
	public void tearDown() {
		ApiProxy.setDelegate(newDelegate.parent);
		helper.tearDown();
	}
}
