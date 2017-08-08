package com.lasthopesoftware.bluewater.client.servers;

public class ProgramVersion {
	public final int major;
	public final int minor;
	public final int patch;
	public final String label;

	public ProgramVersion(int major, int minor, int patch, String label) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.label = label;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ProgramVersion)) return false;

		final ProgramVersion other = (ProgramVersion) obj;
		return other.major == major && other.minor == minor && other.patch == patch && ((label == null && other.label != null) || label.equals(other.label));
	}
}
