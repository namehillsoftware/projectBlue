package com.lasthopesoftware.messenger.promise;


import com.lasthopesoftware.messenger.AwaitingMessenger;
import com.lasthopesoftware.messenger.Message;
import com.lasthopesoftware.messenger.RespondingMessenger;

abstract class ResolutionResponseMessenger<Resolution, Response> extends AwaitingMessenger<Response> implements RespondingMessenger<Resolution> {
	@Override
	public final void respond(Message<Resolution> message) {
		if (message.rejection == null)
			respond(message.resolution);
		else
			sendRejection(message.rejection);
	}

	abstract void respond(Resolution resolution);
}
