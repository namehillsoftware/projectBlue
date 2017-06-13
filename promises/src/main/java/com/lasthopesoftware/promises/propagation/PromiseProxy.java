package com.lasthopesoftware.promises.propagation;


import com.lasthopesoftware.promises.Messenger;
import com.lasthopesoftware.promises.Promise;

public class PromiseProxy<Resolution> {

	private final Messenger<Resolution> messenger;

	public PromiseProxy(Messenger<Resolution> messenger) {
		this.messenger = messenger;
	}

	public void proxy(Promise<Resolution> promise) {
		messenger.cancellationRequested(new CancellationProxy(promise));

		promise.next(new ResolutionProxy<>(messenger));
		promise.error(new RejectionProxy(messenger));
	}
}
