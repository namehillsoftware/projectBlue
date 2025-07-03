package com.lasthopesoftware.bluewater.client.playback.errors

import org.joda.time.Duration
import java.util.concurrent.TimeoutException

class PlaybackResourceNotAvailableInTimeException(resource: String, waitingTime: Duration)
	: TimeoutException("Resource \"$resource\" was not available in $waitingTime.")
