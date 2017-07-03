package com.lasthopesoftware.promises;


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
