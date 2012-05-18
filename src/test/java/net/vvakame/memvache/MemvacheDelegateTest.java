package net.vvakame.memvache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.slim3.tester.ControllerTestCase;

public class MemvacheDelegateTest extends ControllerTestCase {

	@Test
	public void test() throws Exception {
		MemvacheDelegate delegate = MemvacheDelegate.install();

		tester.start("/");
		assertThat(tester.response.getStatus(),
				is(equalTo(HttpServletResponse.SC_OK)));

		delegate.uninstall();
	}
}
