package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.progress;

import android.media.MediaPlayer;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerIllegalStateReporter;
import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;

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
	private final Map<ObservableEmitter<FileProgress>, Long> progressEmitters = new ConcurrentHashMap<>();
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

	public ObservableOnSubscribe<FileProgress> observePeriodically(Duration observationPeriod) {
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
				stopThread();
				return;
			}

			try {
				synchronized (periodSyncObject) {
					Thread.sleep(minimalObservationPeriod);
				}
			} catch (InterruptedException e) {
				for (ObservableEmitter<FileProgress> emitter : progressEmitters.keySet())
					emitter.onError(e);
				stopThread();
				return;
			}

			try {
				if (!mediaPlayer.isPlaying()) continue;

				final FileProgress fileProgress = produceFilePlayingProgress();

				for (ObservableEmitter<FileProgress> emitter : progressEmitters.keySet())
					emitter.onNext(fileProgress);
			} catch (Throwable t) {
				for (ObservableEmitter<FileProgress> emitter : progressEmitters.keySet())
					emitter.onError(t);
				stopThread();
				return;
			}
		}
	}

	private FileProgress produceFilePlayingProgress() {
		try {
			return new FileProgress(
				lastPosition = mediaPlayer.getCurrentPosition(),
				lastDuration = mediaPlayer.getDuration());
		} catch (IllegalStateException e) {
			mediaPlayerIllegalStateReporter.reportIllegalStateException(e, "Getting current position");

			return new FileProgress(
				lastPosition,
				lastDuration);
		}
	}

	private void stopThread() {
		synchronized (startSyncObject) {
			isStarted = false;
		}
	}

	public void cancel() {
		isCancelled = true;
		progressEmitters.clear();
	}
}
