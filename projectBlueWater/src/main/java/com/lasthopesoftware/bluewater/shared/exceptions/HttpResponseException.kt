package com.lasthopesoftware.bluewater.shared.exceptions

import java.io.IOException

class HttpResponseException : IOException {
    val responseCode: Int

    constructor(responseCode: Int) : super() {
        this.responseCode = responseCode
    }

    constructor(responseCode: Int, message: String?) : super(message) {
        this.responseCode = responseCode
    }

    constructor(responseCode: Int, message: String?, cause: Throwable?) : super(message, cause) {
        this.responseCode = responseCode
    }

    constructor(responseCode: Int, cause: Throwable?) : super(cause) {
        this.responseCode = responseCode
    }
}
