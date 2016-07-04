package com.lasthopesoftware.resources.intents;

import android.content.Intent;

/**
 * Created by david on 7/3/16.
 */
public interface IIntentFactory {
	public Intent getIntent(Class cls);
}
