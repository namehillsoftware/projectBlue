package com.lasthopesoftware.promises;


public abstract class ErrorProcessor<InputResolution, NewResolution> extends Messenger<InputResolution, NewResolution> {
	@Override
	protected final void requestResolution(InputResolution inputResolution, Throwable throwable) {
		if (throwable != null)
			processError(throwable);
	}

	protected abstract void processError(Throwable throwable);
}
