package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;


import com.vedsoft.lazyj.AbstractSynchronousLazy;
import com.vedsoft.lazyj.ILazy;

import java.util.Arrays;

public class QueueProviders {
	private static final ILazy<Iterable<IPositionedFileQueueProvider>> lazyProviders = new AbstractSynchronousLazy<Iterable<IPositionedFileQueueProvider>>() {
		@Override
		protected Iterable<IPositionedFileQueueProvider> initialize() throws Exception {
			return Arrays.asList(new CompletingFileQueueProvider(), new CyclicalFileQueueProvider());
		}
	};

	public static Iterable<IPositionedFileQueueProvider> providers() {
		return lazyProviders.getObject();
	}
}
