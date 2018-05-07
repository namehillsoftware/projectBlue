package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.Message;
import com.namehillsoftware.handoff.RespondingMessenger;
import com.namehillsoftware.handoff.SingleMessageBroadcaster;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

abstract class ResponseRoutingMessenger<Resolution, Response> extends SingleMessageBroadcaster<Response> implements com.namehillsoftware.handoff.RespondingMessenger<Resolution> {

	@Override
	public final void respond(Message<Resolution> message) {
		try {
			if (message.rejection == null)
				respond(message.resolution);
			else
				respond(message.rejection);
		} catch (Throwable error) {
			sendRejection(error);
		}
	}

	protected abstract void respond(Resolution resolution) throws Throwable;

	protected abstract void respond(Throwable rejection) throws Throwable;
}
