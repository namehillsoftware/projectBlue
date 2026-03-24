package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.ConfigurePreparedPlaybackQueue

class FakePlaybackQueueConfiguration(override val maxQueueSize: Int = 1) : ConfigurePreparedPlaybackQueue
