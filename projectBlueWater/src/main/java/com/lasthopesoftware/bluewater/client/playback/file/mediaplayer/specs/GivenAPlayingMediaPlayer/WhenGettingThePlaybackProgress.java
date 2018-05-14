package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.specs.GivenAPlayingMediaPlayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.MediaPlayerPlaybackHandler;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingThePlaybackProgress {

	private static Duration progress;
	private static Duration duration;

	@BeforeClass
	public static void before() throws InterruptedException {
		final MediaPlayer mockMediaPlayer = mock(MediaPlayer.class);
		when(mockMediaPlayer.isPlaying()).thenReturn(true);
		when(mockMediaPlayer.getCurrentPosition()).thenReturn(50);
		when(mockMediaPlayer.getDuration()).thenReturn(100);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler = new MediaPlayerPlaybackHandler(mockMediaPlayer);
		mediaPlayerPlaybackHandler
			.promisePlayback()
			.then(p -> {
				progress = p.promisePlayedFile().getProgress();
				duration = p.getDuration();
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenThePlaybackPositionIsCorrect() {
		assertThat(progress).isEqualTo(Duration.millis(50));
	}

	@Test
	public void thenTheDurationIsCorrect() {
		assertThat(duration).isEqualTo(Duration.millis(100));
	}
}
