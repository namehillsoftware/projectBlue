package com.lasthopesoftware.resources.notifications;

import android.app.Notification;

import androidx.core.app.NotificationCompat;

public class FakeNotificationCompatBuilder extends NotificationCompat.Builder {
	private final Notification returnNotification;

	public static NotificationCompat.Builder newFakeBuilder(Notification returnNotification) {
		return new FakeNotificationCompatBuilder(returnNotification);
	}

	@SuppressWarnings("ConstantConditions")
	public FakeNotificationCompatBuilder(Notification returnNotification) {
		super(null, null);

		this.returnNotification = returnNotification;
	}

	@Override
	public Notification build() {
		return returnNotification;
	}
}
