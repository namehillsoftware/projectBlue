package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.MediaPlayerBufferedPromise;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.lazyj.ILazy;
import com.vedsoft.lazyj.Lazy;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by david on 9/20/16.
 */

public class MediaPlayerPlaybackHandler implements IBufferingPlaybackHandler {

	private final MediaPlayer mediaPlayer;
	private final IPromise<IBufferingPlaybackHandler> bufferingPromise;
	private float volume;
	private final ILazy<Observable<Integer>> currentPositionObservable;
	private final IPromise<IPlaybackHandler> playbackPromise;

	public MediaPlayerPlaybackHandler(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
		playbackPromise = new Promise<>(new MediaPlayerPlaybackCompletedTask(this, mediaPlayer));
		bufferingPromise = new Promise<>(new MediaPlayerBufferedPromise(this, mediaPlayer));
		currentPositionObservable = new Lazy<>(() -> Observable.interval(100, TimeUnit.MILLISECONDS).map(i -> mediaPlayer.getCurrentPosition()));
	}

	@Override
	public boolean isPlaying() {
		try {
			return mediaPlayer.isPlaying();
		} catch (IllegalStateException e) {
			return false;
		}
	}

	@Override
	public void pause() {
		mediaPlayer.pause();
	}

	@Override
	public void setVolume(float volume) {
		this.volume = volume;
		mediaPlayer.setVolume(volume, volume);
	}

	@Override
	public float getVolume() {
		return volume;
	}

	@Override
	public int getCurrentPosition() {
		return mediaPlayer.getCurrentPosition();
	}

	@Override
	public Observable<Integer> observeCurrentPosition() {
		return currentPositionObservable.getObject();
	}

	@Override
	public int getDuration() {
		return mediaPlayer.getDuration();
	}

	@Override
	public synchronized IPromise<IPlaybackHandler> promisePlayback() {
		if (!isPlaying())
			mediaPlayer.start();

		return playbackPromise;
	}

	@Override
	public void close() throws IOException {

		playbackPromise.cancel();
		mediaPlayer.release();
	}

	@Override
	public IPromise<IBufferingPlaybackHandler> bufferPlaybackFile() {
		return bufferingPromise;
	}
}