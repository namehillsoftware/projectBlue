package com.lasthopesoftware.bluewater.client.playback.file.exoplayer;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;

import org.joda.time.Duration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class ExoPlayerPositionSource extends Thread {

	private final Object periodSyncObject = new Object();
	private final Object startSyncObject = new Object();
	private final Map<ObservableEmitter<FileProgress>, Long> progressEmitters = new ConcurrentHashMap<>();
	private final ExoPlayer exoPlayer;

	private Long minimalObservationPeriod;
	private boolean isStarted;
	private volatile boolean isCancelled;

	public ExoPlayerPositionSource(ExoPlayer exoPlayer) {
		this.exoPlayer = exoPlayer;
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

			if (exoPlayer.getPlayWhenReady()) {
				e.onNext(new FileProgress(
					exoPlayer.getCurrentPosition(),
					exoPlayer.getDuration()));
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
		while (!isCancelled) {
			if (progressEmitters.isEmpty() || minimalObservationPeriod == null) {
				try {
					Thread.sleep(100);
					continue;
				} catch (InterruptedException e) {
					return;
				}
			}
			try {
				synchronized (periodSyncObject) {
					Thread.sleep(minimalObservationPeriod);
				}
			} catch (InterruptedException e) {
				for (ObservableEmitter<FileProgress> emitter : progressEmitters.keySet())
					emitter.onError(e);
				return;
			}

			try {
				if (!exoPlayer.getPlayWhenReady()) continue;

				final FileProgress fileProgress = new FileProgress(
					exoPlayer.getCurrentPosition(),
					exoPlayer.getDuration());

				for (ObservableEmitter<FileProgress> emitter : progressEmitters.keySet())
					emitter.onNext(fileProgress);
			} catch (Throwable t) {
				for (ObservableEmitter<FileProgress> emitter : progressEmitters.keySet())
					emitter.onError(t);
				return;
			}
		}
	}

	public void cancel() {
		isCancelled = true;
		progressEmitters.clear();
	}
}
