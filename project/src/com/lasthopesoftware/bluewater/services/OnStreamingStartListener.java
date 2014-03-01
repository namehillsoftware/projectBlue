package com.lasthopesoftware.bluewater.services;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;

public interface OnStreamingStartListener {
	public void onStreamingStart(StreamingMusicService service, JrFilePlayer filePlayer);
}
