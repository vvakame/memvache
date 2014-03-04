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
	public void init(FilterConfig filterConfig) {
		boolean enableGetPutCache = false;
		boolean enableSafetyGetCache = true;
		boolean enableQueryKeysOnly = true;
		boolean enableAggressiveQueryCache = false;

		boolean debug = false;

		try {
			String getPutCache = filterConfig.getInitParameter("enableGetPutCacheStrategy");
			if (!isEmpty(getPutCache)) {
				enableGetPutCache = Boolean.valueOf(getPutCache);
			}
			String safetyGetCache = filterConfig.getInitParameter("enableSafetyGetCacheStrategy");
			if (!isEmpty(safetyGetCache)) {
				enableSafetyGetCache = Boolean.valueOf(safetyGetCache);
			}
			String queryKeysOnly = filterConfig.getInitParameter("enableQueryKeysOnlyStrategy");
			if (!isEmpty(queryKeysOnly)) {
				enableQueryKeysOnly = Boolean.valueOf(queryKeysOnly);
			}
			String aggressiveQueryCache =
					filterConfig.getInitParameter("enableAggressiveQueryCacheStrategy");
			if (!isEmpty(aggressiveQueryCache)) {
				enableAggressiveQueryCache = Boolean.valueOf(aggressiveQueryCache);
			}
			String debugMode = filterConfig.getInitParameter("enableDebugMode");
			if (!isEmpty(debugMode)) {
				debug = Boolean.valueOf(debugMode);
			}
		} catch (Exception e) {
		}
		if (enableGetPutCache && enableSafetyGetCache) {
			enableGetPutCache = false;
			logger.warning("Conflicting settings: enableGetPutCache disabled.");
		}
		if (enableGetPutCache) {
			MemvacheDelegate.addStrategy(GetPutCacheStrategy.class);
		} else {
			MemvacheDelegate.removeStrategy(GetPutCacheStrategy.class);
		}
		if (enableSafetyGetCache) {
			MemvacheDelegate.addStrategy(SafetyGetCacheStrategy.class);
		} else {
			MemvacheDelegate.removeStrategy(SafetyGetCacheStrategy.class);
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
		RpcVisitor.debug = debug;
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

	static boolean isEmpty(String str) {
		return str == null || "".equals(str);
	}
}
