package com.lasthopesoftware.resources.notifications.channel;

import android.app.NotificationManager;
import android.content.Context;

import com.lasthopesoftware.bluewater.R;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

public class SharedChannelProperties implements ChannelConfiguration {

	private static final String channelId = "MusicCanoe";

	private static final int channelImportance = NotificationManager.IMPORTANCE_DEFAULT;

	private final Context context;

	private final CreateAndHold<String> lazyChannelName = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() throws Throwable {
			return context.getString(R.string.app_name);
		}
	};

	private final CreateAndHold<String> lazyChannelDescription = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() throws Throwable {
			return String.format("Notifications for %1$s", lazyChannelName.getObject());
		}
	};

	public SharedChannelProperties(Context context) {
		this.context = context;
	}

	public String getChannelId() {
		return channelId;
	}

	public String getChannelName() {
		return lazyChannelName.getObject();
	}

	@Override
	public String getChannelDescription() {
		return lazyChannelDescription.getObject();
	}

	@Override
	public int getChannelImportance() {
		return channelImportance;
	}
}
