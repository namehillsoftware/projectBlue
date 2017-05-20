package com.lasthopesoftware.bluewater.shared.promises;

import com.lasthopesoftware.promises.EmptyMessenger;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.TwoParameterAction;

/**
 * Created by david on 2/12/17.
 */
public class WrappedExecutor<Result> implements Runnable {
	private final TwoParameterAction<IResolvedPromise<Result>, IRejectedPromise> task;
	private final EmptyMessenger<Result> messenger;

	public WrappedExecutor(TwoParameterAction<IResolvedPromise<Result>, IRejectedPromise> task, EmptyMessenger<Result> messenger) {
		this.task = task;
		this.messenger = messenger;
	}

	@Override
	public void run() {
		this.task.runWith(messenger, messenger);
	}
}
