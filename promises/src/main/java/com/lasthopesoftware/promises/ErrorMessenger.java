package com.lasthopesoftware.promises;


public abstract class ErrorMessenger<InputResolution, NewResolution> extends Messenger<InputResolution, NewResolution> {
	@Override
	protected final void requestResolution(InputResolution inputResolution, Throwable throwable) {
		if (throwable != null)
			requestResolution(throwable);
	}

	protected abstract void requestResolution(Throwable throwable);
}
