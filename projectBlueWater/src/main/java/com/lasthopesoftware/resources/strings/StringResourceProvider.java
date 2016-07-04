package com.lasthopesoftware.resources.strings;

import android.content.Context;
import android.support.annotation.StringRes;

/**
 * Created by david on 7/3/16.
 */
public class StringResourceProvider implements IStringResourceProvider {
	private final Context context;

	public StringResourceProvider(Context context) {
		this.context = context;
	}

	@Override
	public String getString(@StringRes int stringResourceId) {
		return context.getString(stringResourceId);
	}
}
