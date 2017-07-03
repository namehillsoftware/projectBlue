package com.lasthopesoftware.promises;


abstract class RejectionResponseMessenger<InputResolution, NewResolution> extends AwaitingMessenger<NewResolution> implements RespondingMessenger<InputResolution> {
	@Override
	public final void respond(Message<InputResolution> inputResolution) {
		if (inputResolution.rejection != null)
			respond(inputResolution.rejection);
	}

	protected abstract void respond(Throwable throwable);
}
