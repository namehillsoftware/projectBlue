package com.lasthopesoftware.promises;


abstract class ResolutionRespondingPromise<Resolution, Response> extends Promise<Response> implements RespondingMessenger<Resolution, Response> {
	@Override
	public final void requestResponse(Message<Resolution> message) {
		if (message.rejection == null)
			requestResponse(message.resolution);
		else
			sendRejection(message.rejection);
	}

	abstract void requestResponse(Resolution resolution);
}
