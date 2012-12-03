package net.vvakame.memvache;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.tester.ControllerTestCase;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityTranslatorPublic;
import com.google.appengine.api.memcache.MemcacheService;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * {@link QueryKeysOnlyStrategy} のテストケース。
 * @author vvakame
 */
public class QueryKeysOnlyStrategyTest extends ControllerTestCase {

	MemvacheDelegate memvacheDelegate;

	RpcCounterDelegate countDelegate;


	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void queryKeysOnly_withoutCache() {
		{
			Entity entity = new Entity("hoge", 1);
			entity.setProperty("v1", 1);
			Datastore.put(entity);
		}
		{
			Entity entity = new Entity("hoge", 2);
			entity.setProperty("v2", 2);
			Datastore.put(entity);
		}

		List<Entity> entities = Datastore.query("hoge").asEntityList();
		assertThat(entities.size(), is(2));
		assertThat(entities.get(0).getKey(), is(Datastore.createKey("hoge", 1)));
		assertThat((Long) entities.get(0).getProperty("v1"), is(1L));
		assertThat(entities.get(1).getKey(), is(Datastore.createKey("hoge", 2)));
		assertThat((Long) entities.get(1).getProperty("v2"), is(2L));

		Map<String, Integer> countMap = countDelegate.countMap;
		assertThat("BatchGetで補填", countMap.get("datastore_v3@Get"), not(0));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void queryKeysOnly_withCache() {
		{
			Entity entity = new Entity("hoge", 1);
			entity.setProperty("v1", 1);
			Datastore.put(entity);
			MemcacheService memcache = MemvacheDelegate.getMemcache();
			memcache.put(entity.getKey(), EntityTranslatorPublic.convertToPb(entity));
		}
		{
			Entity entity = new Entity("hoge", 2);
			entity.setProperty("v2", 2);
			Datastore.put(entity);
			MemcacheService memcache = MemvacheDelegate.getMemcache();
			memcache.put(entity.getKey(), EntityTranslatorPublic.convertToPb(entity));
		}

		Map<String, Integer> countMap = countDelegate.countMap;
		countMap.clear();

		List<Entity> entities = Datastore.query("hoge").asEntityList();
		assertThat(entities.size(), is(2));
		assertThat(entities.get(0).getKey(), is(Datastore.createKey("hoge", 1)));
		assertThat((Long) entities.get(0).getProperty("v1"), is(1L));
		assertThat(entities.get(1).getKey(), is(Datastore.createKey("hoge", 2)));
		assertThat((Long) entities.get(1).getProperty("v2"), is(2L));

		assertThat("Memcacheから取得", countMap.get("datastore_v3@Get"), is(0));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void queryKeysOnly_defectCache() {
		{
			Entity entity = new Entity("hoge", 1);
			entity.setProperty("v1", 1);
			Datastore.put(entity);
			// MemcacheService memcache = MemvacheDelegate.getMemcache();
			// memcache.put(entity.getKey(), EntityTranslatorPublic.convertToPb(entity));
		}
		{
			Entity entity = new Entity("hoge", 2);
			entity.setProperty("v2", 2);
			Datastore.put(entity);
			MemcacheService memcache = MemvacheDelegate.getMemcache();
			memcache.put(entity.getKey(), EntityTranslatorPublic.convertToPb(entity));
		}

		Map<String, Integer> countMap = countDelegate.countMap;
		countMap.clear();

		List<Entity> entities = Datastore.query("hoge").asEntityList();
		assertThat(entities.size(), is(2));
		assertThat(entities.get(0).getKey(), is(Datastore.createKey("hoge", 1)));
		assertThat((Long) entities.get(0).getProperty("v1"), is(1L));
		assertThat(entities.get(1).getKey(), is(Datastore.createKey("hoge", 2)));
		assertThat((Long) entities.get(1).getProperty("v2"), is(2L));

		assertThat("1Entity補填", countMap.get("datastore_v3@Get"), not(0));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void queryKeysOnly() {
		{
			Entity entity = new Entity("hoge", 1);
			entity.setProperty("v1", 1);
			Datastore.put(entity);
		}
		{
			Entity entity = new Entity("hoge", 2);
			entity.setProperty("v2", 2);
			Datastore.put(entity);
		}

		Map<String, Integer> countMap = countDelegate.countMap;
		countMap.clear();
		Datastore.query("hoge").asKeyList().size();
		assertThat("素通し", countMap.size(), is(1));
		assertThat("素通し", countMap.get("datastore_v3@RunQuery"), is(1));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void queryWithReservedKind() {
		Datastore.query("__kind__").asEntityList().size();
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void query_withKeysOnly() {
		{
			Entity entity = new Entity("hoge", 1);
			entity.setProperty("v", "val1");
			Datastore.put(entity);
		}
		{
			Entity entity = new Entity("hoge", 2);
			entity.setProperty("v", "val2");
			Datastore.put(entity);
		}
		{
			Entity entity = new Entity("hoge", 3);
			entity.setProperty("v", "val3");
			Datastore.put(entity);
		}

		List<Entity> entityList = Datastore.query("hoge").asEntityList();
		entityList.iterator().next();
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// かならず RpcCounterDelegate が最初
		countDelegate = RpcCounterDelegate.install();

		memvacheDelegate = MemvacheDelegate.install();
		memvacheDelegate.strategies.clear();
		memvacheDelegate.strategies.add(new QueryKeysOnlyStrategy());
	}

	@Override
	public void tearDown() throws Exception {
		memvacheDelegate.uninstall();
		countDelegate.uninstall();

		super.tearDown();
	}
}
