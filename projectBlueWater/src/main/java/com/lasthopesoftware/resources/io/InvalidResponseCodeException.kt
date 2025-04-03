package com.lasthopesoftware.resources.io

import java.io.IOException

class InvalidResponseCodeException(val responseCode: Int) : IOException("Unexpected response code: $responseCode.")
