package com.namehillsoftware.handoff.promises.response;

public class VoidResponse<Resolution> implements ImmediateResponse<Resolution, Void> {

	private final ResponseAction<Resolution> responseAction;

	public VoidResponse(ResponseAction<Resolution> responseAction) {
		this.responseAction = responseAction;
	}

	@Override
	public Void respond(Resolution resolution) throws Throwable {
		responseAction.perform(resolution);
		return null;
	}
}
