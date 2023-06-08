package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import java.io.IOException

class PreparationException internal constructor(
    val positionedFile: PositionedFile,
    cause: Throwable?
) : IOException(cause)
