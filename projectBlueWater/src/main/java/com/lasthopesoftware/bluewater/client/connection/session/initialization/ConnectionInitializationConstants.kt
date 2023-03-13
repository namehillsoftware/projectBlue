package com.lasthopesoftware.bluewater.client.connection.session.initialization

import org.joda.time.Duration

object ConnectionInitializationConstants {
	val dramaticPause by lazy { Duration.standardSeconds(2).plus(Duration.millis(500)) }
}
