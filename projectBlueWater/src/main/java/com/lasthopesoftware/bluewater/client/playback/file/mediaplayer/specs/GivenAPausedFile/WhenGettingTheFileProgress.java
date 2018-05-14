package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.specs.GivenAPausedFile;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.MediaPlayerPlaybackHandler;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheFileProgress {

	private static Duration progress;
	private static Duration duration;

	@BeforeClass
	public static void before() {
		final MediaPlayer mockMediaPlayer = mock(MediaPlayer.class);
		when(mockMediaPlayer.isPlaying()).thenReturn(false);
		when(mockMediaPlayer.getDuration()).thenReturn(200);

		final MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler = new MediaPlayerPlaybackHandler(mockMediaPlayer);
		progress = mediaPlayerPlaybackHandler.getProgress();
		duration = mediaPlayerPlaybackHandler.getDuration();
	}

	@Test
	public void thenTheFileProgressIsCorrect() {
		assertThat(progress).isEqualTo(Duration.ZERO);
	}

	@Test
	public void thenTheFileDurationIsCorrect() {
		assertThat(duration).isEqualTo(Duration.millis(200));
	}
}
