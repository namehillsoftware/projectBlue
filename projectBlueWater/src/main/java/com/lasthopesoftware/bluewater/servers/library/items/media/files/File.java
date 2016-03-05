package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;

public class File implements IFile {

	private int key;

	public File(int key) {
		this.setKey(key);
	}
	
	@Override
	public int getKey() {
		return key;
	}

	@Override
	public void setKey(int key) {
		this.key = key;
	}

	public String[] getPlaybackParams() {
		/* Playback:
		 * 0: Downloading (not real-time playback);
		 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.;
		 * 2: Real-time playback, no playback statistics handling (default: )
		 */

		return new String[] { "File/GetFile", "File=" + Integer.toString(getKey()), "Quality=medium", "Conversion=Android", "Playback=0" };
	}
	
	public String getPlaybackUrl(ConnectionProvider connectionProvider) {
		return connectionProvider.getUrlProvider().getUrl(getPlaybackParams());
	}

	@Override
	public int compareTo(IFile another) {
		return another == null ? 1 : getKey() - another.getKey();
	}
}
