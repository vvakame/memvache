package net.vvakame.memvache;

import java.util.List;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.tester.ControllerTestCase;

import com.google.appengine.api.datastore.Entity;

/**
 * {@link AggressiveQueryCacheStrategy} のテストケース。
 * @author vvakame
 */
public class AggressiveQueryCacheStrategyTest extends ControllerTestCase {

	DebugDelegate debugDelegate;


	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void run_RPCs() {
		{
			Entity entity = new Entity("hoge", 1);
			entity.setProperty("v", "value1");
			Datastore.put(entity);
		}
		{
			Entity entity = new Entity("hoge", 2);
			entity.setProperty("v", "value2");
			Datastore.put(entity);
		}
		{
			Entity entity = new Entity("hoge", 3);
			entity.setProperty("v", "value3");
			Datastore.put(entity);
		}

		List<Entity> entities = Datastore.query("hoge").asEntityList();
		entities.iterator().next();
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		debugDelegate = DebugDelegate.install();
		debugDelegate.setVisitor(new AggressiveQueryCacheStrategy());
	}

	@Override
	public void tearDown() throws Exception {
		debugDelegate.uninstall();

		super.tearDown();
	}
}
