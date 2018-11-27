package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer;

import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MetadataOutputLogger implements MetadataOutput {

	private static final Logger logger = LoggerFactory.getLogger(MetadataOutputLogger.class);

	@Override
	public void onMetadata(Metadata metadata) {
		if (!logger.isDebugEnabled()) return;

		logger.debug("New metadata");
	}
}
