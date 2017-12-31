package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.IRemoteBroadcaster;
import com.lasthopesoftware.resources.intents.IIntentFactory;
import com.lasthopesoftware.resources.notifications.SupplyNotificationBuilders;
import com.namehillsoftware.handoff.promises.Promise;


public class NotificationBroadcaster implements IRemoteBroadcaster {

	private final Service service;
	private final NotificationManager notificationManager;
	private final SupplyNotificationBuilders supplyNotificationBuilders;
	private final PlaybackNotificationsConfiguration playbackNotificationsConfiguration;
	private final IIntentFactory intentFactory;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;

	private volatile boolean isPlaying;

	public NotificationBroadcaster(Service service, CachedFilePropertiesProvider cachedFilePropertiesProvider, NotificationManager notificationManager, SupplyNotificationBuilders supplyNotificationBuilders, PlaybackNotificationsConfiguration playbackNotificationsConfiguration, IIntentFactory intentFactory) {
		this.service = service;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		this.notificationManager = notificationManager;
		this.supplyNotificationBuilders = supplyNotificationBuilders;
		this.playbackNotificationsConfiguration = playbackNotificationsConfiguration;
		this.intentFactory = intentFactory;
	}

	@Override
	public void setPlaying() {
		isPlaying = true;
	}

	@Override
	public void setPaused() {

	}

	@Override
	public void setStopped() {

	}

	@Override
	public void updateNowPlaying(ServiceFile serviceFile) {
		promiseBuiltNowPlayingNotification(serviceFile)
			.then(notificationBuilder -> {
				service.startForeground(playbackNotificationsConfiguration.getNotificationId(), addNotificationAccoutrements(notificationBuilder).build());
				return null;
			});
	}

	@Override
	public void updateTrackPosition(long trackPosition) {

	}

	private Promise<NotificationCompat.Builder> promiseBuiltNowPlayingNotification(ServiceFile serviceFile) {
		return cachedFilePropertiesProvider.promiseFileProperties(serviceFile.getKey())
			.then(fileProperties -> {
				final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
				final String name = fileProperties.get(FilePropertiesProvider.NAME);

				return supplyNotificationBuilders
					.getNotificationBuilder()
					.setOngoing(isPlaying)
					.setContentTitle(String.format(service.getString(R.string.title_svc_now_playing), service.getText(R.string.app_name)))
					.setContentText(artist + " - " + name)
					.setContentIntent(buildNowPlayingActivityIntent())
					.setShowWhen(false);
//					.setDeleteIntent(PendingIntent.getService(this, 0, getNewSelfIntent(this, PlaybackService.Action.killMusicService), PendingIntent.FLAG_UPDATE_CURRENT))
//					.addAction(new NotificationCompat.Action(
//						R.drawable.av_rewind,
//						service.getString(R.string.btn_previous),
//						PendingIntent.getService(this, 0, getNewSelfIntent(this, PlaybackService.Action.previous), PendingIntent.FLAG_UPDATE_CURRENT)))
//					.addAction(isPlaying
//						? new NotificationCompat.Action(
//						R.drawable.av_pause,
//						service.getString(R.string.btn_pause),
//						PendingIntent.getService(this, 0, getNewSelfIntent(this, PlaybackService.Action.pause), PendingIntent.FLAG_UPDATE_CURRENT))
//						: new NotificationCompat.Action(
//						R.drawable.av_play,
//						service.getString(R.string.btn_play),
//						PendingIntent.getService(this, 0, getNewSelfIntent(this, PlaybackService.Action.play), PendingIntent.FLAG_UPDATE_CURRENT)))
//					.addAction(new NotificationCompat.Action(
//						R.drawable.av_fast_forward,
//						service.getString(R.string.btn_next),
//						PendingIntent.getService(this, 0, getNewSelfIntent(this, PlaybackService.Action.next), PendingIntent.FLAG_UPDATE_CURRENT)));
			});
	}

	private PendingIntent buildNowPlayingActivityIntent() {
		// Set the notification area
		final Intent viewIntent = intentFactory.getIntent(NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(service, 0, viewIntent, 0);
	}

	private static NotificationCompat.Builder addNotificationAccoutrements(NotificationCompat.Builder notificationBuilder) {
		return notificationBuilder
			.setSmallIcon(R.drawable.clearstream_logo_dark)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
	}
}
