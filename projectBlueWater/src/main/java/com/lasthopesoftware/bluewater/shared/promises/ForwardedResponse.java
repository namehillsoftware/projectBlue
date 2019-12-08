package com.lasthopesoftware.bluewater.shared.promises;

import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

public class ForwardedResponse<Resolution extends Response, Response> implements ImmediateResponse<Resolution, Response> {

	private static final CreateAndHold<ForwardedResponse<?, ?>> singlePassThrough = new Lazy<>(ForwardedResponse::new);

	@SuppressWarnings("unchecked")
	public static <Resolution extends Response, Response> ForwardedResponse<Resolution, Response> forward() {
		return (ForwardedResponse<Resolution, Response>) singlePassThrough.getObject();
	}

	private ForwardedResponse() {}

	@Override
	public Response respond(Resolution resolution) {
		return resolution;
	}
}
