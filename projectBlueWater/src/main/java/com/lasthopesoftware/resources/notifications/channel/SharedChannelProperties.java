package com.lasthopesoftware.resources.notifications.channel;

import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

public class SharedChannelProperties implements ChannelConfiguration {

	private static final CreateAndHold<SharedChannelProperties> lazyInstance = new Lazy<>(SharedChannelProperties::new);

	private SharedChannelProperties() {}

	public String getChannelId() {
		return "MusicCanoe";
	}

	public String getChannelName() {
		return null;
	}

	@Override
	public String getChannelDescription() {
		return null;
	}

	@Override
	public int getChannelImportance() {
		return 0;
	}

	public static SharedChannelProperties getInstance() {
		return lazyInstance.getObject();
	}
}
