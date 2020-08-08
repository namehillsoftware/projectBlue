package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.PromisedResponse;

class PromisedEventualRejection<Resolution, Response> extends EventualResponse<Resolution, Response> {
    private final PromisedResponse<Throwable, Response> onRejected;

    PromisedEventualRejection(PromisedResponse<Throwable, Response> onRejected) {
        this.onRejected = onRejected;
    }

    @Override
    protected void respond(Resolution resolution) {}

    @Override
    protected void respond(Throwable reason) throws Throwable {
        proxy(onRejected.promiseResponse(reason));
    }
}
