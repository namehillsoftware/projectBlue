package com.lasthopesoftware.bluewater.shared.DispatchedPromise;

import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.Function;
import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.concurrent.Executor;

/**
 * Created by david on 12/18/16.
 */

public class DispatchedPromise<TResult> extends Promise<TResult> {
	public DispatchedPromise(Function<TResult> task) {
		super(new DispatchedAndroidTask<>(task));
	}

	public DispatchedPromise(Function<TResult> task, Executor executor) {
		super(new DispatchedAndroidTask<>(task, executor));
	}

	public DispatchedPromise(OneParameterFunction<OneParameterAction<Runnable>, TResult> task) {
		super(new DispatchedAndroidTask<>(task));
	}

	public DispatchedPromise(OneParameterFunction<OneParameterAction<Runnable>, TResult> task, Executor executor) {
		super(new DispatchedAndroidTask<>(task, executor));
	}
}
