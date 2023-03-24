package com.lasthopesoftware.bluewater.settings.hidden;

import android.content.Intent;
import com.lasthopesoftware.resources.intents.IIntentFactory;

public class HiddenSettingsActivityIntentBuilder {

	private final IIntentFactory intentFactory;

	public HiddenSettingsActivityIntentBuilder(IIntentFactory intentFactory) {
		this.intentFactory = intentFactory;
	}

	public Intent buildHiddenSettingsIntent() {
		return intentFactory.getIntent(HiddenSettingsActivity.class);
	}
}
