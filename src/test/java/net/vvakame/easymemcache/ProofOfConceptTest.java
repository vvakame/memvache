package net.vvakame.easymemcache;

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import net.vvakame.easymemcache.meta.SampleModelMeta;
import net.vvakame.easymemcache.model.SampleModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slim3.datastore.Datastore;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
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
					&& "Commit".equals(methodName)) {

				DatastorePb.Transaction txPb = new DatastorePb.Transaction();
				txPb.mergeFrom(request);

				logger.info(txPb.toString());
			} else if ("datastore_v3".equals(packageName)
					&& "Rollback".equals(methodName)) {

				DatastorePb.Transaction txPb = new DatastorePb.Transaction();
				txPb.mergeFrom(request);

				logger.info(txPb.toString());
			}
		}
	}
}
