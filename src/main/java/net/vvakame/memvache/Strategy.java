package net.vvakame.memvache;

/**
 * RPCの結果に介入するための戦略を表すインタフェース。
 * @author vvakame
 */
public interface Strategy {

	/**
	 * 戦略適用時の優先順位。<br>
	 * 戦略はこの値が小さい順に適用されていく。
	 * @return 優先度
	 * @author vvakame
	 */
	public int getPriority();

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
