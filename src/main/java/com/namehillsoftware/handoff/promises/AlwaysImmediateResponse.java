package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.AlwaysResponse;

final class AlwaysImmediateResponse<Resolution> extends PromiseResponse<Resolution, Resolution> {

    private AlwaysResponse response;

    AlwaysImmediateResponse(AlwaysResponse response) {
        this.response = response;
    }

    @Override
    protected void respond(Resolution resolution) {
        response.respond();
        resolve(resolution);
    }

    @Override
    protected void respond(Throwable reason) {
        response.respond();
        reject(reason);
    }
}
