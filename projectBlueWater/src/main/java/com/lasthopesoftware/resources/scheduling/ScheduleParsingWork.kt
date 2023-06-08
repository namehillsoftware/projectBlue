package com.lasthopesoftware.resources.scheduling

import java.util.concurrent.Executor

interface ScheduleParsingWork {
    val scheduler: Executor?
}
