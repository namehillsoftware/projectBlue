package com.lasthopesoftware.bluewater.client.playback.file.progress;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import org.joda.time.Duration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.schedulers.ComputationScheduler;
import io.reactivex.internal.schedulers.RxThreadFactory;

public class PollingProgressSource<Error extends Exception> implements Runnable {

	private static final CreateAndHold<Scheduler> lazyScheduler = new AbstractSynchronousLazy<Scheduler>() {
		@Override
		protected Scheduler create() {
			return new ComputationScheduler(
				new RxThreadFactory(
					"File Progress Thread",
					Thread.MIN_PRIORITY,
					true));
		}
	};

	private final Object periodSync = new Object();
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
		notifyPlaybackError.playbackError(this::emitError);
	}

	public ObservableOnSubscribe<Duration> observePeriodically(Duration observationPeriod) {
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

						observationPeriodMilliseconds = Math.max(maybeSmallestEmitter.orElse(minimalObservationPeriod), minimalObservationPeriod);
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

				lazyScheduler
					.getObject()
					.scheduleDirect(this, observationPeriodMilliseconds, TimeUnit.MILLISECONDS);

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

		lazyScheduler
			.getObject()
			.scheduleDirect(this, observationPeriodMilliseconds, TimeUnit.MILLISECONDS);

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
		for (ObservableEmitter<Duration> emitter : progressEmitters.keySet()) {
			if (!emitter.isDisposed())
				emitter.onComplete();
		}

		close();
	}

	public void close() {
		progressEmitters.clear();
	}
}
