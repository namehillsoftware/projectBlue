package com.lasthopesoftware.messenger.promises.response;

public interface ImmediateResponse<Resolution, Response> {
	Response respond(Resolution resolution) throws Throwable;
}
