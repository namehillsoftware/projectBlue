package com.lasthopesoftware.promises;

public abstract class EmptyMessenger<Resolution> extends AwaitingMessenger<Void, Resolution> {

	@Override
	protected final void requestResponse(Void empty, Throwable ignored) {
		requestResolution();
	}

	public abstract void requestResolution();
}
