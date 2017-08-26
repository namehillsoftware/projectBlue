package com.lasthopesoftware.messenger.promises;

import com.lasthopesoftware.messenger.Messenger;

public interface MessengerTask<Resolution> {
	void execute(Messenger<Resolution> messenger);
}
