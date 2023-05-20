package com.lasthopesoftware.bluewater.shared.exceptions

import java.io.IOException

open class DataNotReturnedException(message: String): IOException(message)
