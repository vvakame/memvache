package net.vvakame.memvache;

import java.util.List;
import java.util.Map;

import net.vvakame.memvache.test.TestKind;
import net.vvakame.memvache.test.TestKindMeta;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.S3QueryResultList;
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
	public void case_withCursor() {
		// Memvache有無でCursorその他の値に差が出ないことを確認
		final TestKindMeta meta = TestKindMeta.get();
		for (int i = 1; i <= 200; i++) {
			TestKind test = new TestKind();
			test.setKey(Datastore.createKey(meta, i));
			test.setStr("v" + (i % 10));
			test.setKeyStr(String.valueOf(i));
			Datastore.put(test);
		}
		memvacheDelegate.uninstall();

		String encodedCursor;
		String encodedFilter;
		String encodedSorts;
		{
			S3QueryResultList<TestKind> data =
					Datastore.query(meta).filter(meta.str.equal("v1")).limit(10)
						.asQueryResultList();
			encodedCursor = data.getEncodedCursor();
			encodedFilter = data.getEncodedFilter();
			encodedSorts = data.getEncodedSorts();
			assertThat(data.size(), is(10));
			for (TestKind testKind : data) {
				assertThat(testKind.getKeyStr(), is(String.valueOf(testKind.getKey().getId())));
			}
		}

		memvacheDelegate = MemvacheDelegate.install();

		{
			S3QueryResultList<TestKind> data =
					Datastore.query(meta).filter(meta.str.equal("v1")).limit(10)
						.asQueryResultList();
			assertThat("same as no memvache", data.getEncodedCursor(), is(encodedCursor));
			assertThat("same as no memvache", data.getEncodedFilter(), is(encodedFilter));
			assertThat("same as no memvache", data.getEncodedSorts(), is(encodedSorts));
			assertThat(data.size(), is(10));
			for (TestKind testKind : data) {
				assertThat(testKind.getKeyStr(), is(String.valueOf(testKind.getKey().getId())));
			}
		}
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void case_withCursor_iterate() {
		// Memvache有無でCursorその他の値に差が出ないことを確認
		final TestKindMeta meta = TestKindMeta.get();
		for (int i = 1; i <= 200; i++) {
			TestKind test = new TestKind();
			test.setKey(Datastore.createKey(meta, i));
			test.setStr("v" + (i % 10));
			test.setKeyStr(String.valueOf(i));
			Datastore.put(test);
		}

		S3QueryResultList<TestKind> data;
		{
			data = Datastore.query(meta).filter(meta.str.equal("v1")).limit(10).asQueryResultList();
			assertThat(data.size(), is(10));
			for (TestKind testKind : data) {
				assertThat(testKind.getKeyStr(), is(String.valueOf(testKind.getKey().getId())));
			}
		}
		{
			String encodedCursor = data.getEncodedCursor();
			String encodedFilter = data.getEncodedFilter();
			String encodedSorts = data.getEncodedSorts();
			data =
					Datastore.query(meta).encodedStartCursor(encodedCursor)
						.encodedFilter(encodedFilter).encodedSorts(encodedSorts).limit(3)
						.asQueryResultList();
			assertThat(data.size(), is(3));
			for (TestKind testKind : data) {
				assertThat(testKind.getKeyStr(), is(String.valueOf(testKind.getKey().getId())));
			}
		}
		{
			String encodedCursor = data.getEncodedCursor();
			String encodedFilter = data.getEncodedFilter();
			String encodedSorts = data.getEncodedSorts();
			data =
					Datastore.query(meta).encodedStartCursor(encodedCursor)
						.encodedFilter(encodedFilter).encodedSorts(encodedSorts)
						.asQueryResultList();
			assertThat(data.size(), is(20 - 10 - 3));
			for (TestKind testKind : data) {
				assertThat(testKind.getKeyStr(), is(String.valueOf(testKind.getKey().getId())));
			}
		}
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
