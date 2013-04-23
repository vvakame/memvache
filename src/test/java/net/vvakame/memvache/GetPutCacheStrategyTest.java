package net.vvakame.memvache;

import java.util.Map;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.memcache.Memcache;
import org.slim3.tester.ControllerTestCase;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.EntityTranslatorPublic;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.apphosting.api.DatastorePb;
import com.google.storage.onestore.v3.OnestoreEntity.EntityProto;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * {@link GetPutCacheStrategy} のテストケース。
 * @author vvakame
 */
public class GetPutCacheStrategyTest extends ControllerTestCase {

	MemvacheDelegate memvacheDelegate;

	RpcCounterDelegate countDelegate;


	/**
	 * テストケース。
	 * @author vvakame
	 * @throws EntityNotFoundException 
	 */
	@Test
	public void put_notAllocatedId() throws EntityNotFoundException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity entity = new Entity("hoge");
		datastore.put(entity);

		Key key = entity.getKey();
		datastore.get(key);
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void put_withTx_withCommit() {
		Transaction tx = Datastore.beginTransaction();
		Datastore.put(new Entity("hoge", 1));

		assertThat("Tx下なので0", Memcache.statistics().getItemCount(), is(0L));

		tx.commit();

		assertThat("1つput", Memcache.statistics().getItemCount(), is(1L));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void put_with2Tx_withCommit() {
		Transaction tx1 = Datastore.beginTransaction();
		Datastore.put(new Entity("hoge", 1));

		Transaction tx2 = Datastore.beginTransaction();
		Datastore.put(new Entity("hoge", 2));

		assertThat("Tx下なので0", Memcache.statistics().getItemCount(), is(0L));
		tx2.commit();
		assertThat("1つ目put", Memcache.statistics().getItemCount(), is(1L));

		tx1.commit();
		assertThat("2つ目put", Memcache.statistics().getItemCount(), is(2L));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void put_withTx_withRollback() {
		Transaction tx = Datastore.beginTransaction();
		Datastore.put(new Entity("hoge", 1));

		assertThat("Tx下なので0", Memcache.statistics().getItemCount(), is(0L));

		tx.rollback();

		assertThat("なかったことに", Memcache.statistics().getItemCount(), is(0L));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void get_existsAllCache() {
		Key key;
		{
			Entity entity = new Entity("hoge", 1);
			entity.setProperty("v1", 1);
			MemcacheService memcache = MemvacheDelegate.getMemcache();
			key = entity.getKey();
			EntityProto proto = EntityTranslatorPublic.convertToPb(entity);
			DatastorePb.GetResponse.Entity en = new DatastorePb.GetResponse.Entity();
			en.setEntity(proto);
			memcache.put(key, en);
		}

		Map<String, Integer> countMap = countDelegate.countMap;
		countMap.clear();

		Datastore.get(key);

		assertThat("あった", countMap.get("memcache@Get"), is(1));
		assertThat("あった", countMap.get("datastore_v3@Get"), is(0));
		assertThat("GetしてないのでSetなし", countMap.get("memcache@Set"), is(0));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void get_existsDefectCache() {
		Key key1;
		{
			Entity entity = new Entity("hoge", 1);
			entity.setProperty("v1", 1);
			MemcacheService memcache = MemvacheDelegate.getMemcache();
			key1 = entity.getKey();
			EntityProto entityProto = EntityTranslatorPublic.convertToPb(entity);
			com.google.apphosting.api.DatastorePb.GetResponse.Entity en =
					new DatastorePb.GetResponse.Entity();
			en.setEntity(entityProto);
			memcache.put(key1, en);
		}

		Key key2;
		{
			Entity entity = new Entity("hoge", 1);
			entity.setProperty("v1", 1);
			key2 = entity.getKey();
			Datastore.put(entity);
		}

		Map<String, Integer> countMap = countDelegate.countMap;
		countMap.clear();

		Datastore.get(key1, key2);

		assertThat("あった", countMap.get("memcache@Get"), is(1));
		assertThat("1つない", countMap.get("datastore_v3@Get"), is(1));
		assertThat("1つ新規", countMap.get("memcache@Set"), is(1));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void delete() {
		Entity entity = new Entity("hoge", 1);
		Datastore.put(entity);

		assertThat(Memcache.statistics().getItemCount(), is(1L));

		Datastore.delete(entity.getKey());

		assertThat(Memcache.statistics().getItemCount(), is(0L));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void run_RPCs() {
		Key key = Datastore.createKey("hoge", 20);
		Entity entity = new Entity(key);
		Datastore.put(entity);
		Datastore.get(key);
		Datastore.delete(key);
		Datastore.getOrNull(key);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// かならず RpcCounterDelegate が最初
		countDelegate = RpcCounterDelegate.install();

		memvacheDelegate = MemvacheDelegate.install();
		memvacheDelegate.strategies.get().clear();
		memvacheDelegate.strategies.get().add(new GetPutCacheStrategy());
	}

	@Override
	public void tearDown() throws Exception {
		memvacheDelegate.uninstall();
		countDelegate.uninstall();

		super.tearDown();
	}
}
