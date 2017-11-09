package com.namehillsoftware.handoff.promises.propagation;


import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;

public class PromiseProxy<Resolution> {

	private final Messenger<Resolution> messenger;
	private final CancellationProxy cancellationProxy = new CancellationProxy();

	public PromiseProxy(Messenger<Resolution> messenger) {
		this.messenger = messenger;
		messenger.cancellationRequested(cancellationProxy);
	}

	public void proxy(Promise<Resolution> promise) {
		cancellationProxy.doCancel(promise);

		promise.then(new ResolutionProxy<Resolution>(messenger));
		promise.excuse(new RejectionProxy(messenger));
	}
}
