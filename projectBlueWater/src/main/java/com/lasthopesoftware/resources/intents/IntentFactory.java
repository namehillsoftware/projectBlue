package com.lasthopesoftware.resources.intents;

import android.content.Context;
import android.content.Intent;

public class IntentFactory implements IIntentFactory {

	private final Context context;

	public IntentFactory(Context context) {
		this.context = context;
	}

	@Override
	public Intent getIntent(Class cls) {
		return new Intent(context, cls);
	}
}
