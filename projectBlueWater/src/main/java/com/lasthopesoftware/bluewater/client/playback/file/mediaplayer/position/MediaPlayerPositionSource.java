package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.position;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.PlayingFileProgress;

import org.joda.time.Period;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class MediaPlayerPositionSource extends Thread implements ObservableOnSubscribe<PlayingFileProgress> {

	private final Collection<ObservableEmitter<PlayingFileProgress>> progressEmitters = new CopyOnWriteArrayList<>();
	private final MediaPlayer mediaPlayer;
	private final int periodMilliseconds;

	private boolean isStarted;

	public MediaPlayerPositionSource(MediaPlayer mediaPlayer, Period period) {
		this.mediaPlayer = mediaPlayer;
		this.periodMilliseconds = period.getMillis();
	}

	@Override
	public synchronized void subscribe(ObservableEmitter<PlayingFileProgress> e) {
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

		if (mediaPlayer.isPlaying()) {
			e.onNext(new PlayingFileProgress(
				mediaPlayer.getCurrentPosition(),
				mediaPlayer.getDuration()));
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
				if (!mediaPlayer.isPlaying()) continue;

				final PlayingFileProgress playingFileProgress = new PlayingFileProgress(
					mediaPlayer.getCurrentPosition(),
					mediaPlayer.getDuration());

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
