package net.vvakame.memvache;

import net.vvakame.memvache.GetPutCacheStrategy;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.tester.ControllerTestCase;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;

/**
 * {@link GetPutCacheStrategy} のテストケース。
 * @author vvakame
 */
public class GetPutCacheStrategyTest extends ControllerTestCase {

	DebugDelegate debugDelegate;


	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void run_RPCs() {
		Key key = Datastore.createKey("hoge", 20);
		Entity entity = new Entity(key);
		Datastore.put(entity);
		Datastore.get(key);
		Datastore.delete(key);
		Datastore.getOrNull(key);
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void concurrent_Tx() {
		Transaction tx1 = Datastore.beginTransaction();
		Transaction tx2 = Datastore.beginTransaction();

		Datastore.put(tx1, new Entity("hoge", 1));
		Datastore.put(tx2, new Entity("hoge", 2));

		tx1.commit();
		tx2.rollback();
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		debugDelegate = DebugDelegate.install();
		debugDelegate.setVisitor(new GetPutCacheStrategy());
	}

	@Override
	public void tearDown() throws Exception {
		debugDelegate.uninstall();

		super.tearDown();
	}
}
