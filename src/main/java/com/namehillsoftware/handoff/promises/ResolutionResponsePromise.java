package com.namehillsoftware.handoff.promises;


abstract class ResolutionResponsePromise<Resolution, Response> extends ResponseRoutingPromise<Resolution, Response> {

	@Override
	protected final void respond(Throwable rejection) {
		reject(rejection);
	}
}
