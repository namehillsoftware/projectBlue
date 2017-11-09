package com.namehillsoftware.handoff;


public interface RespondingMessenger<Input> {
	void respond(Message<Input> message);
}
