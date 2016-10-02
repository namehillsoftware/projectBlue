package com.vedsoft.fluent;

import java.util.concurrent.Executor;

/**
 * Created by david on 10/2/16.
 */

public abstract class FluentRunnable extends FluentCallable<Void> {
	public FluentRunnable() {
		super();
	}

	public FluentRunnable(Executor executor) {
		super(executor);
	}

	protected abstract void runInBackground();

	@Override
	protected Void executeInBackground() {
		runInBackground();
		return null;
	}
}
