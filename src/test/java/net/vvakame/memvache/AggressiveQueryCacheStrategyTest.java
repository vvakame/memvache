package net.vvakame.memvache;

import java.util.List;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.EntityQuery;
import org.slim3.datastore.KindlessQuery;
import org.slim3.tester.ControllerTestCase;

import com.google.appengine.api.datastore.Entity;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

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
		assertThat(entities.size(), is(3));
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
