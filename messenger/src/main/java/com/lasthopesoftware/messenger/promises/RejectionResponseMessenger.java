package com.lasthopesoftware.messenger.promises;


import com.lasthopesoftware.messenger.Message;
import com.lasthopesoftware.messenger.RespondingMessenger;
import com.lasthopesoftware.messenger.SingleMessageBroadcaster;

abstract class RejectionResponseMessenger<InputResolution, NewResolution> extends SingleMessageBroadcaster<NewResolution> implements RespondingMessenger<InputResolution> {
	@Override
	public final void respond(Message<InputResolution> inputResolution) {
		if (inputResolution.rejection != null)
			respond(inputResolution.rejection);
	}

	protected abstract void respond(Throwable throwable);
}
