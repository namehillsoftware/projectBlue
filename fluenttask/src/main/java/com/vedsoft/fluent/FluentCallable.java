package com.vedsoft.fluent;

import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

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

	protected abstract TResult executeInBackground();

	@Override
	protected final TResult executeInBackground(Void[] params) {
		return executeInBackground();
	}
}
