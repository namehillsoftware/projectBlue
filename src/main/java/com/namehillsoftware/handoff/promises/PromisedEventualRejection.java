package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

public class PromisedEventualRejection<Resolution, Response> extends PromiseResponse<Resolution, Response> {
    private final CancellationProxy cancellationProxy = new CancellationProxy();
    private final PromisedResponse<Throwable, Response> onRejected;
    private final InternalResolutionProxy resolutionProxy = new InternalResolutionProxy();
    private final InternalRejectionProxy rejectionProxy = new InternalRejectionProxy();

    PromisedEventualRejection(PromisedResponse<Throwable, Response> onRejected) {
        this.onRejected = onRejected;
        respondToCancellation(cancellationProxy);
    }

    @Override
    protected void respond(Resolution resolution) {}

    @Override
    protected void respond(Throwable reason) {
        try {
            final Promise<Response> promisedResponse = onRejected.promiseResponse(reason);
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
