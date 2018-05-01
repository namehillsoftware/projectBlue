package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.specs.GivenAPlayingExoPlayer;

import com.annimon.stream.Stream;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
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

	private static List<Player.EventListener> eventListeners = new ArrayList<>();

	@BeforeClass
	public static void context() throws InterruptedException {
		final ExoPlayer mockExoPlayer = mock(ExoPlayer.class);
		when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
		when(mockExoPlayer.getDuration()).thenReturn(100L);
		doAnswer((Answer<Void>) invocation -> {
			eventListeners.add(invocation.getArgument(0));
			return null;
		}).when(mockExoPlayer).addListener(any());

		final ExoPlayerPlaybackHandler mediaPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockExoPlayer);
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

		Stream.of(eventListeners).forEach(e -> e.onPlayerStateChanged(false, Player.STATE_ENDED));

		countDownLatch.await(2, TimeUnit.SECONDS);
	}

	@Test
	public void thenCompletedPlaybackIsObserved() {
		assertThat(isCompleted).isTrue();
	}
}
