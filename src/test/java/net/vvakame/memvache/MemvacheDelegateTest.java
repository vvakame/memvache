package net.vvakame.memvache;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.memcache.Memcache;
import org.slim3.tester.ControllerTestCase;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;

public class MemvacheDelegateTest extends ControllerTestCase {

	RpcCounterDelegate counter;
	DebugDelegate debugDelegate;
	MemvacheDelegate delegate;

	@Test
	public void readConfigure() throws Exception {
		assertThat("default 300, 読めてたら100", MemvacheDelegate.expireSecond,
				is(100));
		assertThat(MemvacheDelegate.ignoreKindSet.size(), is(2));
	}

	@Test
	public void put_singleEntity_noNameSpace() throws Exception {
		assertThat(Memcache.statistics().getItemCount(), is(0L));

		{
			Entity entity = new Entity("test");
			Datastore.put(entity);
		}

		assertThat("新規カウンタ+1", Memcache.statistics().getItemCount(), is(1L));

		NamespaceManager.set("memvache");
		assertThat("カウンタが0→1", (Long) Memcache.get("@test"), is(1L));
	}

	@Test
	public void put_singleEntity_hasNameSpace() throws Exception {
		assertThat(Memcache.statistics().getItemCount(), is(0L));

		{
			NamespaceManager.set("hoge");
			Entity entity = new Entity("test");
			Datastore.put(entity);
		}

		assertThat("新規カウンタ+1", Memcache.statistics().getItemCount(), is(1L));

		NamespaceManager.set("memvache");
		assertThat("カウンタが0→1", (Long) Memcache.get("hoge@test"), is(1L));
	}

	@Test
	public void put_multiEntity_sameKind() throws Exception {
		assertThat(Memcache.statistics().getItemCount(), is(0L));

		{
			Entity entityA = new Entity("test");
			Entity entityB = new Entity("test");
			Datastore.put(entityA, entityB);
		}

		assertThat("新規カウンタ+1", Memcache.statistics().getItemCount(), is(1L));

		NamespaceManager.set("memvache");
		assertThat("カウンタが0→1", (Long) Memcache.get("@test"), is(1L));
	}

	@Test
	public void put_multiEntity_otherKind() throws Exception {
		assertThat(Memcache.statistics().getItemCount(), is(0L));

		{
			Entity entityA = new Entity("test1");
			Entity entityB = new Entity("test2");
			Datastore.put(entityA, entityB);
		}

		assertThat("新規カウンタ+1", Memcache.statistics().getItemCount(), is(2L));

		NamespaceManager.set("memvache");
		assertThat("カウンタが0→1", (Long) Memcache.get("@test1"), is(1L));
		assertThat("カウンタが0→1", (Long) Memcache.get("@test2"), is(1L));
	}

	@Test
	public void runQuery() {

		{
			Entity entityA = new Entity("test");
			Entity entityB = new Entity("test");
			Datastore.put(entityA, entityB);
		}
		assertThat("Putでカウンタ追加", Memcache.statistics().getItemCount(), is(1L));
		assertThat("まだRunQueryは走ってない",
				counter.countMap.get("datastore_v3@RunQuery"), is(0));

		{
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			List<Entity> list = ds.prepare(new Query("test")).asList(
					FetchOptions.Builder.withDefaults());
			assertThat(list.size(), is(2));

		}
		assertThat("RunQueryのキャッシュ+1回", Memcache.statistics().getItemCount(),
				is(2L));
		assertThat("RunQuery1回実行",
				counter.countMap.get("datastore_v3@RunQuery"), is(1));

		{
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			List<Entity> list = ds.prepare(new Query("test")).asList(
					FetchOptions.Builder.withDefaults());
			assertThat(list.size(), is(2));

		}
		assertThat("RunQueryキャッシュからHit", Memcache.statistics().getItemCount(),
				is(2L));
		assertThat("RunQuery実行増えてない",
				counter.countMap.get("datastore_v3@RunQuery"), is(1));

		{
			Entity entityA = new Entity("test");
			Entity entityB = new Entity("test");
			Datastore.put(entityA, entityB);
		}
		{
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			List<Entity> list = ds.prepare(new Query("test")).asList(
					FetchOptions.Builder.withDefaults());
			assertThat(list.size(), is(4));

		}
		assertThat("Putでカウンタ更新, 新規RunQuery+1回", Memcache.statistics()
				.getItemCount(), is(3L));
		assertThat("RunQuery2回目実行",
				counter.countMap.get("datastore_v3@RunQuery"), is(2));
	}

