package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.Messenger;

public interface MessengerOperator<Resolution> {
	void send(Messenger<Resolution> messenger);
}
