package com.lasthopesoftware.bluewater.shared.exceptions

import android.content.Context
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

private val logger by lazyLogger<UnexpectedExceptionToasterResponse>()

class UnexpectedExceptionToasterResponse(private val context: Context) : ImmediateResponse<Throwable?, Unit> {
    override fun respond(error: Throwable?) {
		logger.error("An unexpected exception occurred, announcing it.", error)
		if (error != null)
        	UnexpectedExceptionToaster.announce(context, error)
    }
}
