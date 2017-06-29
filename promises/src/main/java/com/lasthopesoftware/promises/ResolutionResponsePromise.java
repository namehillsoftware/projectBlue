package com.lasthopesoftware.promises;


abstract class ResolutionResponsePromise<Resolution, Response> extends Promise<Response> implements RespondingMessenger<Resolution> {
	@Override
	public final void respond(Message<Resolution> message) {
		if (message.rejection == null)
			respond(message.resolution);
		else
			sendRejection(message.rejection);
	}

	abstract void respond(Resolution resolution);
}
