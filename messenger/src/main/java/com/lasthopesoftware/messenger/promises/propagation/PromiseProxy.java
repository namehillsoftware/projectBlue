package com.lasthopesoftware.messenger.promises.propagation;


import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;

public class PromiseProxy<Resolution> {

	private final Messenger<Resolution> messenger;
	private final CancellationProxy cancellationProxy = new CancellationProxy();

	public PromiseProxy(Messenger<Resolution> messenger) {
		this.messenger = messenger;
		messenger.cancellationRequested(cancellationProxy);
	}

	public void proxy(Promise<Resolution> promise) {
		cancellationProxy.doCancel(promise);

		promise.then(new ResolutionProxy<>(messenger));
		promise.excuse(new RejectionProxy(messenger));
	}
}
