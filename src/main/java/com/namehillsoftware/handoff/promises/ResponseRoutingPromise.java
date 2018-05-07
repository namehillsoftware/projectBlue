package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.Message;

abstract class ResponseRoutingPromise<Resolution, Response> extends Promise<Response> implements com.namehillsoftware.handoff.RespondingMessenger<Resolution> {

	@Override
	public final void respond(Message<Resolution> message) {
		try {
			if (message.rejection == null)
				respond(message.resolution);
			else
				respond(message.rejection);
		} catch (Throwable error) {
			reject(error);
		}
	}

	protected abstract void respond(Resolution resolution) throws Throwable;

	protected abstract void respond(Throwable rejection) throws Throwable;
}
