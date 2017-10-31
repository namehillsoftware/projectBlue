package com.lasthopesoftware.messenger.promises;

import com.lasthopesoftware.messenger.Message;
import com.lasthopesoftware.messenger.RespondingMessenger;
import com.lasthopesoftware.messenger.SingleMessageBroadcaster;
import com.lasthopesoftware.messenger.promises.propagation.PromiseProxy;
import com.lasthopesoftware.messenger.promises.response.PromisedResponse;

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
