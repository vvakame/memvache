package net.vvakame.memvache;

import java.util.List;

import net.vvakame.memvache.QueryKeysOnlyStrategy;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.tester.ControllerTestCase;

import com.google.appengine.api.datastore.Entity;

/**
 * {@link QueryKeysOnlyStrategy} のテストケース。
 * @author vvakame
 */
public class QueryKeysOnlyStrategyTest extends ControllerTestCase {

	DebugDelegate debugDelegate;


	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void query_withKeysOnly() {
		{
			Entity entity = new Entity("hoge", 1);
			entity.setProperty("v", "val1");
		}
		{
			Entity entity = new Entity("hoge", 2);
			entity.setProperty("v", "val2");
		}
		{
			Entity entity = new Entity("hoge", 3);
			entity.setProperty("v", "val3");
		}

		List<Entity> entityList = Datastore.query("hoge").asEntityList();
		entityList.iterator().next();
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		debugDelegate = DebugDelegate.install();
		debugDelegate.setVisitor(new QueryKeysOnlyStrategy());
	}

	@Override
	public void tearDown() throws Exception {
		debugDelegate.uninstall();

		super.tearDown();
	}
}
