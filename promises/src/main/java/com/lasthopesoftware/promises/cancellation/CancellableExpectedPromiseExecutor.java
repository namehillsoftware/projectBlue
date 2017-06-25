package com.lasthopesoftware.promises.cancellation;


import com.lasthopesoftware.promises.EmptyMessenger;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

class CancellableExpectedPromiseExecutor<Resolution> extends EmptyMessenger<Resolution> {
	private final CancellationToken cancellationToken = new CancellationToken();
	private final CarelessOneParameterFunction<CancellationToken, Resolution> task;

	CancellableExpectedPromiseExecutor(CarelessOneParameterFunction<CancellationToken, Resolution> task) {
		this.task = task;
		cancellationRequested(cancellationToken);
	}

	@Override
	public void requestResolution() {
		try {
			task.resultFrom(cancellationToken);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
	}
}
