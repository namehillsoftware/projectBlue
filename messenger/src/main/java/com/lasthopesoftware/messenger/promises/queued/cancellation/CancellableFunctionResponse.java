package com.lasthopesoftware.messenger.promises.queued.cancellation;

import com.lasthopesoftware.messenger.Messenger;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

public final class CancellableFunctionResponse<Result> implements OneParameterAction<Messenger<Result>> {
	private final CarelessOneParameterFunction<CancellationToken, Result> task;

	public CancellableFunctionResponse(CarelessOneParameterFunction<CancellationToken, Result> task) {
		this.task = task;
	}

	@Override
	public void runWith(Messenger<Result> messenger) {
		final CancellationToken cancellationToken = new CancellationToken();
		messenger.cancellationRequested(cancellationToken);

		try {
			messenger.sendResolution(task.resultFrom(cancellationToken));
		} catch (Throwable throwable) {
			messenger.sendRejection(throwable);
		}
	}
}
