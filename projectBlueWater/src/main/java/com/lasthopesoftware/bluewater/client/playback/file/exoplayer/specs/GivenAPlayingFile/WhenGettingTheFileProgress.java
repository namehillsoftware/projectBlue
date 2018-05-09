package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.specs.GivenAPlayingFile;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheFileProgress {

	private static Duration progress;

	@BeforeClass
	public static void before() throws InterruptedException {
		final ExoPlayer mockMediaPlayer = mock(ExoPlayer.class);
		when(mockMediaPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockMediaPlayer.getCurrentPosition()).thenReturn(75L);
		when(mockMediaPlayer.getDuration()).thenReturn(101L);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final ExoPlayerPlaybackHandler exoPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockMediaPlayer);
		exoPlayerPlaybackHandler
			.promisePlayback()
			.then(p -> {
				final ProgressingPromise<Duration, PlayedFile> returnPromise = p.promisePlayedFile();

				progress = returnPromise.getProgress();

				countDownLatch.countDown();

				return null;
			}, e -> {
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheFileProgressIsCorrect() {
		assertThat(progress).isEqualTo(Duration.millis(75));
	}
}
