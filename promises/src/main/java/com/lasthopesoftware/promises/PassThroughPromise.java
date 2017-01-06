package com.lasthopesoftware.promises;

import java.util.concurrent.Callable;

/**
 * Created by david on 1/5/17.
 */

public class PassThroughPromise<TPassThroughResult> extends ExpectedPromise<TPassThroughResult> {
	public PassThroughPromise(TPassThroughResult passThroughResult) {
		super(new PassThroughCallable<>(passThroughResult));
	}

	private static class PassThroughCallable<TPassThroughResult> implements Callable<TPassThroughResult> {
		private final TPassThroughResult passThroughResult;

		PassThroughCallable(TPassThroughResult passThroughResult) {
			this.passThroughResult = passThroughResult;
		}

		@Override
		public TPassThroughResult call() throws Exception {
			return passThroughResult;
		}
	}
}