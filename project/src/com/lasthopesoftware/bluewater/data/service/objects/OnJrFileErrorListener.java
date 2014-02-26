package com.lasthopesoftware.bluewater.data.service.objects;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFileMediaPlayer;

public interface OnJrFileErrorListener {
	boolean onJrFileError(JrFileMediaPlayer mediaPlayer, int what, int extra);
}
