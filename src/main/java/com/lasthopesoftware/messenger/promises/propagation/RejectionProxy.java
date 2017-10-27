package com.lasthopesoftware.messenger.promises.propagation;


import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;

public final class RejectionProxy implements ImmediateResponse<Throwable, Void> {
	private final Messenger<?> reject;

	public RejectionProxy(Messenger<?> reject) {
		this.reject = reject;
	}

	@Override
	public Void respond(Throwable throwable) throws Throwable {
		reject.sendRejection(throwable);
		return null;
	}
}
