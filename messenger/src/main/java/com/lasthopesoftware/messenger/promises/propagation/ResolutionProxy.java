package com.lasthopesoftware.messenger.promises.propagation;


import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;

public final class ResolutionProxy<Resolution> implements ImmediateResponse<Resolution, Void> {
	private final Messenger<Resolution> resolve;

	public ResolutionProxy(Messenger<Resolution> resolve) {
		this.resolve = resolve;
	}

	@Override
	public Void respond(Resolution resolution) throws Throwable {
		resolve.sendResolution(resolution);
		return null;
	}
}
