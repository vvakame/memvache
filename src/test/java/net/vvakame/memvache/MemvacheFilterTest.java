package net.vvakame.memvache;

import java.util.ArrayList;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.slim3.tester.MockFilterConfig;
import org.slim3.tester.MockServletContext;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * {@link MemvacheFilter} のテストケース。
 * @author vvakame
 */
public class MemvacheFilterTest {

	/**
	 * デフォルト設定の挙動。
	 * @throws Exception
	 * @author vvakame
	 */
	@Test
	public void defaultSetting() throws Exception {
		MemvacheFilter filter = new MemvacheFilter();

		MockServletContext servletContext = new MockServletContext();
		MockFilterConfig filterConfig = new MockFilterConfig(servletContext);
		filter.init(filterConfig);

		Set<Class<? extends Strategy>> strategies = MemvacheDelegate.enabledStrategies;
		assertThat("default", strategies.size(), is(2));

		ArrayList<Class<? extends Strategy>> strategyList =
				new ArrayList<Class<? extends Strategy>>(strategies);
		assertThat(strategyList.get(0).newInstance(), instanceOf(QueryKeysOnlyStrategy.class));
		assertThat(strategyList.get(1).newInstance(), instanceOf(GetPutCacheStrategy.class));
		assertThat(RpcVisitor.debug, is(false));
	}

	/**
	 * ストラテジが0件の場合。
	 * @throws Exception
	 * @author vvakame
	 */
	@Test
	public void zero() throws Exception {
		MemvacheFilter filter = new MemvacheFilter();

		MockServletContext servletContext = new MockServletContext();
		MockFilterConfig filterConfig = new MockFilterConfig(servletContext);
		filterConfig.setInitParameter("enableGetPutCacheStrategy", "false");
		filterConfig.setInitParameter("enableQueryKeysOnlyStrategy", "false");
		filterConfig.setInitParameter("enableAggressiveQueryCacheStrategy", "false");
		filter.init(filterConfig);

		Set<Class<? extends Strategy>> strategies = MemvacheDelegate.enabledStrategies;
		assertThat("default", strategies.size(), is(0));
	}

	/**
	 * ストラテジ1件。
	 * @throws Exception
	 * @author vvakame
	 */
	@Test
	public void enableGetPutCacheStrategy() throws Exception {
		MemvacheFilter filter = new MemvacheFilter();

		MockServletContext servletContext = new MockServletContext();
		MockFilterConfig filterConfig = new MockFilterConfig(servletContext);
		filterConfig.setInitParameter("enableGetPutCacheStrategy", "true");
		filterConfig.setInitParameter("enableQueryKeysOnlyStrategy", "false");
		filterConfig.setInitParameter("enableAggressiveQueryCacheStrategy", "false");
		filter.init(filterConfig);

		Set<Class<? extends Strategy>> strategies = MemvacheDelegate.enabledStrategies;
		assertThat("default", strategies.size(), is(1));

		ArrayList<Class<? extends Strategy>> strategyList =
				new ArrayList<Class<? extends Strategy>>(strategies);
		assertThat(strategyList.get(0).newInstance(), instanceOf(GetPutCacheStrategy.class));
	}

	/**
	 * ストラテジ1件。
	 * @throws Exception
	 * @author vvakame
	 */
	@Test
	public void enableQueryKeysOnlyStrategy() throws Exception {
		MemvacheFilter filter = new MemvacheFilter();

		MockServletContext servletContext = new MockServletContext();
		MockFilterConfig filterConfig = new MockFilterConfig(servletContext);
		filterConfig.setInitParameter("enableGetPutCacheStrategy", "false");
		filterConfig.setInitParameter("enableQueryKeysOnlyStrategy", "true");
		filterConfig.setInitParameter("enableAggressiveQueryCacheStrategy", "false");
		filter.init(filterConfig);

		Set<Class<? extends Strategy>> strategies = MemvacheDelegate.enabledStrategies;
		assertThat("default", strategies.size(), is(1));

		ArrayList<Class<? extends Strategy>> strategyList =
				new ArrayList<Class<? extends Strategy>>(strategies);
		assertThat(strategyList.get(0).newInstance(), instanceOf(QueryKeysOnlyStrategy.class));
	}

	/**
	 * ストラテジ1件。
	 * @throws Exception
	 * @author vvakame
	 */
	@Test
	public void enableAggressiveQueryCacheStrategy() throws Exception {
		MemvacheFilter filter = new MemvacheFilter();

		MockServletContext servletContext = new MockServletContext();
		MockFilterConfig filterConfig = new MockFilterConfig(servletContext);
		filterConfig.setInitParameter("enableGetPutCacheStrategy", "false");
		filterConfig.setInitParameter("enableQueryKeysOnlyStrategy", "false");
		filterConfig.setInitParameter("enableAggressiveQueryCacheStrategy", "true");
		filter.init(filterConfig);

		Set<Class<? extends Strategy>> strategies = MemvacheDelegate.enabledStrategies;
		assertThat("default", strategies.size(), is(1));

		ArrayList<Class<? extends Strategy>> strategyList =
				new ArrayList<Class<? extends Strategy>>(strategies);
		assertThat(strategyList.get(0).newInstance(),
				instanceOf(AggressiveQueryCacheStrategy.class));
	}

	/**
	 * デバッグモード。
	 * @throws Exception
	 * @author vvakame
	 */
	@Test
	public void enableDebugModel() throws Exception {
		MemvacheFilter filter = new MemvacheFilter();

		MockServletContext servletContext = new MockServletContext();
		MockFilterConfig filterConfig = new MockFilterConfig(servletContext);
		filterConfig.setInitParameter("enableDebugModel", "true");
		filter.init(filterConfig);

		assertThat(RpcVisitor.debug, is(true));
	}

	/**
	 * 初期処理
	 * @author vvakame
	 */
	@Before
	public void setUp() {
		MemvacheDelegate.staticInitialize();
	}
}
