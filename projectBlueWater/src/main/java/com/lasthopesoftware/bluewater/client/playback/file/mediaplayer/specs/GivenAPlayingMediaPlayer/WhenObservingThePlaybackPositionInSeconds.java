package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.specs.GivenAPlayingMediaPlayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenObservingThePlaybackPositionInSeconds {

	private static List<FileProgress> collectedProgresses = new ArrayList<>();

	@BeforeClass
	public static void before() throws InterruptedException {
		final MediaPlayer mockMediaPlayer = mock(MediaPlayer.class);
		when(mockMediaPlayer.isPlaying()).thenReturn(true);
		when(mockMediaPlayer.getCurrentPosition()).thenReturn(50, 50, 50, 50, 50);
		when(mockMediaPlayer.getDuration())
			.thenReturn(100);


		final MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler = new MediaPlayerPlaybackHandler(mockMediaPlayer);
		final ConnectableObservable<FileProgress> progressObservable = mediaPlayerPlaybackHandler
			.observeProgress(Duration.standardSeconds(1))
			.publish();

		final Disposable disposable = progressObservable.connect();
		final CountDownLatch countDownLatch = new CountDownLatch(1);

		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				disposable.dispose();
				countDownLatch.countDown();
			}
		}, 2600);

		progressObservable
			.subscribe(collectedProgresses::add);

		countDownLatch.await();
	}

	@Test
	public void thenTheCorrectNumberOfPlaylistProgressesAreCollected() {
		assertThat(collectedProgresses.size()).isEqualTo(2);
	}
}
