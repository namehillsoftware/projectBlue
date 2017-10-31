package com.lasthopesoftware.messenger.promises;

import com.lasthopesoftware.messenger.Message;
import com.lasthopesoftware.messenger.RespondingMessenger;
import com.lasthopesoftware.messenger.SingleMessageBroadcaster;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;

public class GuaranteedResponseMessenger<Resolution, Response> extends SingleMessageBroadcaster<Response> implements RespondingMessenger<Resolution> {

	private final ImmediateResponse<Resolution, Response> onFulfilled;
	private final ImmediateResponse<Throwable, Response> onRejected;

	public GuaranteedResponseMessenger(ImmediateResponse<Resolution, Response> onFulfilled, ImmediateResponse<Throwable, Response> onRejected) {
		this.onFulfilled = onFulfilled;
		this.onRejected = onRejected;
	}

	@Override
	public void respond(Message<Resolution> message) {
		try {
			sendResolution(message.rejection == null
				? onFulfilled.respond(message.resolution)
				: onRejected.respond(message.rejection));
		} catch (Throwable error) {
			sendRejection(error);
		}
	}
}
