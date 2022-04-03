package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages

import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import org.joda.time.Duration

data class TrackPositionUpdate(val filePosition: Duration, val fileDuration: Duration) : ApplicationMessage
