package com.lasthopesoftware.bluewater.client.playback.queues;


import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.ILazy;

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
