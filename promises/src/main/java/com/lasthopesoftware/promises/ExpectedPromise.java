package com.lasthopesoftware.promises;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

/**
 * Created by david on 10/17/16.
 */

public class ExpectedPromise<TResult> extends Promise<TResult> {
	ExpectedPromise(@NotNull Callable<TResult> executor) {
		super((resolve, reject) -> {
			try {
				resolve.run(executor.call());
			} catch (Exception e) {
				reject.run(e);
			}
		});
	}
}
