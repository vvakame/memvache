package net.vvakame.easymemcache;

import java.util.List;
import java.util.concurrent.Future;

import org.junit.Test;
import org.slim3.tester.AppEngineTestCase;

import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

public class ProofOfConceptTest extends AppEngineTestCase {

	@Test
	public void test() {
		@SuppressWarnings("unchecked")
		Delegate<Environment> delegate = ApiProxy.getDelegate();
		SampleDelegate newDelegate = new SampleDelegate(delegate);
		ApiProxy.setDelegate(newDelegate);
	}

	static class SampleDelegate implements Delegate<Environment> {

		Delegate<Environment> parent;

		public SampleDelegate(Delegate<Environment> parent) {
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
		public Future<byte[]> makeAsyncCall(Environment env,
				String packageName, String methodName, byte[] request,
				ApiConfig apiConfig) {

			return parent.makeAsyncCall(env, packageName, methodName, request,
					apiConfig);
		}

		@Override
		public byte[] makeSyncCall(Environment env, String packageName,
				String methodName, byte[] request) throws ApiProxyException {

			return parent.makeSyncCall(env, packageName, methodName, request);
		}
	}
}
