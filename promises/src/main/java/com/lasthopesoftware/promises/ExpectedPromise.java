package com.lasthopesoftware.promises;

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
