package com.vedsoft.fluent;

import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.util.concurrent.Executor;

/**
 * Created by david on 10/2/16.
 */

public abstract class FluentCallable<TResult> extends FluentSpecifiedTask<Void, Void, TResult> {
	public FluentCallable() {
		super();
	}

	public FluentCallable(Executor executor) {
		super(executor);
	}

	@Override
	public FluentCallable<TResult> onComplete(OneParameterRunnable<TResult> onComplete) {
		return (FluentCallable<TResult>)super.onComplete(onComplete);
	}

	@Override
	public FluentCallable<TResult> onComplete(TwoParameterRunnable<IFluentTask<Void, Void, TResult>, TResult> listener) {
		return (FluentCallable<TResult>)super.onComplete(listener);
	}

	protected abstract TResult executeInBackground();

	@Override
	protected final TResult executeInBackground(Void[] params) {
		return executeInBackground();
	}
}
