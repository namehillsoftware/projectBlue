package com.lasthopesoftware.bluewater.client.playback.file.progress;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import org.joda.time.Duration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.schedulers.ComputationScheduler;

public class PollingProgressSource<Error extends Exception> implements Runnable {

	private final CreateAndHold<Scheduler> scheduler = new AbstractSynchronousLazy<Scheduler>() {
		@Override
		protected Scheduler create() {
			final Scheduler scheduler = new ComputationScheduler();
			scheduler.start();
			return scheduler;
		}
	};
	private final ReentrantLock periodSync = new ReentrantLock();
	private final Object startSyncObject = new Object();

	private final Map<ObservableEmitter<Duration>, Long> progressEmitters = new ConcurrentHashMap<>();
	private final ReadFileProgress fileProgressReader;
	private final long minimalObservationPeriod;

	private long observationPeriodMilliseconds;
	private boolean isStarted;

	public PollingProgressSource(
		ReadFileProgress fileProgressReader,
		NotifyFilePlaybackComplete notifyFilePlaybackComplete,
		NotifyFilePlaybackError<Error> notifyPlaybackError,
		Duration minimalObservationPeriod) {

		this.fileProgressReader = fileProgressReader;
		this.minimalObservationPeriod = minimalObservationPeriod.getMillis();
		notifyFilePlaybackComplete.playbackCompleted(this::whenPlaybackCompleted);
		notifyPlaybackError.playbackError(this::onPlaybackError);
	}

	public ObservableOnSubscribe<Duration> observePeriodically(Duration observationPeriod) {
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
			progressEmitters.put(e, observationMilliseconds);

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

			synchronized (startSyncObject) {
				if (isStarted) return;

				scheduler.getObject().scheduleDirect(this, observationPeriodMilliseconds, TimeUnit.MILLISECONDS);

				isStarted = true;
			}
		};
	}

	@Override
	public void run() {
		if (progressEmitters.isEmpty()) {
			synchronized (startSyncObject) {
				if (progressEmitters.isEmpty()) {
					isStarted = false;
					return;
				}
			}
		}

		scheduler.getObject().scheduleDirect(this, observationPeriodMilliseconds, TimeUnit.MILLISECONDS);

		try {
			emitProgress(fileProgressReader.getFileProgress());
		} catch (Throwable t) {
			emitError(t);
		}
	}

	private void emitProgress(Duration fileProgress) {
		for (ObservableEmitter<Duration> emitter : progressEmitters.keySet()) {
			if (!emitter.isDisposed())
				emitter.onNext(fileProgress);
		}
	}

	private void emitError(Throwable error) {
		for (ObservableEmitter emitter : progressEmitters.keySet()) {
			if (!emitter.isDisposed())
				emitter.onError(error);
		}
	}

	private void whenPlaybackCompleted() {
		for (ObservableEmitter<Duration> emitter : progressEmitters.keySet())
			emitter.onComplete();

		close();
	}

	private void onPlaybackError(Error error) {
		for (ObservableEmitter<Duration> emitter : progressEmitters.keySet())
			emitter.onError(error);
	}

	public void close() {
		if (scheduler.isCreated())
			scheduler.getObject().shutdown();
	}
}
