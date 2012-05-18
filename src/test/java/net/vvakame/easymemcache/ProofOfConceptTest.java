package net.vvakame.easymemcache;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import net.vvakame.easymemcache.meta.SampleModelMeta;
import net.vvakame.easymemcache.model.SampleModel;

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
import com.google.appengine.api.memcache.MemcacheServicePb;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;
import com.google.apphosting.api.DatastorePb;

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
	SampleDelegate newDelegate;

	@Before
	public void setUp() throws Exception {

		LocalDatastoreServiceTestConfig dsConfig = new LocalDatastoreServiceTestConfig()
				.setDefaultHighRepJobPolicyUnappliedJobPercentage(0.01f);
		helper = new LocalServiceTestHelper(dsConfig);
		helper.setUp();

		@SuppressWarnings("unchecked")
		Delegate<Environment> delegate = ApiProxy.getDelegate();
		newDelegate = new SampleDelegate(delegate);
		ApiProxy.setDelegate(newDelegate);
	}

	@After
	public void tearDown() {
		ApiProxy.setDelegate(newDelegate.parent);
		helper.tearDown();
	}

	static class SampleDelegate implements Delegate<Environment> {

		Delegate<Environment> parent;

		public SampleDelegate(Delegate<Environment> parent) {
			this.parent = parent;
		}

		@Override
		public void flushLogs(Environment env) {
			parent.flushLogs(env);
		}

		@Override
		public List<Thread> getRequestThreads(Environment env) {
			return parent.getRequestThreads(env);
		}

		@Override
		public void log(Environment env, LogRecord log) {
			parent.log(env, log);
		}

		@Override
		public Future<byte[]> makeAsyncCall(Environment env,
				String packageName, String methodName, byte[] request,
				ApiConfig apiConfig) {

			process(packageName, methodName, request);

			return parent.makeAsyncCall(env, packageName, methodName, request,
					apiConfig);
		}

		@Override
		public byte[] makeSyncCall(Environment env, String packageName,
				String methodName, byte[] request) throws ApiProxyException {

			process(packageName, methodName, request);

			return parent.makeSyncCall(env, packageName, methodName, request);
		}

		static void process(String packageName, String methodName,
				byte[] request) {

			logger.info("packageName=" + packageName + ", methodName="
					+ methodName + ", dataSize=" + request.length);

			if ("datastore_v3".equals(packageName)
					&& "BeginTransaction".equals(methodName)) {

				DatastorePb.BeginTransactionRequest requestPb = new DatastorePb.BeginTransactionRequest();
				requestPb.mergeFrom(request);

				logger.info(requestPb.toString());
			} else if ("datastore_v3".equals(packageName)
					&& "Put".equals(methodName)) {

				DatastorePb.PutRequest requestPb = new DatastorePb.PutRequest();
				requestPb.mergeFrom(request);

				logger.info(requestPb.toString());
			} else if ("datastore_v3".equals(packageName)
					&& "Get".equals(methodName)) {

				DatastorePb.GetRequest requestPb = new DatastorePb.GetRequest();
				requestPb.mergeFrom(request);

				logger.info(requestPb.toString());
			} else if ("datastore_v3".equals(packageName)
					&& "Delete".equals(methodName)) {

				DatastorePb.DeleteRequest requestPb = new DatastorePb.DeleteRequest();
				requestPb.mergeFrom(request);

				logger.info(requestPb.toString());
			} else if ("datastore_v3".equals(packageName)
					&& "RunQuery".equals(methodName)) {

				{
					DatastorePb.Query requestPb = new DatastorePb.Query();
					requestPb.mergeFrom(request);

					logger.info(requestPb.toString());
				}
			} else if ("datastore_v3".equals(packageName)
					&& "Commit".equals(methodName)) {

				DatastorePb.Transaction txPb = new DatastorePb.Transaction();
				txPb.mergeFrom(request);

				logger.info(txPb.toString());
			} else if ("datastore_v3".equals(packageName)
					&& "Rollback".equals(methodName)) {

				DatastorePb.Transaction txPb = new DatastorePb.Transaction();
				txPb.mergeFrom(request);

				logger.info(txPb.toString());
			} else if ("memcache".equals(packageName)
					&& "Set".equals(methodName)) {

				try {
					MemcacheServicePb.MemcacheSetRequest pb = MemcacheServicePb.MemcacheSetRequest
							.parseFrom(request);
					logger.info(pb.toString());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else if ("memcache".equals(packageName)
					&& "Get".equals(methodName)) {

				try {
					MemcacheServicePb.MemcacheGetRequest pb = MemcacheServicePb.MemcacheGetRequest
							.parseFrom(request);
					logger.info(pb.toString());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else if ("memcache".equals(packageName)
					&& "FlushAll".equals(methodName)) {

				try {
					MemcacheServicePb.MemcacheFlushRequest pb = MemcacheServicePb.MemcacheFlushRequest
							.parseFrom(request);
					logger.info(pb.toString());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
