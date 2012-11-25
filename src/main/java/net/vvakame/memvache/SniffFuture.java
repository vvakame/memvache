package net.vvakame.memvache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link Future} の処理結果を覗き見してなんらかの処理をするためのラッパクラス。
 * @author vvakame
 * @param <P>
 */
abstract class SniffFuture<P> implements Future<P> {

	final Future<P> root;


	/**
	 * the constructor.
	 * @param root 覗き見する {@link Future}
	 * @category constructor
	 */
	public SniffFuture(Future<P> root) {
		this.root = root;
	}

	/**
	 * 覗き見して行う処理。
	 * @param data 処理結果データ
	 * @return 改変後の処理結果データ
	 * @author vvakame
	 */
	public abstract P processDate(P data);

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return root.cancel(mayInterruptIfRunning);
	}

	@Override
	public P get() throws InterruptedException, ExecutionException {
		P data = root.get();
		P modified = processDate(data);
		if (modified != null) {
			return modified;
		} else {
			return data;
		}
	}

	@Override
	public P get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
			TimeoutException {
		P data = root.get(timeout, unit);
		P modified = processDate(data);
		if (modified != null) {
			return modified;
		} else {
			return data;
		}
	}

	@Override
	public boolean isCancelled() {
		return root.isCancelled();
	}

	@Override
	public boolean isDone() {
		return root.isDone();
	}
}
