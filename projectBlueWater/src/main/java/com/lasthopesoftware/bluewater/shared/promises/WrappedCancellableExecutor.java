package com.lasthopesoftware.bluewater.shared.promises;

import com.lasthopesoftware.promises.Messenger;
import com.vedsoft.futures.runnables.OneParameterAction;

public class WrappedCancellableExecutor<Result> implements Runnable {
	private final Messenger<Result> messenger;
	private final OneParameterAction<Messenger<Result>> task;

	public WrappedCancellableExecutor(Messenger<Result> messenger, OneParameterAction<Messenger<Result>> task) {
		this.messenger = messenger;
		this.task = task;
	}

	@Override
	public void run() {
		task.runWith(messenger);
	}
}
