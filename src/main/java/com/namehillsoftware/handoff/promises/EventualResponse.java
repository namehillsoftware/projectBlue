package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

abstract class EventualResponse<Resolution, Response> extends PromiseResponse<Resolution, Response> {
    private final CancellationProxy cancellationProxy = new CancellationProxy();
    private final InternalRejectionProxy rejectionProxy = new InternalRejectionProxy();
    private final InternalResolutionProxy resolutionProxy = new InternalResolutionProxy();

    EventualResponse() {
        respondToCancellation(cancellationProxy);
    }

    protected final void proxy(Promise<Response> promisedResponse) {
        try {
            cancellationProxy.doCancel(promisedResponse);

            promisedResponse.then(resolutionProxy, rejectionProxy);
        } catch (Throwable throwable) {
            reject(throwable);
        }
    }

    private final class InternalResolutionProxy implements ImmediateResponse<Response, Void> {

        @Override
        public Void respond(Response resolution) {
            resolve(resolution);
            return null;
        }
    }

    private final class InternalRejectionProxy implements ImmediateResponse<Throwable, Void> {

        @Override
        public Void respond(Throwable throwable) {
            reject(throwable);
            return null;
        }
    }
}
