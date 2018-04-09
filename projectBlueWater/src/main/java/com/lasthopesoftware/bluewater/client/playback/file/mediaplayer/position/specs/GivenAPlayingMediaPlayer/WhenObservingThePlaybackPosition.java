package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.position.specs.GivenAPlayingMediaPlayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.PlayingFileProgress;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.position.MediaPlayerPositionObservableProvider;

import org.joda.time.Period;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//@RunWith(RobolectricTestRunner.class)
public class WhenObservingThePlaybackPosition {

	private static PlayingFileProgress progress;

	@BeforeClass
	public static void before() {
		final MediaPlayer mockMediaPlayer = mock(MediaPlayer.class);
		when(mockMediaPlayer.isPlaying()).thenReturn(true);
		when(mockMediaPlayer.getCurrentPosition()).thenReturn(50);
		when(mockMediaPlayer.getDuration()).thenReturn(100);

		final MediaPlayerPositionObservableProvider mediaPlayerPlaybackHandler = new MediaPlayerPositionObservableProvider(mockMediaPlayer);
		progress = mediaPlayerPlaybackHandler
			.observePlayingFileProgress(Period.ZERO)
			.blockingFirst();
	}

	@Test
	public void thenThePlaybackPositionIsCorrect() {
		assertThat(progress.position).isEqualTo(50);
	}

	@Test
	public void thenThePlaybackDurationIsCorrect() {
		assertThat(progress.duration).isEqualTo(100);
	}
}
