package com.lasthopesoftware.bluewater.shared.promises;

import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

public class ForwardedResponse<T> implements ImmediateResponse<T, T> {

	private static final CreateAndHold<ForwardedResponse<?>> singlePassThrough = new Lazy<>(ForwardedResponse::new);

	@SuppressWarnings("unchecked")
	public static <T> ForwardedResponse<T> forward() {
		return (ForwardedResponse<T>) singlePassThrough.getObject();
	}

	private ForwardedResponse() {}

	@Override
	public T respond(T t) {
		return t;
	}
}
