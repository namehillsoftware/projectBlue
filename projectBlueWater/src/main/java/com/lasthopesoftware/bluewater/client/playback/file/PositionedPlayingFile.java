package com.lasthopesoftware.bluewater.client.playback.file;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;

public final class PositionedPlayingFile implements Comparable<PositionedPlayingFile> {
	private final PlayingFile playingFile;
	private final ManagePlayableFileVolume playableFileVolumeManager;
	private final PositionedFile positionedFile;

	public PositionedPlayingFile(int playlistPosition, PlayingFile playingFile, ManagePlayableFileVolume playableFileVolumeManager, ServiceFile serviceFile) {
		this(playingFile, playableFileVolumeManager, new PositionedFile(playlistPosition, serviceFile));
	}

	public PositionedPlayingFile(PlayingFile playingFile, ManagePlayableFileVolume playableFileVolumeManager, PositionedFile positionedFile) {
		this.playableFileVolumeManager = playableFileVolumeManager;
		this.positionedFile = positionedFile;
		this.playingFile = playingFile;
	}

	public int getPlaylistPosition() {
		return positionedFile.getPlaylistPosition();
	}

	public ServiceFile getServiceFile() {
		return positionedFile.getServiceFile();
	}

	public PlayingFile getPlayingFile() {
		return playingFile;
	}

	public ManagePlayableFileVolume getPlayableFileVolumeManager() {
		return playableFileVolumeManager;
	}

	@Override
	public int compareTo(@NonNull PositionedPlayingFile other) {
		return positionedFile.compareTo(other.positionedFile);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PositionedPlayingFile && compareTo((PositionedPlayingFile)obj) == 0;
	}

	@Override
	public int hashCode() {
		return positionedFile.hashCode();
	}

	public PositionedFile asPositionedFile() {
		return positionedFile;
	}
}
