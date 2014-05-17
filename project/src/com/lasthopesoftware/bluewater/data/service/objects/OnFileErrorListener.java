package com.lasthopesoftware.bluewater.data.service.objects;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;

public interface OnFileErrorListener {
	boolean onJrFileError(FilePlayer mediaPlayer, int what, int extra);
}
