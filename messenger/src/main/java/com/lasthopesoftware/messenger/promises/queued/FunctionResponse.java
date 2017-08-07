package com.lasthopesoftware.messenger.promises.queued;

import com.lasthopesoftware.messenger.Messenger;
import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

public final class FunctionResponse<Result> implements OneParameterAction<Messenger<Result>> {

	private final CarelessFunction<Result> callable;

	public FunctionResponse(CarelessFunction<Result> callable) {
		this.callable = callable;
	}

	@Override
	public void runWith(Messenger<Result> messenger) {
		try {
			messenger.sendResolution(callable.result());
		} catch (Throwable rejection) {
			messenger.sendRejection(rejection);
		}
	}
}
