package com.lasthopesoftware.bluewater.data.service.objects;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;

public interface OnJrFileErrorListener {
	boolean onJrFileError(JrFilePlayer mediaPlayer, int what, int extra);
}
