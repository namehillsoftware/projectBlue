package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.specs.GivenAPlayingMediaPlayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.MediaPlayerPlaybackHandler;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenPlaybackCompletes {

	private static boolean isCompleted;

	private static MediaPlayer.OnCompletionListener onCompletionListener;

	@BeforeClass
	public static void context() throws InterruptedException {
		final MediaPlayer mockMediaPlayer = mock(MediaPlayer.class);
		when(mockMediaPlayer.isPlaying()).thenReturn(true);
		when(mockMediaPlayer.getCurrentPosition()).thenReturn(50);
		when(mockMediaPlayer.getDuration()).thenReturn(100);
		doAnswer((Answer<Void>) invocation -> {
			onCompletionListener = invocation.getArgument(0);
			return null;
		}).when(mockMediaPlayer).setOnCompletionListener(any());

		final MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler = new MediaPlayerPlaybackHandler(mockMediaPlayer);
		mediaPlayerPlaybackHandler.promisePlayback();
		final Observable<Duration> firstObservable =
			mediaPlayerPlaybackHandler.observeProgress(Duration.millis(500));

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		firstObservable
			.subscribe(
				p -> {},
				e -> {},
				() -> {
					isCompleted = true;
					countDownLatch.countDown();
				});

		onCompletionListener.onCompletion(mockMediaPlayer);

		countDownLatch.await(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenCompletedPlaybackIsObserved() {
		assertThat(isCompleted).isTrue();
	}
}
