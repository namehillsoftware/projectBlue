package com.lasthopesoftware.policies.retries

import kotlin.coroutines.cancellation.CancellationException

class RetriesCancelledException : CancellationException("Retries cancelled")
