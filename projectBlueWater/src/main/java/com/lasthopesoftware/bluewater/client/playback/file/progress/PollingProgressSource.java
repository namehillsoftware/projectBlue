package com.lasthopesoftware.bluewater.client.playback.file.progress;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.joda.time.Duration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class PollingProgressSource extends Thread {

	private static final long minimalObservationPeriod = 100L;

	private final Object periodSync = new Object();
	private final Object startSyncObject = new Object();
	private final Map<ObservableEmitter<FileProgress>, Long> progressEmitters = new ConcurrentHashMap<>();
	private final ReadFileProgress fileProgressReader;

	private long observationPeriodMilliseconds = minimalObservationPeriod;
	private boolean isStarted;
	private volatile boolean isCancelled;

	public PollingProgressSource(ReadFileProgress fileProgressReader) {
		this.fileProgressReader = fileProgressReader;
	}

	public ObservableOnSubscribe<FileProgress> observePeriodically(Duration observationPeriod) {
		final long observationMilliseconds = observationPeriod.getMillis();
		synchronized (periodSync) {
			observationPeriodMilliseconds = Math.max(
				Math.min(observationMilliseconds, observationPeriodMilliseconds),
				minimalObservationPeriod);
		}

		return e -> {
			progressEmitters.put(e, observationMilliseconds);

			e.setDisposable(new Disposable() {
				@Override
				public void dispose() {
					progressEmitters.remove(e);
					synchronized (periodSync) {
					if (observationMilliseconds > observationPeriodMilliseconds) return;

						final Optional<Long> maybeSmallestEmitter =
							Stream.of(progressEmitters.values())
								.sorted()
								.findFirst();

						observationPeriodMilliseconds = maybeSmallestEmitter.orElse(minimalObservationPeriod);
					}
				}

				@Override
				public boolean isDisposed() {
					return !progressEmitters.containsKey(e);
				}
			});

			e.onNext(fileProgressReader.getFileProgress());

			if (isStarted) return;

			synchronized (startSyncObject) {
				if (isStarted) return;

				start();

				isStarted = true;
			}
		};
	}

	private synchronized void updateObservationPeriod(long newObservationPeriodMilliseconds) {
		observationPeriodMilliseconds = Math.max(
			Math.min(newObservationPeriodMilliseconds, observationPeriodMilliseconds),
			minimalObservationPeriod);
	}

	@Override
	public void run() {
		while (!isCancelled) {
			try {
				Thread.sleep(observationPeriodMilliseconds);
			} catch (InterruptedException e) {
				for (ObservableEmitter<FileProgress> emitter : progressEmitters.keySet())
					emitter.onError(e);

				return;
			}

			if (progressEmitters.isEmpty()) continue;

			try {
				final FileProgress fileProgress = fileProgressReader.getFileProgress();

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
