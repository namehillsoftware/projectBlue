package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPlaybackQueueProvider;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.BufferingPlaybackQueuesProvider;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayerProducer;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import io.reactivex.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 12/17/16.
 */

public class WhenGettingANonCyclicalPlaybackQueue {

	private static Collection<PositionedPlaybackFile> playedFiles;

	@BeforeClass
	public static void setup() {
		final IPlaybackPreparerTaskFactory playbackPreparerTaskFactory = mock(IPlaybackPreparerTaskFactory.class);
		when(playbackPreparerTaskFactory.getPlaybackPreparerTask(any(), any())).thenReturn((resolve, reject, onCancelled) -> resolve.withResult(mock(IBufferingPlaybackHandler.class)));

		final PlaylistPlayerProducer playlistPlayerProducer =
			new PlaylistPlayerProducer(new BufferingPlaybackQueuesProvider(playbackPreparerTaskFactory));

		final IPlaylistPlayer playlistPlayer = playlistPlayerProducer.getPlaylistPlayer(Arrays.asList(new File(1), new File(2), new File(3)), 0, 0, false);
		Observable.create(playlistPlayer).toList().subscribe(positionedPlaybackFiles -> playedFiles = positionedPlaybackFiles);
	}

	@Test
	public void thenTheNewPlaybackQueuePlaysUntilCompletion() {
		assertThat(Stream.of(playedFiles).map(File::getKey).collect(Collectors.toList())).containsExactly(1, 2, 3);
	}
}
