package com.namehillsoftware.handoff.promises.propagation;


import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

public final class ResolutionProxy<Resolution> implements ImmediateResponse<Resolution, Void> {
	private final Messenger<Resolution> resolve;

	public ResolutionProxy(Messenger<Resolution> resolve) {
		this.resolve = resolve;
	}

	@Override
	public Void respond(Resolution resolution) {
		resolve.sendResolution(resolution);
		return null;
	}
}
