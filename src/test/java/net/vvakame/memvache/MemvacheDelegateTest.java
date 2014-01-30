package net.vvakame.memvache;

import java.util.List;

import net.vvakame.memvache.test.TestKind;
import net.vvakame.memvache.test.TestKindMeta;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.memcache.Memcache;
import org.slim3.tester.AppEngineTestCase;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * {@link MemvacheDelegate} 全体に対してのテストケース。<br>
 * 個別のテストは適切な所に作成すること。
 * @author vvakame
 */
public class MemvacheDelegateTest extends AppEngineTestCase {

	MemvacheDelegate memvacheDelegate;


	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void test() {
		final TestKindMeta meta = TestKindMeta.get();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 500; i++) {
			builder.append(i % 10);
		}
		final String str = builder.toString();
		final byte[] data = new byte[100 * 1024];
		for (int i = 1; i <= 200; i++) {
			TestKind model = new TestKind();
			model.setKey(Datastore.createKey(meta, i));
			model.setStr(str);
			model.setKeyStr(Datastore.keyToString(model.getKey()));
			model.setData(data);

			Datastore.put(model);
		}
		System.out.println("modelList putted");
		Memcache.cleanAll();

		List<TestKind> list = Datastore.query(meta).prefetchSize(20).asList();
		assertThat(list.size(), is(200));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void priority() {
		memvacheDelegate.uninstall();

		MemvacheDelegate.enabledStrategies.clear();
		MemvacheDelegate.addStrategy(QueryKeysOnlyStrategy.class);
		MemvacheDelegate.addStrategy(AggressiveQueryCacheStrategy.class);
		MemvacheDelegate.addStrategy(GetPutCacheStrategy.class);

		memvacheDelegate = MemvacheDelegate.install();

		List<Strategy> strategies = memvacheDelegate.strategies.get();
		assertThat(strategies.get(0), instanceOf(AggressiveQueryCacheStrategy.class));
		assertThat(strategies.get(1), instanceOf(QueryKeysOnlyStrategy.class));
		assertThat(strategies.get(2), instanceOf(GetPutCacheStrategy.class));
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		memvacheDelegate = MemvacheDelegate.install();
	}

	@Override
	public void tearDown() throws Exception {
		memvacheDelegate.uninstall();

		super.tearDown();
	}
}
