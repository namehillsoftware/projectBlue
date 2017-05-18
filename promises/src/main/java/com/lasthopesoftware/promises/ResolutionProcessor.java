package com.lasthopesoftware.promises;


public abstract class ResolutionProcessor<InputResolution, NewResolution> extends Messenger<InputResolution, NewResolution> {

	@Override
	protected final void requestResolution(InputResolution inputResolution, Throwable throwable) {
		if (throwable == null)
			processResolution(inputResolution);
		else
			sendRejection(throwable);
	}

	protected abstract void processResolution(InputResolution inputResolution);
}