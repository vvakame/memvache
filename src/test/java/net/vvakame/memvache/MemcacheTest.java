package net.vvakame.memvache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slim3.tester.AppEngineTestCase;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * Memcache の試験。
 * @author vvakame
 */
public class MemcacheTest extends AppEngineTestCase {

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void test() {
		final MemcacheService memcache = MemcacheServiceFactory.getMemcacheService("memvache");
		memcache.put("a", "a");
		memcache.put("b", "b");
		memcache.put("c", "c");

		List<String> keys = new ArrayList<String>();
		keys.add("b");
		keys.add("e");
		Map<String, Object> all = memcache.getAll(keys);
		assertThat(all.size(), is(1));
		assertThat((String) all.get("b"), is("b"));
	}
}
