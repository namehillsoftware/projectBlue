package com.lasthopesoftware.promises.cancellable;

import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.jetbrains.annotations.NotNull;

/**
 * Created by david on 10/28/16.
 */

public final class CancellablePromise<TResult> extends DependentCancellablePromise<Void, TResult> {
	public CancellablePromise(@NotNull ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> executor) {
		super((result, exception, resolve, reject, onCancelled) -> {
			executor.runWith(resolve, reject, onCancelled);
		});

		provide(null, null);
	}
}
