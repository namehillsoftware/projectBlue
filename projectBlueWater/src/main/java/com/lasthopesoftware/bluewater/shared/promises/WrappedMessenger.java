package com.lasthopesoftware.bluewater.shared.promises;


import com.lasthopesoftware.promises.EmptyMessenger;

public class WrappedMessenger implements Runnable {
	private final EmptyMessenger<?> messenger;

	public WrappedMessenger(EmptyMessenger<?> messenger) {
		this.messenger = messenger;
	}

	@Override
	public void run() {
		messenger.requestResolution();
	}
}
