package com.namehillsoftware.handoff.promises;


import com.namehillsoftware.handoff.Message;
import com.namehillsoftware.handoff.RespondingMessenger;
import com.namehillsoftware.handoff.SingleMessageBroadcaster;

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
