package com.vedsoft.fluent;

import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.util.concurrent.Executor;

/**
 * Created by david on 10/2/16.
 */

public abstract class FluentDeterministicTask<TResult> extends FluentSpecifiedTask<Void, Void, TResult> {
	public FluentDeterministicTask() {
		super();
	}

	public FluentDeterministicTask(Executor executor) {
		super(executor);
	}

	@Override
	public FluentDeterministicTask<TResult> onComplete(OneParameterRunnable<TResult> onComplete) {
		return (FluentDeterministicTask<TResult>)super.onComplete(onComplete);
	}

	@Override
	public FluentDeterministicTask<TResult> onComplete(TwoParameterRunnable<IFluentTask<Void, Void, TResult>, TResult> listener) {
		return (FluentDeterministicTask<TResult>)super.onComplete(listener);
	}

	protected abstract TResult executeInBackground();

	@Override
	protected final TResult executeInBackground(Void[] params) {
		return executeInBackground();
	}
}
