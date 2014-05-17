package com.lasthopesoftware.bluewater.data.service.objects;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;

public interface OnFileCompleteListener {
	void onJrFileComplete(JrFilePlayer mediaPlayer);
}
