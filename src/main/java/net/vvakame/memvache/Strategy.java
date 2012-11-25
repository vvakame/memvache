package net.vvakame.memvache;

/**
 * RPCの結果に介入するための戦略を表すインタフェース。
 * @author vvakame
 */
interface Strategy {

	/**
	 * RPCをする前の書き換え戦略。
	 * @param service
	 * @param method
	 * @param request
	 * @return リクエストの書き換え または レスポンスの生成 または null(何もしない)
	 * @author vvakame
	 */
	public Pair<byte[], byte[]> preProcess(final String service, final String method,
			final byte[] request);

	/**
	 * RPCをした後の書き換え戦略。
	 * @param service
	 * @param method
	 * @param request
	 * @param response
	 * @return レスポンスの書き換え または null(何もしない)
	 * @author vvakame
	 */
	public byte[] postProcess(final String service, final String method, final byte[] request,
			final byte[] response);
}
