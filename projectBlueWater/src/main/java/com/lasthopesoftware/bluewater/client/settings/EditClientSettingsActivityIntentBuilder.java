package com.lasthopesoftware.bluewater.client.settings;

import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.resources.intents.IIntentFactory;
import com.lasthopesoftware.resources.intents.IntentFactory;

/**
 * Created by david on 7/10/16.
 */
public class EditClientSettingsActivityIntentBuilder implements IEditClientSettingsActivityIntentBuilder {
	private IIntentFactory intentFactory;

	public EditClientSettingsActivityIntentBuilder(Context context) {
		this(new IntentFactory(context));
	}

	public EditClientSettingsActivityIntentBuilder(IIntentFactory intentFactory) {
		this.intentFactory = intentFactory;
	}

	@Override
	public Intent buildIntent(int libraryId) {
		final Intent settingsIntent = intentFactory.getIntent(EditClientSettingsActivity.class);
		settingsIntent.putExtra(EditClientSettingsActivity.serverIdExtra, libraryId);

		return settingsIntent;
	}
}
