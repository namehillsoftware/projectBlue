package com.namehillsoftware.handoff.promises;


abstract class RejectionResponsePromise<InputResolution, NewResolution> extends ResponseRoutingPromise<InputResolution, NewResolution> {

	protected final void respond(InputResolution resolution) {}
}
