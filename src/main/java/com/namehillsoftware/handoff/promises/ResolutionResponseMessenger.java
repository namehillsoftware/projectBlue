package com.namehillsoftware.handoff.promises;


abstract class ResolutionResponseMessenger<Resolution, Response> extends ResponseRoutingMessenger<Resolution, Response> {

	@Override
	protected final void respond(Throwable rejection) {
		sendRejection(rejection);
	}
}
