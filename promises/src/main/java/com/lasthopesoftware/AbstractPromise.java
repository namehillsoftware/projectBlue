package com.lasthopesoftware;

public abstract class AbstractPromise<TResult> extends UnresolvedPromise<Void, TResult> {
	@Override
	protected final void execute(Void result) {
		execute();
	}

	protected abstract void execute();
}
