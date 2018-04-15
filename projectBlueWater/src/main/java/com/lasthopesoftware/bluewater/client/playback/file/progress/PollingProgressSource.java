package com.lasthopesoftware.bluewater.client.playback.file.progress;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.joda.time.Duration;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class PollingProgressSource extends Thread {

	private static final Executor notificationExecutor = Executors.newSingleThreadExecutor();

	private final ReentrantLock periodSync = new ReentrantLock();
	private final Object startSyncObject = new Object();
	private final ReentrantLock emittersLock = new ReentrantLock();
	private final Condition emittersNotEmpty = emittersLock.newCondition();

	private final Map<ObservableEmitter<FileProgress>, Long> progressEmitters = new ConcurrentHashMap<>();
	private final ReadFileProgress fileProgressReader;
	private final long minimalObservationPeriod;
	private long observationPeriodMilliseconds;
	private boolean isStarted;
	private boolean isCancelled;

	public PollingProgressSource(ReadFileProgress fileProgressReader, Duration minimalObservationPeriod) {
		this.fileProgressReader = fileProgressReader;
		this.minimalObservationPeriod = minimalObservationPeriod.getMillis();
	}

	public ObservableOnSubscribe<FileProgress> observePeriodically(Duration observationPeriod) {
		final long observationMilliseconds = observationPeriod.getMillis();
		periodSync.lock();
		try {
			observationPeriodMilliseconds = Math.max(
				Math.min(observationMilliseconds, observationPeriodMilliseconds),
				minimalObservationPeriod);
		} finally {
			periodSync.unlock();
		}

		return e -> {
			emittersLock.lock();
			try {
				progressEmitters.put(e, observationMilliseconds);
				emittersNotEmpty.signal();
			} finally {
				emittersLock.unlock();
			}

			e.setDisposable(new Disposable() {
				@Override
				public void dispose() {
					progressEmitters.remove(e);

					periodSync.lock();
					try {
						if (observationMilliseconds > observationPeriodMilliseconds) return;

						final Optional<Long> maybeSmallestEmitter =
							Stream.of(progressEmitters.values())
								.sorted()
								.findFirst();

						observationPeriodMilliseconds = Math.max(maybeSmallestEmitter.orElse(minimalObservationPeriod), minimalObservationPeriod);
					} finally {
						periodSync.unlock();
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

	@Override
	public void run() {
		while (!isCancelled) {
			try {
				Thread.sleep(observationPeriodMilliseconds);
			} catch (InterruptedException e) {
				notificationExecutor.execute(new ErrorEmitter(e, progressEmitters.keySet()));

				return;
			}

			if (progressEmitters.isEmpty()) {
				emittersLock.lock();
				try {
					emittersNotEmpty.await();
				} catch (InterruptedException e) {
					notificationExecutor.execute(new ErrorEmitter(e, progressEmitters.keySet()));

					return;
				} finally {
					emittersLock.unlock();
				}
			}

			try {
				notificationExecutor.execute(
					new ProgressEmitter(
						fileProgressReader.getFileProgress(),
						progressEmitters.keySet()));
			} catch (Throwable t) {
				notificationExecutor.execute(new ErrorEmitter(t, progressEmitters.keySet()));

				return;
			}
		}
	}

	public void cancel() {
		isCancelled = true;
		progressEmitters.clear();

		emittersLock.lock();
		try {
			emittersNotEmpty.signal();
		} finally {
			emittersLock.unlock();
		}
	}

	private static class ProgressEmitter implements Runnable {

		private final FileProgress fileProgress;
		private final Set<ObservableEmitter<FileProgress>> emitters;

		ProgressEmitter(FileProgress fileProgress, Set<ObservableEmitter<FileProgress>> emitters) {
			this.fileProgress = fileProgress;
			this.emitters = emitters;
		}

		@Override
		public void run() {
			for (ObservableEmitter<FileProgress> emitter : emitters)
				emitter.onNext(fileProgress);
		}
	}

	private static class ErrorEmitter implements Runnable {

		private final Throwable error;
		private final Set<ObservableEmitter<FileProgress>> emitters;

		ErrorEmitter(Throwable error, Set<ObservableEmitter<FileProgress>> emitters) {
			this.error = error;
			this.emitters = emitters;
		}

		@Override
		public void run() {
			for (ObservableEmitter<FileProgress> emitter : emitters)
				emitter.onError(error);
		}
	}
}
