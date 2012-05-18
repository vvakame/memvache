package net.vvakame.memvache.internal;

public class Pair<First, Second> {
	final public First first;
	final public Second second;

	private Pair(First first, Second second) {
		this.first = first;
		this.second = second;
	}

	public static <First, Second> Pair<First, Second> create(First first,
			Second second) {
		return new Pair<First, Second>(first, second);
	}
}
