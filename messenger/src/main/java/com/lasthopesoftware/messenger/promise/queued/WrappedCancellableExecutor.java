package com.lasthopesoftware.messenger.promise.queued;

import com.lasthopesoftware.messenger.Messenger;
import com.vedsoft.futures.runnables.OneParameterAction;

class WrappedCancellableExecutor<Result> implements Runnable {
	private final Messenger<Result> messenger;
	private final OneParameterAction<Messenger<Result>> task;

	WrappedCancellableExecutor(Messenger<Result> messenger, OneParameterAction<Messenger<Result>> task) {
		this.messenger = messenger;
		this.task = task;
	}

	@Override
	public void run() {
		task.runWith(messenger);
	}
}
