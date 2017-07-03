package com.lasthopesoftware.messenger;


public interface RespondingMessenger<Input> {
	void respond(Message<Input> message);
}
