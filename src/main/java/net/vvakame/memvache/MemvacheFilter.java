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

import org.slim3.util.StringUtil;

/**
 * {@link MemvacheDelegate} を適用するための {@link Filter}。
 * @author vvakame
 */
public class MemvacheFilter implements Filter {

	static final Logger logger = Logger.getLogger(MemvacheFilter.class.getSimpleName());


	@Override
	public void init(FilterConfig filterConfig) {
		boolean enableGetPutCache = true;
		boolean enableQueryKeysOnly = true;
		boolean enableAggressiveQueryCache = false;

		try {
			String getPutCache = filterConfig.getInitParameter("enableGetPutCacheStrategy");
			if (!StringUtil.isEmpty(getPutCache)) {
				enableGetPutCache = Boolean.valueOf(getPutCache);
			}
			String queryKeysOnly = filterConfig.getInitParameter("enableQueryKeysOnlyStrategy");
			if (!StringUtil.isEmpty(queryKeysOnly)) {
				enableQueryKeysOnly = Boolean.valueOf(queryKeysOnly);
			}
			String aggressiveQueryCache =
					filterConfig.getInitParameter("enableAggressiveQueryCacheStrategy");
			if (!StringUtil.isEmpty(aggressiveQueryCache)) {
				enableAggressiveQueryCache = Boolean.valueOf(aggressiveQueryCache);
			}
		} catch (Exception e) {
		}
		if (enableGetPutCache) {
			MemvacheDelegate.addStrategy(GetPutCacheStrategy.class);
		} else {
			MemvacheDelegate.removeStrategy(GetPutCacheStrategy.class);
		}
		if (enableQueryKeysOnly) {
			MemvacheDelegate.addStrategy(QueryKeysOnlyStrategy.class);
		} else {
			MemvacheDelegate.removeStrategy(QueryKeysOnlyStrategy.class);
		}
		if (enableAggressiveQueryCache) {
			MemvacheDelegate.addStrategy(AggressiveQueryCacheStrategy.class);
		} else {
			MemvacheDelegate.removeStrategy(AggressiveQueryCacheStrategy.class);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		MemvacheDelegate delegate = null;
		try {
			delegate = MemvacheDelegate.install();
			preProcess(delegate);
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

	protected void preProcess(MemvacheDelegate delegate) {
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
	public void destroy() {
	}
}
