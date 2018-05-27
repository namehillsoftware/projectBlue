package com.lasthopesoftware.bluewater.client.playback.service.notification.building;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.namehillsoftware.handoff.promises.Promise;

public class PlaybackStartingNotificationBuilder {

	private final Context context;
	private final SetupMediaStyleNotifications mediaStyleNotificationsSetup;

	public PlaybackStartingNotificationBuilder(Context context, SetupMediaStyleNotifications mediaStyleNotificationsSetup) {
		this.context = context;
		this.mediaStyleNotificationsSetup = mediaStyleNotificationsSetup;
	}

	public Promise<NotificationCompat.Builder> promisePreparedPlaybackStartingNotification() {
		final NotificationCompat.Builder builder = mediaStyleNotificationsSetup.getMediaStyleNotification();
		builder
			.setOngoing(true)
			.setColor(ContextCompat.getColor(context, R.color.clearstream_dark))
			.setContentIntent(buildNowPlayingActivityIntent())
			.setShowWhen(false)
			.setSmallIcon(R.drawable.clearstream_logo_dark)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setContentTitle(context.getString(R.string.app_name))
			.setContentText(context.getString(R.string.lbl_starting_playback));

		return new Promise<>(builder);
	}

	private PendingIntent buildNowPlayingActivityIntent() {
		// Set the notification area
		final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(context, 0, viewIntent, 0);
	}
}
