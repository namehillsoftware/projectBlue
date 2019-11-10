package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.promises.response.ImmediateAction;

final class ImmediateActionResponse<Resolution> extends PromiseResponse<Resolution, Resolution> {

    private ImmediateAction response;

    ImmediateActionResponse(ImmediateAction response) {
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
