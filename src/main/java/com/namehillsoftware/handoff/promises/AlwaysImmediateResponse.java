package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.ImmediateAction;

final class AlwaysImmediateResponse<Resolution> extends PromiseResponse<Resolution, Resolution> {

    private ImmediateAction response;

    AlwaysImmediateResponse(ImmediateAction response) {
        this.response = response;
    }

    @Override
    protected void respond(Resolution resolution) {
        response.act();
        resolve(resolution);
    }

    @Override
    protected void respond(Throwable reason) {
        response.act();
        reject(reason);
    }
}
