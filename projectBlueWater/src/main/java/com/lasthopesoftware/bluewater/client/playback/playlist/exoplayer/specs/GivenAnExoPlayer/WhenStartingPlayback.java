package com.lasthopesoftware.bluewater.client.playback.playlist.exoplayer.specs.GivenAnExoPlayer;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;
import com.lasthopesoftware.bluewater.client.playback.playlist.exoplayer.ExoPlaylistPlayer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Arrays;
import io.reactivex.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenStartingPlayback {

	private List<PositionedPlayingFile> positionedPlayingFiles = new ArrayList<>();
	private Player.EventListener listener;

	@Before
	public void before() {
		final ExoPlayer exoPlayer = mock(ExoPlayer.class);
		doAnswer((Answer<Void>) invocation -> {
			listener = invocation.getArgument(0);
			return null;
		}).when(exoPlayer).addListener(any());

		when(exoPlayer.getCurrentWindowIndex())
			.thenReturn(1)
			.thenReturn(2)
			.thenReturn(3)
			.thenReturn(4)
			.thenReturn(5);

		Observable
			.create(
				new ExoPlaylistPlayer(exoPlayer,
				Arrays.asList(new ServiceFile[] {
					new ServiceFile(1),
					new ServiceFile(1),
					new ServiceFile(1),
					new ServiceFile(1),
					new ServiceFile(1)
				})))
			.subscribe(this.positionedPlayingFiles::add);

		listener.onPositionDiscontinuity(Player.DISCONTINUITY_REASON_PERIOD_TRANSITION);
		listener.onPositionDiscontinuity(Player.DISCONTINUITY_REASON_PERIOD_TRANSITION);
		listener.onPositionDiscontinuity(Player.DISCONTINUITY_REASON_PERIOD_TRANSITION);
		listener.onPositionDiscontinuity(Player.DISCONTINUITY_REASON_PERIOD_TRANSITION);
		listener.onPositionDiscontinuity(Player.DISCONTINUITY_REASON_PERIOD_TRANSITION);
	}

	@Test
	public void thenThePlaybackCountIsCorrect() {
		assertThat(this.positionedPlayingFiles.size()).isEqualTo(5);
	}
}
