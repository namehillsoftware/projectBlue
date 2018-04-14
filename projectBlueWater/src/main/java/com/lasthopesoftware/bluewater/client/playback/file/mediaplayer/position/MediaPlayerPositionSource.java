package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.position;

import android.media.MediaPlayer;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFileProgress;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerIllegalStateReporter;

import org.joda.time.Duration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class MediaPlayerPositionSource implements Runnable {

	private static final MediaPlayerIllegalStateReporter mediaPlayerIllegalStateReporter = new MediaPlayerIllegalStateReporter(MediaPlayerPositionSource.class);

	private final Object periodSyncObject = new Object();
	private final Object startSyncObject = new Object();
	private final Map<ObservableEmitter<PlayingFileProgress>, Long> progressEmitters = new ConcurrentHashMap<>();
	private final MediaPlayer mediaPlayer;

	private Long minimalObservationPeriod;
	private boolean isStarted;
	private volatile boolean isCancelled;
	private Thread broadcastThread;
	private int lastPosition;
	private int lastDuration;

	public MediaPlayerPositionSource(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	public ObservableOnSubscribe<PlayingFileProgress> observePeriodically(Duration observationPeriod) {
		final long observationMilliseconds = observationPeriod.getMillis();
		synchronized (periodSyncObject) {
			minimalObservationPeriod = Math.max(minimalObservationPeriod != null
				? Math.min(observationMilliseconds, minimalObservationPeriod)
				: observationMilliseconds, 100);
		}

		return e -> {
			progressEmitters.put(e, observationMilliseconds);

			e.setDisposable(new Disposable() {
				@Override
				public void dispose() {
					progressEmitters.remove(e);
					synchronized (periodSyncObject) {
						if (observationMilliseconds > minimalObservationPeriod) return;

						final Optional<Long> maybeSmallestEmitter =
							Stream.of(progressEmitters.values())
								.sorted()
								.findFirst();

						minimalObservationPeriod = maybeSmallestEmitter.isPresent() ? maybeSmallestEmitter.get() : null;
					}
				}

				@Override
				public boolean isDisposed() {
					return !progressEmitters.containsKey(e);
				}
			});

			if (mediaPlayer.isPlaying())
				e.onNext(produceFilePlayingProgress());

			if (isStarted) return;

			synchronized (startSyncObject) {
				if (isStarted) return;

				broadcastThread = new Thread(this);
				broadcastThread.start();

				isStarted = true;
			}
		};
	}

	@Override
	public void run() {
		while (!isCancelled) {
			if (progressEmitters.isEmpty() || minimalObservationPeriod == null) {
				setStopped();
				return;
			}

			try {
				synchronized (periodSyncObject) {
					Thread.sleep(minimalObservationPeriod);
				}
			} catch (InterruptedException e) {
				for (ObservableEmitter<PlayingFileProgress> emitter : progressEmitters.keySet())
					emitter.onError(e);
				setStopped();
				return;
			}

			try {
				if (!mediaPlayer.isPlaying()) continue;

				final PlayingFileProgress playingFileProgress = produceFilePlayingProgress();

				for (ObservableEmitter<PlayingFileProgress> emitter : progressEmitters.keySet())
					emitter.onNext(playingFileProgress);
			} catch (Throwable t) {
				for (ObservableEmitter<PlayingFileProgress> emitter : progressEmitters.keySet())
					emitter.onError(t);
				setStopped();
				return;
			}
		}
	}

	private PlayingFileProgress produceFilePlayingProgress() {
		try {
			return new PlayingFileProgress(
				lastPosition = mediaPlayer.getCurrentPosition(),
				lastDuration = mediaPlayer.getDuration());
		} catch (IllegalStateException e) {
			mediaPlayerIllegalStateReporter.reportIllegalStateException(e, "Getting current position");

			return new PlayingFileProgress(
				lastPosition,
				lastDuration);
		}
	}

	private void setStopped() {
		synchronized (startSyncObject) {
			isStarted = false;
		}
	}

	public void cancel() {
		isCancelled = true;
		progressEmitters.clear();
	}
}
