package com.namehillsoftware.handoff.promises;


abstract class RejectionResponseMessenger<InputResolution, NewResolution> extends ResponseRoutingMessenger<InputResolution, NewResolution> {

	protected final void respond(InputResolution resolution) {}
}
