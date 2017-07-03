package com.lasthopesoftware.promises;


interface RespondingMessenger<Input> {
	void respond(Message<Input> message);
}
