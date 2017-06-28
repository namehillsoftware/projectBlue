package com.lasthopesoftware.promises;


abstract class ErrorRespondingPromise<InputResolution, NewResolution> extends Promise<NewResolution> implements RespondingMessenger<InputResolution, NewResolution> {
	@Override
	public final void requestResponse(Message<InputResolution> inputResolution) {
		if (inputResolution.rejection != null)
			requestResolution(inputResolution.rejection);
	}

	protected abstract void requestResolution(Throwable throwable);
}
