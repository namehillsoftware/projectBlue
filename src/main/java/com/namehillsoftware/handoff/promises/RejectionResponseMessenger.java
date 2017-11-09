package com.namehillsoftware.handoff.promises;


import com.namehillsoftware.handoff.Message;
import com.namehillsoftware.handoff.RespondingMessenger;
import com.namehillsoftware.handoff.SingleMessageBroadcaster;

abstract class RejectionResponseMessenger<InputResolution, NewResolution> extends SingleMessageBroadcaster<NewResolution> implements RespondingMessenger<InputResolution> {
	@Override
	public final void respond(Message<InputResolution> inputResolution) {
		if (inputResolution.rejection != null)
			respond(inputResolution.rejection);
	}

	protected abstract void respond(Throwable throwable);
}
