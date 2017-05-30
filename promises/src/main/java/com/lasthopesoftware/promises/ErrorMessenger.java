package com.lasthopesoftware.promises;


abstract class ErrorMessenger<InputResolution, NewResolution> extends AwaitingMessenger<InputResolution, NewResolution> {
	@Override
	protected final void requestResolution(InputResolution inputResolution, Throwable throwable) {
		if (throwable != null)
			requestResolution(throwable);
	}

	protected abstract void requestResolution(Throwable throwable);
}
