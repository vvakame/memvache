package net.vvakame.memvache.issue8;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.vvakame.memvache.MemvacheDelegate;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.tester.ControllerTestCase;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * Issue 8 再現用のテスト。
 * @author vvakame
 */
public class GetAsMapTest extends ControllerTestCase {

	MemvacheDelegate memvacheDelegate;


	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void test() {
		List<Key> keyList = new ArrayList<Key>();
		List<Entity> entityList = new ArrayList<Entity>();
		for (int i = 1; i <= 100; i++) {
			Key key = Datastore.createKey("hoge", i);
			keyList.add(key);
			if (i % 2 == 0) {
				// entity doesn't have any property.
				Entity entity = new Entity(key);
				entityList.add(entity);
			}
		}
		Datastore.put(entityList);

		Map<Key, Entity> map = Datastore.getAsMap(keyList);
		assertThat(map.size(), is(50));

		for (Key key : keyList) {
			if (key.getId() % 2 == 0) {
				Entity entity = map.get(key);
				assertThat(entity.getKey(), is(key));
			}
		}
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
