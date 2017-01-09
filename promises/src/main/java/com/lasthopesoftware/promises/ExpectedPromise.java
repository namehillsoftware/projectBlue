package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.TwoParameterAction;

import java.util.concurrent.Callable;

/**
 * Created by david on 10/17/16.
 */

public class ExpectedPromise<TResult> extends Promise<TResult> {
	public ExpectedPromise(Callable<TResult> executor) {
		super(new InternalExpectedPromiseExecutor<>(executor));
	}

	/**
	 * Created by david on 10/17/16.
	 */
	private static class InternalExpectedPromiseExecutor<TResult> implements TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> {
		private final Callable<TResult> executor;

		InternalExpectedPromiseExecutor(Callable<TResult> executor) {
			this.executor = executor;
		}

		@Override
		public void runWith(IResolvedPromise<TResult> resolve, IRejectedPromise reject) {
			try {
				resolve.withResult(executor.call());
			} catch (Exception e) {
				reject.withError(e);
			}
		}
	}
}
