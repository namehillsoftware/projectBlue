package com.lasthopesoftware.bluewater.services;

import com.lasthopesoftware.bluewater.data.service.objects.JrFile;

public interface OnStreamingStartListener {
	public void onStreamingStart(StreamingMusicService service, JrFile file);
}
