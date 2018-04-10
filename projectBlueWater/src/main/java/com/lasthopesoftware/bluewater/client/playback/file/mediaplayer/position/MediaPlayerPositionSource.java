package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.position;

import android.media.MediaPlayer;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFileProgress;

import org.joda.time.Period;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class MediaPlayerPositionSource extends Thread {

	private final Object periodSyncObject = new Object();
	private final Object startSyncObject = new Object();
	private final Map<ObservableEmitter<PlayingFileProgress>, Integer> progressEmitters = new ConcurrentHashMap<>();
	private final MediaPlayer mediaPlayer;

	private int minimalObservationPeriod;
	private boolean isStarted;

	public MediaPlayerPositionSource(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	public ObservableOnSubscribe<PlayingFileProgress> observePeriodically(Period observationPeriod) {
		final int observationMilliseconds = observationPeriod.getMillis();
		synchronized (periodSyncObject) {
			minimalObservationPeriod = Math.min(observationMilliseconds, minimalObservationPeriod);
		}

		return e -> {
			progressEmitters.put(e, observationMilliseconds);

			e.setDisposable(new Disposable() {
				@Override
				public void dispose() {
					progressEmitters.remove(e);
					synchronized (periodSyncObject) {
						if (observationMilliseconds > minimalObservationPeriod) return;

						final Optional<Integer> maybeSmallestEmitter =
							Stream.of(progressEmitters.values())
								.sorted()
								.findFirst();

						if (maybeSmallestEmitter.isPresent())
							minimalObservationPeriod = maybeSmallestEmitter.get();
					}
				}

				@Override
				public boolean isDisposed() {
					return !progressEmitters.containsKey(e);
				}
			});

			if (mediaPlayer.isPlaying()) {
				e.onNext(new PlayingFileProgress(
					mediaPlayer.getCurrentPosition(),
					mediaPlayer.getDuration()));
			}

			if (isStarted) return;

			synchronized (startSyncObject) {
				if (!isStarted) start();
				isStarted = true;
			}
		};
	}

	@Override
	public void run() {
		while (!progressEmitters.isEmpty()) {
			try {
				synchronized (periodSyncObject) {
					Thread.sleep(minimalObservationPeriod);
				}
			} catch (InterruptedException e) {
				for (ObservableEmitter<PlayingFileProgress> emitter : progressEmitters.keySet())
					emitter.onError(e);
				return;
			}

			try {
				if (!mediaPlayer.isPlaying()) continue;

				final PlayingFileProgress playingFileProgress = new PlayingFileProgress(
					mediaPlayer.getCurrentPosition(),
					mediaPlayer.getDuration());

				for (ObservableEmitter<PlayingFileProgress> emitter : progressEmitters.keySet())
					emitter.onNext(playingFileProgress);
			} catch (Throwable t) {
				for (ObservableEmitter<PlayingFileProgress> emitter : progressEmitters.keySet())
					emitter.onError(t);
				return;
			}
		}
	}

	public void cancel() {
		progressEmitters.clear();
	}
}
