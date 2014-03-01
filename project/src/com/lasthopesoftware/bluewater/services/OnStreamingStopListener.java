package com.lasthopesoftware.bluewater.services;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;

public interface OnStreamingStopListener {
	public void onStreamingStop(StreamingMusicService service, JrFilePlayer filePlayer);
}
