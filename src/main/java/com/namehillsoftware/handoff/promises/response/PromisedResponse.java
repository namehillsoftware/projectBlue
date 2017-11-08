package com.namehillsoftware.handoff.promises.response;

import com.namehillsoftware.handoff.promises.Promise;

public interface PromisedResponse<Resolution, Response> {
	Promise<Response> promiseResponse(Resolution resolution) throws Throwable;
}
