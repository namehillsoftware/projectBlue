package com.lasthopesoftware.bluewater.shared.promises;

import com.lasthopesoftware.promises.EmptyMessenger;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

public class WrappedCancellableExecutor<Result> implements Runnable {
	private final ThreeParameterAction<IResolvedPromise<Result>, IRejectedPromise, OneParameterAction<Runnable>> task;
	private final EmptyMessenger<Result> messenger;

	public WrappedCancellableExecutor(ThreeParameterAction<IResolvedPromise<Result>, IRejectedPromise, OneParameterAction<Runnable>> task, EmptyMessenger<Result> messenger) {
		this.task = task;
		this.messenger = messenger;
	}

	@Override
	public void run() {
		this.task.runWith(messenger, messenger, messenger);
	}
}
