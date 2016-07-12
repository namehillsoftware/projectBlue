package com.lasthopesoftware.bluewater.client.settings;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.lasthopesoftware.resources.intents.IIntentFactory;
import com.lasthopesoftware.resources.intents.IntentFactory;

/**
 * Created by david on 7/10/16.
 */
public class EditClientSettingsActivityIntentBuilder implements IEditClientSettingsActivityIntentBuilder {
	private IIntentFactory intentFactory;

	public EditClientSettingsActivityIntentBuilder(@NonNull Context context) {
		this(new IntentFactory(context));
	}

	public EditClientSettingsActivityIntentBuilder(@NonNull IIntentFactory intentFactory) {
		this.intentFactory = intentFactory;
	}

	@Override
	public Intent buildIntent(int libraryId) {
		final Intent returnIntent = intentFactory.getIntent(EditClientSettingsActivity.class);
		returnIntent.putExtra(EditClientSettingsActivity.serverIdExtra, libraryId);

		return returnIntent;
	}
}
