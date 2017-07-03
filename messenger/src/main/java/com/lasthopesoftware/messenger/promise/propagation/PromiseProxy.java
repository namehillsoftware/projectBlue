package com.lasthopesoftware.messenger.promise.propagation;


import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promise.Promise;

public class PromiseProxy<Resolution> {

	private final Messenger<Resolution> messenger;
	private final CancellationProxy cancellationProxy = new CancellationProxy();

	public PromiseProxy(Messenger<Resolution> messenger) {
		this.messenger = messenger;
		messenger.cancellationRequested(cancellationProxy);
	}

	public void proxy(Promise<Resolution> promise) {
		cancellationProxy.doCancel(promise);

		promise.next(new ResolutionProxy<>(messenger));
		promise.error(new RejectionProxy(messenger));
	}
}
