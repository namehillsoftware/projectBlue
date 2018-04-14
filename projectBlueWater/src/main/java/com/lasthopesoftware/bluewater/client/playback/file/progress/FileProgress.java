package com.lasthopesoftware.bluewater.client.playback.file.progress;

import java.util.Objects;

public class FileProgress {
	public final long position;
	public final long duration;

	public FileProgress(long position, long duration) {
		this.position = position;
		this.duration = duration;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FileProgress that = (FileProgress) o;
		return position == that.position &&
			duration == that.duration;
	}

	@Override
	public int hashCode() {
		return Objects.hash(position, duration);
	}

	@Override
	public String toString() {
		return "FileProgress{" +
			"position=" + position +
			", duration=" + duration +
			'}';
	}
}
