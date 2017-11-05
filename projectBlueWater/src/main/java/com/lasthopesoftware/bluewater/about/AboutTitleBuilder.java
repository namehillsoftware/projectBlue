package com.lasthopesoftware.bluewater.about;

import android.content.Context;

import com.lasthopesoftware.bluewater.R;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.ILazy;

public class AboutTitleBuilder implements BuildAboutTitle {
	private final ILazy<String> lazyAboutTitle;

	public AboutTitleBuilder(Context context) {
		lazyAboutTitle = new AbstractSynchronousLazy<String>() {
			@Override
			protected String initialize() throws Exception {
				return String.format(
					context.getString(R.string.title_activity_about),
					context.getString(R.string.app_name));
			}
		};
	}

	@Override
	public String buildTitle() {
		return lazyAboutTitle.getObject();
	}
}
