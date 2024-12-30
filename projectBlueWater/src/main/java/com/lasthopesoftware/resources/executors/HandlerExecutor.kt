package com.lasthopesoftware.resources.executors

import android.os.Handler
import java.util.concurrent.Executor

class HandlerExecutor(val handler: Handler) : Executor {
	override fun execute(command: Runnable) {
		if (handler.looper.thread === Thread.currentThread()) command.run()
		else handler.post(command)
	}
}
