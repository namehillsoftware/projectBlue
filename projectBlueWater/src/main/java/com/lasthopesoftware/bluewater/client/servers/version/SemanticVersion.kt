package com.lasthopesoftware.bluewater.client.servers.version;

public class SemanticVersion {
	public final int major;
	public final int minor;
	public final int patch;
	public final String label;

	public SemanticVersion(int major, int minor, int patch) {
		this(major, minor, patch, "");
	}

	public SemanticVersion(int major, int minor, int patch, String label) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.label = label;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SemanticVersion)) return false;

		final SemanticVersion other = (SemanticVersion) obj;
		return other.major == major && other.minor == minor && other.patch == patch && ((label == null && other.label == null) || (label != null && label.equals(other.label)));
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + patch + label;
	}
}
