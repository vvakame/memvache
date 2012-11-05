package net.vvakame.memvache.internal;

/**
 * 2つの要素を持つペア。2要素タプル。
 * @author vvakame
 * @param <First>
 * @param <Second>
 */
public class Pair<First, Second> {

	/** 1番目の値 */
	final public First first;

	/** 2番目の値 */
	final public Second second;


	private Pair(First first, Second second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * 2つの値を組み立てて {@link Pair} を作成する。
	 * @param first
	 * @param second
	 * @return 組み立てた {@link Pair}
	 * @author vvakame
	 */
	public static <First, Second>Pair<First, Second> create(First first, Second second) {
		return new Pair<First, Second>(first, second);
	}
}
