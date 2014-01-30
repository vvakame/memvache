package net.vvakame.memvache.issue24;

import java.util.Map;
import java.util.concurrent.Future;

import net.vvakame.memvache.AggressiveQueryCacheStrategy;
import net.vvakame.memvache.GetPutCacheStrategy;
import net.vvakame.memvache.MemvacheDelegate;
import net.vvakame.memvache.QueryKeysOnlyStrategy;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.tester.ControllerTestCase;
import org.slim3.util.FutureUtil;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.repackaged.com.google.common.util.Base64;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.apphosting.api.DatastorePb.GetResponse;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

/**
 * Issue24 検証用テストケース
 * @author vvakame
 */
public class DatastoreInternalErrorTest extends ControllerTestCase {

	MemvacheDelegate memvacheDelegate;


	/**
	 * とりあえずテストが通るように処理を追加してみよう。
	 * @author vvakame
	 * @throws Exception 
	 */
	@Test
	@SuppressWarnings("deprecation")
	public void プロダクション環境からエラーになるデータを引っこ抜いてきた() throws Exception {
		GetResponse pb;
		{
			String base64data =
					"CxKtBWpGahtzfm91bWVzaG91a291LWthc2FpLWRldmVsb3ByJwsSCUVxdWlwbWVudCIYMTMyMDUtXy1TSE9VS0FTRU4tXy0zMTAwDHITGg1zdG9wU3RhcnREYXRlIAAqAHIeGg1lcXVpcG1lbnRUeXBlIAAqCxoJU0hPVUtBU0VOchsaDnNoaWt1Y2hvdXNvbklkIAAqBxoFMTMyMDVyGggHGgl1cGRhdGVkQXQgACoJCMDqzc7n8LsCcg4aAmlkIAAqBhoEMzEwMHIRGgtzdG9wRW5kRGF0ZSAAKgByGggHGgljcmVhdGVkQXQgACoJCMDqzc7n8LsCcgoaAnN2IAAqAggBchQaCGdlb0hhc2hzIAEqBhoEeG43NXIVGghnZW9IYXNocyABKgcaBXhuNzU5chYaCGdlb0hhc2hzIAEqCBoGeG43NTk5chcaCGdlb0hhc2hzIAEqCRoHeG43NTk5bXIYGghnZW9IYXNocyABKgoaCHhuNzU5OW1xchkaCGdlb0hhc2hzIAEqCxoJeG43NTk5bXFzchoaCGdlb0hhc2hzIAEqDBoKeG43NTk5bXFzZXIbGghnZW9IYXNocyABKg0aC3huNzU5OW1xc2U3chwaCGdlb0hhc2hzIAEqDhoMeG43NTk5bXFzZTd3eiEaEHNoaWt1Y2hvdXNvbk5hbWUgACoLGgnpnZLmooXluIJ6IhoGcmVtYXJrIAAqFhoU5rKz6L6655S6NuS4geebrjEzLTJ6ExoNYXV0aG9yVXNlcktleSAAKgB6EhoDbG5nIAAqCSG1vu2uWGlhQHoSGgpvY2NWZXJzaW9uIAAqAggBehIaA2xhdCAAKgkh3HtdK8bjQUCCAScLEglFcXVpcG1lbnQiGDEzMjA1LV8tU0hPVUtBU0VOLV8tMzEwMAwYmJLTzufwuwIM";
			byte[] bytes = Base64.decode(base64data);
			pb = new GetResponse();
			pb.mergeFrom(bytes);
		}
		assertThat(pb.entitySize(), is(1));

		Key key =
				Datastore
					.stringToKey("ahtzfm91bWVzaG91a291LWthc2FpLWRldmVsb3ByJwsSCUVxdWlwbWVudCIYMTMyMDUtXy1TSE9VS0FTRU4tXy0zMTAwDA");
		final MemcacheService memcache = MemvacheDelegate.getMemcache();

		memcache.put(key, pb.entitys().get(0));
		Future<Map<Key, Entity>> future = Datastore.getAsMapAsync(key);
		Map<Key, Entity> entityMap = FutureUtil.get(future);
		assertThat(entityMap.size(), is(1));
		assertThat(entityMap.get(key), notNullValue());
	}

	/**
	 * メソッド名の通り
	 * @author vvakame
	 */
	@Test
	public void KeyのみEntityは流れてくるPBの構成が違うらしいことを実験() {
		Entity entity = new Entity("test", 1);
		Datastore.put(entity);
		Datastore.get(entity.getKey());
		MemvacheDelegate.getMemcache().clearAll();
		Datastore.get(entity.getKey());
		Datastore.get(entity.getKey());
	}


	LocalServiceTestHelper helper;


	@Override
	public void setUp() throws Exception {
		LocalDatastoreServiceTestConfig dsConfig =
				new LocalDatastoreServiceTestConfig().setNoStorage(true)
					.setApplyAllHighRepJobPolicy();
		helper = new LocalServiceTestHelper(dsConfig);
		helper.setUp();

		super.setUp();

		MemvacheDelegate.addStrategy(GetPutCacheStrategy.class);
		MemvacheDelegate.removeStrategy(QueryKeysOnlyStrategy.class);
		MemvacheDelegate.removeStrategy(AggressiveQueryCacheStrategy.class);

		memvacheDelegate = MemvacheDelegate.install();
	}

	@Override
	public void tearDown() throws Exception {
		memvacheDelegate.uninstall();

		super.tearDown();
		helper.tearDown();
	}
}
