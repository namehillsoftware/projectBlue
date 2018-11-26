package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer;

import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class TextOutputLogger implements TextOutput {

	private static final Logger logger = LoggerFactory.getLogger(TextOutputLogger.class);

	@Override
	public void onCues(List<Cue> cues) {
		if (!logger.isDebugEnabled()) return;
		
		logger.debug("Cues updated");
	}
}
