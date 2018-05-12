package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.specs.GivenAPlayingMediaPlayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.MediaPlayerPlaybackHandler;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public class WhenPlaybackCompletes {

	private static PlayedFile playedFile;

	@BeforeClass
	public static void context() throws InterruptedException {
		final MediaPlayer mediaPlayer = mock(MediaPlayer.class);
		final MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler = new MediaPlayerPlaybackHandler(mediaPlayer);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		mediaPlayerPlaybackHandler
			.promisePlayback()
			.eventually(PlayingFile::promisePlayedFile)
			.then(p -> {
				playedFile = p;
				countDownLatch.countDown();
				return null;
			});

		mediaPlayerPlaybackHandler.onCompletion(mediaPlayer);

		countDownLatch.await();
	}

	@Test
	public void thenThePlayedFileIsReturned() {
		assertThat(playedFile).isNotNull();
	}
}
