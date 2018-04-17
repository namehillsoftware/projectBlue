package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.specs.GivenAPlayingMediaPlayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.MediaPlayerPlaybackHandler;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingThePlaybackDuration {

	private static Duration duration;

	@BeforeClass
	public static void before() {
		final MediaPlayer mockMediaPlayer = mock(MediaPlayer.class);
		when(mockMediaPlayer.isPlaying()).thenReturn(true);
		when(mockMediaPlayer.getCurrentPosition()).thenReturn(50);
		when(mockMediaPlayer.getDuration()).thenReturn(100);

		final MediaPlayerPlaybackHandler mediaPlayerPlaybackHandler = new MediaPlayerPlaybackHandler(mockMediaPlayer);

		duration = mediaPlayerPlaybackHandler.getDuration();
	}

	@Test
	public void thenThePlaybackPositionIsCorrect() {
		assertThat(duration).isEqualTo(Duration.millis(100));
	}
}
