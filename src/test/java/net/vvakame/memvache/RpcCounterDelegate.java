package net.vvakame.memvache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

/**
 * RPCが発生した時の Serivice@Method の回数を保持する。
 * @author vvakame
 */
class RpcCounterDelegate implements Delegate<Environment> {

	Delegate<Environment> parent;

	final public Map<String, Integer> countMap = new LinkedHashMap<String, Integer>() {

		private static final long serialVersionUID = 1L;


		@Override
		public Integer get(Object key) {
			Integer val = super.get(key);
			if (val != null) {
				return val;
			} else {
				return 0;
			}
		}
	};


	public static RpcCounterDelegate install() {
		@SuppressWarnings("unchecked")
		Delegate<Environment> originalDelegate = ApiProxy.getDelegate();
		if (originalDelegate instanceof RpcCounterDelegate == false) {
			RpcCounterDelegate newDelegate = new RpcCounterDelegate(originalDelegate);
			ApiProxy.setDelegate(newDelegate);
			return newDelegate;
		} else {
			return (RpcCounterDelegate) originalDelegate;
		}
	}

	public static void uninstall(Delegate<Environment> originalDelegate) {
		ApiProxy.setDelegate(originalDelegate);
	}

	public void uninstall() {
		ApiProxy.setDelegate(parent);
	}

	public RpcCounterDelegate(Delegate<Environment> parent) {
		this.parent = parent;
	}

	@Override
	public void flushLogs(Environment env) {
		parent.flushLogs(env);
	}

	@Override
	public List<Thread> getRequestThreads(Environment env) {
		return parent.getRequestThreads(env);
	}

	@Override
	public void log(Environment env, LogRecord log) {
		parent.log(env, log);
	}

	@Override
	public Future<byte[]> makeAsyncCall(Environment env, String packageName, String methodName,
			byte[] request, ApiConfig apiConfig) {

		process(packageName, methodName, request);

		return parent.makeAsyncCall(env, packageName, methodName, request, apiConfig);
	}

	@Override
	public byte[] makeSyncCall(Environment env, String packageName, String methodName,
			byte[] request) throws ApiProxyException {

		process(packageName, methodName, request);

		return parent.makeSyncCall(env, packageName, methodName, request);
	}

	void process(String packageName, String methodName, byte[] request) {
		final String key = packageName + "@" + methodName;
		Integer count = countMap.get(key);
		if (count == null) {
			count = 1;
		} else {
			count++;
		}
		countMap.put(key, count);
	}
}
