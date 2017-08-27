package com.lasthopesoftware.messenger.promises.response;

import com.lasthopesoftware.messenger.promises.Promise;

public interface PromisedResponse<Resolution, Response> {
	Promise<Response> promiseResponse(Resolution resolution) throws Throwable;
}
