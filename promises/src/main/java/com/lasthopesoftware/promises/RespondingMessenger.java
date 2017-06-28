package com.lasthopesoftware.promises;


interface RespondingMessenger<Input, Resolution> extends Messenger<Resolution> {
	void requestResponse(Message<Input> message);
}
