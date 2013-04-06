package net.vvakame.memvache;

/**
 * 2つの要素を持つペア。2要素タプル。<br>
 * RPCのリクエストの改変やレスポンスの生成の受け渡しに利用する。
 * @author vvakame
 * @param <Req> 改変したリクエスト
 * @param <Resp> 生成したレスポンス
 */
public class Pair<Req, Resp> {

	/** リクエストの改変データ */
	final public Req request;

	/** レスポンスの生成データ */
	final public Resp response;


	private Pair(Req first, Resp second) {
		this.request = first;
		this.response = second;
	}

	/**
	 * リクエストの改変データを持った {@link Pair} を生成する。
	 * @param request
	 * @return リクエストの改変データ持ちの {@link Pair}
	 * @author vvakame
	 */
	public static <First, Second>Pair<First, Second> request(First request) {
		return new Pair<First, Second>(request, null);
	}

	/**
	 * レスポンスの生成データを持った {@link Pair} を生成する。
	 * @param response
	 * @return レスポンスの生成データ持ちの {@link Pair}
	 * @author vvakame
	 */
	public static <First, Second>Pair<First, Second> response(Second response) {
		return new Pair<First, Second>(null, response);
	}
}
