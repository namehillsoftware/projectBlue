package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.specs.GivenAPlayingMediaPlayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.PlayingFileProgress;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.MediaPlayerPlaybackHandler;

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

	private static List<PlayingFileProgress> collectedProgresses = new ArrayList<>();

	@BeforeClass
	public static void before() throws InterruptedException {
		final MediaPlayer mockMediaPlayer = mock(MediaPlayer.class);
		when(mockMediaPlayer.isPlaying()).thenReturn(true);
		when(mockMediaPlayer.getCurrentPosition()).thenReturn(50, 50, 50, 50, 50);
		when(mockMediaPlayer.getDuration())
			.thenReturn(100);

		final MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler = new MediaPlayerPlaybackHandler(mockMediaPlayer);
		final ConnectableObservable<PlayingFileProgress> firstObservable =
			mediaPlayerPlaybackHandler.observeProgress(Duration.millis(500))
				.publish();

		final CountDownLatch countDownLatch = new CountDownLatch(2);
		final Observable<PlayingFileProgress> secondObservable = mediaPlayerPlaybackHandler.observeProgress(Duration.ZERO);
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
