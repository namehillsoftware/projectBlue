package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.specs.GivenAPlayingMediaPlayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error.MediaPlayerErrorException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenAnErrorOccurs {
	private static MediaPlayer.OnErrorListener onErrorListener;
	private static MediaPlayerErrorException mediaPlayerErrorException;

	@BeforeClass
	public static void context() throws InterruptedException {
		final MediaPlayer mockMediaPlayer = mock(MediaPlayer.class);
		when(mockMediaPlayer.isPlaying()).thenReturn(true);
		when(mockMediaPlayer.getCurrentPosition()).thenReturn(50);
		when(mockMediaPlayer.getDuration()).thenReturn(100);

		doAnswer((Answer<Void>) invocation -> {
			onErrorListener = invocation.getArgument(0);
			return null;
		}).when(mockMediaPlayer).setOnErrorListener(any());

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler = new MediaPlayerPlaybackHandler(mockMediaPlayer);
		mediaPlayerPlaybackHandler.promisePlayback()
			.eventually(PlayingFile::promisePlayedFile)
			.then(
				p -> null,
				e -> {
					if (e instanceof MediaPlayerErrorException) {
						mediaPlayerErrorException = (MediaPlayerErrorException)e;
					}

					return null;
				})
			.then(v -> {
				countDownLatch.countDown();
				return null;
			});

		onErrorListener.onError(
			mockMediaPlayer,
			4,
			3);

		countDownLatch.await(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenTheCorrectErrorIsObserved() {
		assertThat(mediaPlayerErrorException.what).isEqualTo(4);
	}

	@Test
	public void thenTheCorrectErrorExtraIsObserved() {
		assertThat(mediaPlayerErrorException.extra).isEqualTo(3);
	}
}
