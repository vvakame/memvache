package net.vvakame.memvache;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * {@link MemvacheDelegate} を適用するための {@link Filter}。
 * @author vvakame
 */
public class MemvacheFilter implements Filter {

	static final Logger logger = Logger.getLogger(MemvacheFilter.class.getSimpleName());


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		MemvacheDelegate delegate = null;
		try {
			delegate = MemvacheDelegate.install();
		} catch (Throwable th) {
			logger.log(Level.INFO, "failed to create api call log.");
		} finally {
			try {
				chain.doFilter(request, response);
			} catch (Throwable th) {
				logger.log(Level.INFO, "failed to save accesslog.", th);
				doThrow(th);
			} finally {
				if (delegate != null) {
					delegate.uninstall();
				}
			}
		}
	}

	void doThrow(Throwable th) throws IOException, ServletException {
		if (th instanceof ServletException) {
			throw (ServletException) th;
		}
		if (th instanceof IOException) {
			throw (IOException) th;
		}
		throw new ServletException(th);
	}

	@Override
	public void init(FilterConfig filterConfig) {
	}

	@Override
	public void destroy() {
	}
}
