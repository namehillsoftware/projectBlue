package com.lasthopesoftware.bluewater.client.playback.file.exoplayer;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFileProgress;

import org.joda.time.Period;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class ExoPlayerPositionSource extends Thread implements ObservableOnSubscribe<PlayingFileProgress> {

	private final Collection<ObservableEmitter<PlayingFileProgress>> progressEmitters = new CopyOnWriteArrayList<>();
	private final ExoPlayer exoPlayer;
	private final int periodMilliseconds;

	private boolean isStarted;

	public ExoPlayerPositionSource(ExoPlayer exoPlayer, Period period) {
		this.exoPlayer = exoPlayer;
		this.periodMilliseconds = period.getMillis();
	}

	@Override
	public void subscribe(ObservableEmitter<PlayingFileProgress> e) {
		progressEmitters.add(e);

		e.setDisposable(new Disposable() {
			@Override
			public void dispose() {
				progressEmitters.remove(e);
			}

			@Override
			public boolean isDisposed() {
				return progressEmitters.contains(e);
			}
		});

		if (exoPlayer.getPlayWhenReady()) {
			e.onNext(new PlayingFileProgress(
				exoPlayer.getCurrentPosition(),
				exoPlayer.getDuration()));
		}

		if (!isStarted) start();
		isStarted = true;
	}

	@Override
	public void run() {
		while (!progressEmitters.isEmpty()) {
			try {
				Thread.sleep(periodMilliseconds);
			} catch (InterruptedException e) {
				for (ObservableEmitter<PlayingFileProgress> emitter : progressEmitters)
					emitter.onError(e);
				return;
			}

			try {
				if (!exoPlayer.getPlayWhenReady()) continue;

				final PlayingFileProgress playingFileProgress = new PlayingFileProgress(
					exoPlayer.getCurrentPosition(),
					exoPlayer.getDuration());

				for (ObservableEmitter<PlayingFileProgress> emitter : progressEmitters)
					emitter.onNext(playingFileProgress);
			} catch (Throwable t) {
				for (ObservableEmitter<PlayingFileProgress> emitter : progressEmitters)
					emitter.onError(t);
				return;
			}
		}
	}

	public void cancel() {
		progressEmitters.clear();
	}
}
