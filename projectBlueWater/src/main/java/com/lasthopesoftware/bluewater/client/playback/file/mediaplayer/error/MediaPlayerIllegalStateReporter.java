package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaPlayerIllegalStateReporter {

	private final Logger logger;

	public MediaPlayerIllegalStateReporter(Class cls) {
		this.logger = LoggerFactory.getLogger(cls);
	}


	public void reportIllegalStateException(IllegalStateException se, String attemptedAction) {
		logger.warn("The media player was in an illegal state when " + attemptedAction, se);
	}
}
