package com.namehillsoftware.handoff.promises.response;

public interface ImmediateResponse<Resolution, Response> {
	Response respond(Resolution resolution) throws Throwable;
}
