package com.lasthopesoftware.bluewater.services;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFileMediaPlayer;

public interface OnStreamingStartListener {
	public void onStreamingStart(StreamingMusicService service, JrFileMediaPlayer filePlayer);
}
