package com.lasthopesoftware.bluewater.services;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFileMediaPlayer;

public interface OnStreamingStopListener {
	public void onStreamingStop(StreamingMusicService service, JrFileMediaPlayer filePlayer);
}
