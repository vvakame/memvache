package net.vvakame.memvache.usage.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;
import org.slim3.datastore.Datastore;
import org.slim3.memcache.Memcache;
import org.slim3.util.FutureUtil;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

public class Issue24TestController extends Controller {

	@Override
	protected Navigation run() throws Exception {
		Memcache.cleanAll();

		Key key;
		{
			Entity entity = new Entity("hoge");
			entity.setProperty("str", "String!");

			Datastore.put(entity);
			key = entity.getKey();

			response.getOutputStream().write(
					(entity.getKey().toString() + " was wrote\n").getBytes());
		}

		// Put時キャッシュ
		Datastore.getOrNull(key);

		Memcache.cleanAll();

		Datastore.getOrNull(key);
		// Get時キャッシュ
		Datastore.getOrNull(key);

		List<Entity> list = Datastore.query("hoge").asEntityList();
		List<Key> keyList = new ArrayList<>();
		for (Entity entity : list) {
			keyList.add(entity.getKey());
		}

		Future<Map<Key, Entity>> future1 = Datastore.getAsMapAsync(keyList);
		Future<Map<Key, Entity>> future2 = Datastore.getAsMapAsync(keyList);
		Future<Map<Key, Entity>> future3 = Datastore.getAsMapAsync(keyList);
		Map<Key, Entity> map = FutureUtil.getQuietly(future1);
		response.getOutputStream().write((map.size() + " entities read\n").getBytes());

		FutureUtil.getQuietly(future3);
		FutureUtil.getQuietly(future2);

		return null;
	}
}