	@Test
	public void disable() {

		{
			Entity entityA = new Entity("test");
			Entity entityB = new Entity("test");
			Datastore.put(entityA, entityB);
		}
		assertThat("Putでカウンタ追加", Memcache.statistics().getItemCount(), is(1L));
		assertThat("まだRunQueryは走ってない",
				counter.countMap.get("datastore_v3@RunQuery"), is(0));

		{
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			List<Entity> list = ds.prepare(new Query("test")).asList(
					FetchOptions.Builder.withDefaults());
			assertThat(list.size(), is(2));

		}
		assertThat("RunQueryのキャッシュ+1回", Memcache.statistics().getItemCount(),
				is(2L));
		assertThat("RunQuery1回実行",
				counter.countMap.get("datastore_v3@RunQuery"), is(1));

		MemvacheDelegate.get().disable();
		{
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			List<Entity> list = ds.prepare(new Query("test")).asList(
					FetchOptions.Builder.withDefaults());
			assertThat(list.size(), is(2));

		}
		assertThat("変動なし", Memcache.statistics().getItemCount(), is(2L));
		assertThat("Memvache無しで生実行",
				counter.countMap.get("datastore_v3@RunQuery"), is(2));

		{
			Entity entityA = new Entity("test2");
			Entity entityB = new Entity("test3");
			Datastore.put(entityA, entityB);
		}
		assertThat("Putは動いてる+2回", Memcache.statistics().getItemCount(), is(4L));

		MemvacheDelegate.get().enable();
		{
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			List<Entity> list = ds.prepare(new Query("test")).asList(
					FetchOptions.Builder.withDefaults());
			assertThat(list.size(), is(2));

		}
		assertThat("RunQuery実行増えてない",
				counter.countMap.get("datastore_v3@RunQuery"), is(2));
	}

	@Test
	public void ignoreKind() {
		// ignore1, ignore2 が除外対象

		{
			Entity entity = new Entity("ignore1");
			Datastore.put(entity);
		}
		assertThat("無視される", Memcache.statistics().getItemCount(), is(0L));

		{
			Entity entity = new Entity("test");
			Datastore.put(entity);
		}
		assertThat("カウント更新+1回", Memcache.statistics().getItemCount(), is(1L));

		{
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			List<Entity> list = ds.prepare(new Query("test")).asList(
					FetchOptions.Builder.withDefaults());
			assertThat(list.size(), is(1));

		}
		assertThat("キャッシュなし+1回", Memcache.statistics().getMissCount(), is(1L));
		assertThat("RunQuery実行", counter.countMap.get("datastore_v3@RunQuery"),
				is(1));

		{
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			List<Entity> list = ds.prepare(new Query("test")).asList(
					FetchOptions.Builder.withDefaults());
			assertThat(list.size(), is(1));

		}
		assertThat("キャッシュ利用", counter.countMap.get("datastore_v3@RunQuery"),
				is(1));

		{
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			List<Entity> list = ds.prepare(new Query("ignore1")).asList(
					FetchOptions.Builder.withDefaults());
			assertThat(list.size(), is(1));

		}
		assertThat("RunQuery実行", counter.countMap.get("datastore_v3@RunQuery"),
				is(2));

		{
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			List<Entity> list = ds.prepare(new Query("ignore1")).asList(
					FetchOptions.Builder.withDefaults());
			assertThat(list.size(), is(1));

		}
		assertThat("キャッシュ無視で再実行",
				counter.countMap.get("datastore_v3@RunQuery"), is(3));
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		counter = RpcCounterDelegate.install(); // 一番最初にやらないと現実のRPC数と一致しない
		delegate = MemvacheDelegate.install();
		debugDelegate = DebugDelegate.install();
	}

	@Override
	public void tearDown() throws Exception {
		debugDelegate.uninstall();
		delegate.uninstall();
		counter.uninstall();

		super.tearDown();
	}
}
