package com.lasthopesoftware.bluewater.client.stored.service.notifications;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

public class SyncChannelProperties implements ChannelConfiguration {

	private static final String channelId = "MusicCanoeSync";

	private static final int channelImportance = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? 2 : NotificationManager.IMPORTANCE_LOW;

	private final Context context;

	private final CreateAndHold<String> lazyChannelName = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() {
			return context.getString(R.string.app_name);
		}
	};

	private final CreateAndHold<String> lazyChannelDescription = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() {
			return String.format("Sync notifications for %1$s", lazyChannelName.getObject());
		}
	};

	public SyncChannelProperties(Context context) {
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
