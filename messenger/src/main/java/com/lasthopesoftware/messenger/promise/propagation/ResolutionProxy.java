package com.lasthopesoftware.messenger.promise.propagation;


import com.lasthopesoftware.messenger.Messenger;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

public final class ResolutionProxy<Resolution> implements CarelessOneParameterFunction<Resolution, Void> {
	private final Messenger<Resolution> resolve;

	public ResolutionProxy(Messenger<Resolution> resolve) {
		this.resolve = resolve;
	}

	@Override
	public Void resultFrom(Resolution resolution) throws Throwable {
		resolve.sendResolution(resolution);
		return null;
	}
}
