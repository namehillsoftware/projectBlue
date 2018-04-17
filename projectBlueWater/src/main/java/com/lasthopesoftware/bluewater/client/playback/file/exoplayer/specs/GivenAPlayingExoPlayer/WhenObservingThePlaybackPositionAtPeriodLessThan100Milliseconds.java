package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.specs.GivenAPlayingExoPlayer;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;

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

public class WhenObservingThePlaybackPositionAtPeriodLessThan100Milliseconds {

	private static List<Duration> collectedProgresses = new ArrayList<>();

	@BeforeClass
	public static void before() throws InterruptedException {
		final ExoPlayer mockExoPlayer = mock(ExoPlayer.class);
		when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
		when(mockExoPlayer.getDuration()).thenReturn(100L);

		final ExoPlayerPlaybackHandler exoPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockExoPlayer);
		exoPlayerPlaybackHandler.promisePlayback();
		final ConnectableObservable<Duration> progressObservable = exoPlayerPlaybackHandler
			.observeProgress(Duration.millis(5))
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
		}, 2550);

		progressObservable
			.subscribe(collectedProgresses::add);

		countDownLatch.await();
	}

	@Test
	public void thenThePlaylistProgressesAreStillCollectedAtEvery100Milliseconds() {
		assertThat(collectedProgresses.size()).isBetween(25, 26);
	}
}
