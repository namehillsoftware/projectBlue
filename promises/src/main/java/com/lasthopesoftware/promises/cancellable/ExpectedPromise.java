package com.lasthopesoftware.promises.cancellable;

import com.lasthopesoftware.promises.Promise;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

/**
 * Created by david on 10/17/16.
 */

public class ExpectedPromise<TResult> extends Promise<TResult> {
	public ExpectedPromise(@NotNull Callable<TResult> executor) {
		super(new InternalExpectedPromiseExecutor<>(executor));
	}

}
