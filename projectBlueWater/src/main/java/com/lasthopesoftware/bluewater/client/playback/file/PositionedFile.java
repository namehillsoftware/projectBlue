package com.lasthopesoftware.bluewater.client.playback.file;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

public final class PositionedFile implements Comparable<PositionedFile> {
	private final int playlistPosition;
	private final ServiceFile serviceFile;

	public PositionedFile(int playlistPosition, ServiceFile serviceFile) {
		this.playlistPosition = playlistPosition;
		this.serviceFile = serviceFile;
	}

	public int getPlaylistPosition() {
		return playlistPosition;
	}

	public ServiceFile getServiceFile() {
		return serviceFile;
	}

	@Override
	public int hashCode() {
		return serviceFile.hashCode() * 31 + playlistPosition;
	}

	@Override
	public int compareTo(@NonNull PositionedFile other) {
		final int playlistComparison = playlistPosition - other.playlistPosition;
		return playlistComparison != 0
			? playlistComparison
			: serviceFile.compareTo(other.serviceFile);

	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof PositionedFile && compareTo((PositionedFile) obj) == 0;
	}

	@Override
	public String toString() {
		return "playlistPosition: " + playlistPosition + ", serviceFile" + serviceFile;
	}
}
