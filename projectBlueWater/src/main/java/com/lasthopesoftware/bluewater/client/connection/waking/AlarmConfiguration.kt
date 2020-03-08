package com.lasthopesoftware.bluewater.client.connection.waking

import org.joda.time.Duration

data class AlarmConfiguration(val timesToWake: Int, val timesBetweenWaking: Duration)
