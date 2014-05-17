package com.lasthopesoftware.bluewater.data.service.objects;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;

public interface OnFilePreparedListener {
	void onJrFilePrepared(JrFilePlayer mediaPlayer);
}
