package com.namehillsoftware.handoff.promises.propagation;


import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

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
