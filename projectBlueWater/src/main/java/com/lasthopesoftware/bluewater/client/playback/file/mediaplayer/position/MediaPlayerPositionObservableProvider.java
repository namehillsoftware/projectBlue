package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.position;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.PlayingFileProgress;

import org.joda.time.Period;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

public class MediaPlayerPositionObservableProvider {

	private final MediaPlayer mediaPlayer;
	private MediaPlayerObservablePeriod mediaPlayerObservablePeriod;

	public MediaPlayerPositionObservableProvider(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	public synchronized Observable<PlayingFileProgress> observePlayingFileProgress(Period period) {
		if (mediaPlayerObservablePeriod != null) {
			if (period.getMillis() > mediaPlayerObservablePeriod.observedPeriod.getMillis())
				return Observable.create(mediaPlayerObservablePeriod.mediaPlayerPositionSource).sample(period.getMillis(), TimeUnit.MILLISECONDS);

			if (period.getMillis() == mediaPlayerObservablePeriod.observedPeriod.getMillis())
				return Observable.create(mediaPlayerObservablePeriod.mediaPlayerPositionSource);

			mediaPlayerObservablePeriod = null;
		}

		mediaPlayerObservablePeriod = new MediaPlayerObservablePeriod(
			period,
			new MediaPlayerPositionSource(mediaPlayer, period));

		return Observable
			.create(mediaPlayerObservablePeriod.mediaPlayerPositionSource)
			.doOnDispose(() -> mediaPlayerObservablePeriod = null);
	}

	private static class MediaPlayerObservablePeriod {
		final Period observedPeriod;
		final MediaPlayerPositionSource mediaPlayerPositionSource;

		private MediaPlayerObservablePeriod(Period observedPeriod, MediaPlayerPositionSource mediaPlayerPositionSource) {
			this.observedPeriod = observedPeriod;
			this.mediaPlayerPositionSource = mediaPlayerPositionSource;
		}
	}
}
