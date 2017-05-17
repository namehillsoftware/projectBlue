package com.lasthopesoftware.promises;

/**
 * Created by david on 4/2/17.
 */

public abstract class EmptyMessenger<Resolution> extends Messenger<Void, Resolution> {

	@Override
	protected final void requestResolution(Void empty) {
		requestResolution();
	}

	public abstract void requestResolution();
}
