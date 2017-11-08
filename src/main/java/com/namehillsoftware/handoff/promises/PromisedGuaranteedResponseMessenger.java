package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.Message;
import com.namehillsoftware.handoff.RespondingMessenger;
import com.namehillsoftware.handoff.SingleMessageBroadcaster;
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

public class PromisedGuaranteedResponseMessenger<Resolution, Response> extends SingleMessageBroadcaster<Response> implements RespondingMessenger<Resolution> {

	private final PromiseProxy<Response> promiseProxy = new PromiseProxy<Response>(this);
	private final PromisedResponse<Resolution, Response> onFulfilled;
	private final PromisedResponse<Throwable, Response> onRejected;

	public PromisedGuaranteedResponseMessenger(PromisedResponse<Resolution, Response> onFulfilled, PromisedResponse<Throwable, Response> onRejected) {
		this.onFulfilled = onFulfilled;
		this.onRejected = onRejected;
	}

	@Override
	public void respond(Message<Resolution> message) {
		try {
			promiseProxy.proxy(message.rejection == null
				? onFulfilled.promiseResponse(message.resolution)
				: onRejected.promiseResponse(message.rejection));
		} catch (Throwable error) {
			sendRejection(error);
		}
	}
}
