package com.lasthopesoftware.bluewater.services;

import com.lasthopesoftware.bluewater.data.objects.JrFile;

public interface OnStreamingStopListener {
	public void onStreamingStop(StreamingMusicService service, JrFile file);
}
