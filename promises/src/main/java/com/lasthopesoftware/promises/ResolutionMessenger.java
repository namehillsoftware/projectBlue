package com.lasthopesoftware.promises;


public abstract class ResolutionMessenger<InputResolution, NewResolution> extends AwaitingMessenger<InputResolution, NewResolution> {

	@Override
	protected final void requestResolution(InputResolution inputResolution, Throwable throwable) {
		if (throwable == null)
			requestResolution(inputResolution);
		else
			sendRejection(throwable);
	}

	protected abstract void requestResolution(InputResolution inputResolution);
}