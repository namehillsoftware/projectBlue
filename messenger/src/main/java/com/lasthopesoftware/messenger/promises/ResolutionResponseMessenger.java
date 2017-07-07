package com.lasthopesoftware.messenger.promises;


import com.lasthopesoftware.messenger.Message;
import com.lasthopesoftware.messenger.RespondingMessenger;
import com.lasthopesoftware.messenger.SingleMessageBroadcaster;

abstract class ResolutionResponseMessenger<Resolution, Response> extends SingleMessageBroadcaster<Response> implements RespondingMessenger<Resolution> {
	@Override
	public final void respond(Message<Resolution> message) {
		if (message.rejection == null)
			respond(message.resolution);
		else
			sendRejection(message.rejection);
	}

	abstract void respond(Resolution resolution);
}
