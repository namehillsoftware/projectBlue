package com.lasthopesoftware.bluewater.shared.promises.extensions;

import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;

public abstract class ProgressingPromise<Progress, Resolution> extends Promise<Resolution> {

	public ProgressingPromise(MessengerOperator<Resolution> messengerOperator) {
		super(messengerOperator);
	}

	protected ProgressingPromise() {}

	public abstract Progress getProgress();
}
