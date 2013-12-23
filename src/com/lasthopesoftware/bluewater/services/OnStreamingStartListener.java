package com.lasthopesoftware.bluewater.services;

import com.lasthopesoftware.bluewater.data.objects.JrFile;

public interface OnStreamingStartListener {
	public void onStreamingStart(StreamingMusicService service, JrFile file);
}
