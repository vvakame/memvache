package net.vvakame.memvache.internal;

import net.vvakame.memvache.DebugDelegate;

import org.junit.Test;
import org.slim3.datastore.Datastore;
import org.slim3.tester.ControllerTestCase;

import com.google.appengine.api.datastore.Transaction;

/**
 * {@link RpcVisitor} のテスト 兼 各種RPC発生実験。
 * @author vvakame
 */
public class RpcVisitorTest extends ControllerTestCase {

	DebugDelegate debugDelegate;


	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void datastore_v3_Get_with_Tx() {
		Transaction tx = Datastore.beginTransaction();
		Datastore.getOrNull(Datastore.createKey("Hoge", 1));
		tx.rollback();
	}

	/**
	 * テストケース。
	 * @author vvakame
	 */
	@Test
	public void datastore_v3_Get_without_Tx() {
		Datastore.getOrNull(Datastore.createKey("Hoge", 1));
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		debugDelegate = DebugDelegate.install();
	}

	@Override
	public void tearDown() throws Exception {
		debugDelegate.uninstall();

		super.tearDown();
	}
}
