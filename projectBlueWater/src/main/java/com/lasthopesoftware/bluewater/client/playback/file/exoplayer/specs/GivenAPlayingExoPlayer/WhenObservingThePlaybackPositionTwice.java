package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.specs.GivenAPlayingExoPlayer;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenObservingThePlaybackPositionTwice {

	private static List<FileProgress> collectedProgresses = new ArrayList<>();

	@BeforeClass
	public static void before() throws InterruptedException {
		final ExoPlayer mockExoPlayer = mock(ExoPlayer.class);
		when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
		when(mockExoPlayer.getDuration()).thenReturn(100L);

		final ExoPlayerPlaybackHandler mediaPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockExoPlayer);
		final ConnectableObservable<FileProgress> firstObservable =
			mediaPlayerPlaybackHandler.observeProgress(Duration.millis(500))
				.publish();

		final CountDownLatch countDownLatch = new CountDownLatch(2);
		final Observable<FileProgress> secondObservable = mediaPlayerPlaybackHandler.observeProgress(Duration.ZERO);
		secondObservable.take(3)
			.subscribe(e -> {}, e -> {}, () -> countDownLatch.countDown());

		final Disposable disposable = firstObservable.connect();

		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				disposable.dispose();
				countDownLatch.countDown();
			}
		}, 2600);

		firstObservable.subscribe(collectedProgresses::add);

		countDownLatch.await();
	}

	@Test
	public void thenTheCorrectNumberOfPlaylistProgressesAreCollected() {
		assertThat(collectedProgresses.size()).isEqualTo(5);
	}
}
