package com.lasthopesoftware.bluewater.client.playback.file.progress;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.joda.time.Duration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class PollingProgressSource implements Runnable {

	private final Object periodSyncObject = new Object();
	private final Object startSyncObject = new Object();
	private final Map<ObservableEmitter<FileProgress>, Long> progressEmitters = new ConcurrentHashMap<>();
	private final ReadFileProgress fileProgressReader;

	private Long minimalObservationPeriod;
	private boolean isStarted;
	private volatile boolean isCancelled;
	private Thread broadcastThread;

	public PollingProgressSource(ReadFileProgress fileProgressReader) {
		this.fileProgressReader = fileProgressReader;
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

			e.onNext(fileProgressReader.getFileProgress());

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
				final FileProgress fileProgress = fileProgressReader.getFileProgress();

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
