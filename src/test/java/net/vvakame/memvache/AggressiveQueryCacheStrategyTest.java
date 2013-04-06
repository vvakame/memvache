package net.vvakame.memvache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.vvakame.memvache.AggressiveQueryCacheStrategy.Settings;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.EntityQuery;
import org.slim3.datastore.KindlessQuery;
import org.slim3.memcache.Memcache;
import org.slim3.tester.ControllerTestCase;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * {@link AggressiveQueryCacheStrategy} のテストケース。
 * @author vvakame
 */
public class AggressiveQueryCacheStrategyTest extends ControllerTestCase {

	MemvacheDelegate memvacheDelegate;

	RpcCounterDelegate countDelegate;


	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void queryCache() {
		assertThat(Memcache.statistics().getItemCount(), is(0L));

		Datastore.query("hoge").asEntityList().size();

		assertThat("Queryのキャッシュが作成された", Memcache.statistics().getItemCount(), is(1L));

		final Map<String, Integer> countMap = countDelegate.countMap;
		// {memcache@Stats=2, datastore_v3@RunQuery=1, memcache@Get=3, memcache@Set=1}
		assertThat("Queryの回数", countMap.get("datastore_v3@RunQuery"), is(1));

		Datastore.query("hoge").asEntityList().size();

		assertThat("増えてない", countMap.get("datastore_v3@RunQuery"), is(1));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void expireQueryCache() {
		queryCache();
		final Map<String, Integer> countMap = countDelegate.countMap;
		assertThat(countMap.get("datastore_v3@RunQuery"), is(1));

		Datastore.put(new Entity("hoge"));
		assertThat("increment", Memcache.statistics().getItemCount(), is(2L));

		Datastore.query("hoge").asEntityList().size();

		assertThat("増えた", countMap.get("datastore_v3@RunQuery"), is(2));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void expireQueryCacheByAncester() {
		queryCache();
		final Map<String, Integer> countMap = countDelegate.countMap;
		assertThat(countMap.get("datastore_v3@RunQuery"), is(1));

		{
			Key parent = Datastore.createKey("hoge", 1);
			Key child = Datastore.createKey(parent, "foo", 1000);
			Datastore.put(new Entity(child));
		}
		Datastore.query("hoge").asEntityList().size();
		assertThat("増えてない", countMap.get("datastore_v3@RunQuery"), is(1));

		{
			Key parent = Datastore.createKey("foo", 1);
			Key child = Datastore.createKey(parent, "hoge", 1000);
			Datastore.put(new Entity(child));
		}

		Datastore.query("hoge").asEntityList().size();
		assertThat("増えた", countMap.get("datastore_v3@RunQuery"), is(2));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void notExpireQueryByOtherNamespace() {
		queryCache();
		final Map<String, Integer> countMap = countDelegate.countMap;
		assertThat(countMap.get("datastore_v3@RunQuery"), is(1));

		NamespaceManager.set("other");
		Datastore.put(new Entity("hoge"));

		NamespaceManager.set(null);

		Datastore.query("hoge").asEntityList().size();
		assertThat("増えない", countMap.get("datastore_v3@RunQuery"), is(1));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void expireQueryPutMultiKind() {
		Datastore.query("hoge").asEntityList().size();
		Datastore.query("fuga").asEntityList().size();

		final Map<String, Integer> countMap = countDelegate.countMap;
		assertThat(countMap.get("datastore_v3@RunQuery"), is(2));
		assertThat("query cache 2", Memcache.statistics().getItemCount(), is(2L));

		List<Entity> entities = new ArrayList<Entity>();
		entities.add(new Entity("hoge"));
		entities.add(new Entity("fuga"));
		entities.add(new Entity("hige"));
		Datastore.put(entities);
		assertThat("3Kind inc", Memcache.statistics().getItemCount(), is(5L));

		Datastore.query("hoge").asEntityList().size();
		Datastore.query("fuga").asEntityList().size();

		assertThat(countMap.get("datastore_v3@RunQuery"), is(4));
		assertThat("query cache 2", Memcache.statistics().getItemCount(), is(7L));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void ignoreKind() {
		Settings settings = Settings.getInstance();

		settings.ignoreKinds.clear();
		settings.ignoreKinds.add("hoge");

		assertThat(Memcache.statistics().getItemCount(), is(0L));

		Datastore.put(new Entity("hoge"));
		Datastore.query("hoge").asEntityList().size();
		assertThat(Memcache.statistics().getItemCount(), is(0L));

		Datastore.query("fuga").asEntityList().size();
		assertThat(Memcache.statistics().getItemCount(), is(1L));

		settings.ignoreKinds.clear();
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void kindlessAncestorQuery() {
		Entity parent = new Entity("parent", 1);
		Datastore.put(parent);
		for (int i = 1; i <= 2; i++) {
			Entity child = new Entity("child", i, parent.getKey());
			Datastore.put(child);
			for (int j = 1; j <= 2; j++) {
				Entity grandChild = new Entity("grandChild", i * 10 + j, child.getKey());
				Datastore.put(grandChild);
			}
		}
		KindlessQuery query = Datastore.query(parent.getKey());
		assertThat(query.asEntityList().size(), is(7));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void ancestorQuery() {
		Entity parent = new Entity("parent", 1);
		Datastore.put(parent);
		for (int i = 1; i <= 2; i++) {
			Entity child = new Entity("child", i, parent.getKey());
			Datastore.put(child);
			for (int j = 1; j <= 2; j++) {
				Entity grandChild = new Entity("grandChild", i * 10 + j, child.getKey());
				Datastore.put(grandChild);
			}
		}
		EntityQuery query = Datastore.query("child", parent.getKey());
		assertThat(query.asEntityList().size(), is(2));
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void prefetchSize() {
		for (int i = 1; i <= 2; i++) {
			Entity child = new Entity("kind", i);
			Datastore.put(child);
		}
		EntityQuery query = Datastore.query("kind");
		// query.prefetchSize(1000);
		assertThat(query.asEntityList().size(), is(2));
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// かならず RpcCounterDelegate が最初
		countDelegate = RpcCounterDelegate.install();

		memvacheDelegate = MemvacheDelegate.install();
		memvacheDelegate.strategies.clear();
		memvacheDelegate.strategies.add(new AggressiveQueryCacheStrategy());
	}

	@Override
	public void tearDown() throws Exception {
		memvacheDelegate.uninstall();
		countDelegate.uninstall();

		super.tearDown();
	}
}
