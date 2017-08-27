package com.lasthopesoftware.messenger.promises;

import com.lasthopesoftware.messenger.Messenger;

public interface MessengerOperator<Resolution> {
	void send(Messenger<Resolution> messenger);
}
