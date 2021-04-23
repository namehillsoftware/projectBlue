package com.lasthopesoftware.bluewater.shared.promises.extensions

import android.content.Context
import android.os.Handler
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.PreparedMessengerOperator
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellablePreparedMessengerOperator
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import org.joda.time.Duration

open class LoopedInPromise<Result> : Promise<Result> {
	constructor(task: MessageWriter<Result>, context: Context)
		: this(task, Handler(context.mainLooper))

	constructor(task: MessageWriter<Result>, handler: Handler)
		: super(Executors.LoopedInResponse<Result>(PreparedMessengerOperator<Result>(task), handler))

	constructor(task: MessageWriter<Result>, context: Context, delay: Duration)
		: this(task, Handler(context.mainLooper), delay)

	constructor(task: MessageWriter<Result>, handler: Handler, delay: Duration)
		: super(Executors.DelayedLoopedInPromise<Result>(PreparedMessengerOperator<Result>(task), handler, delay))

	constructor(task: CancellableMessageWriter<Result>, handler: Handler)
		: super(Executors.LoopedInResponse<Result>(CancellablePreparedMessengerOperator<Result>(task), handler))

	constructor(task: MessengerOperator<Result>, handler: Handler)
		: super(Executors.LoopedInResponse<Result>(task, handler))

	private class Executors {
		class LoopedInResponse<Result>(private val task: MessengerOperator<Result>, private val handler: Handler) : MessengerOperator<Result>, Runnable {
			private lateinit var resultMessenger: Messenger<Result>

			override fun send(resultMessenger: Messenger<Result>) {
				this.resultMessenger = resultMessenger
				if (handler.looper.thread === Thread.currentThread()) run() else handler.post(this)
			}

			override fun run() {
				try {
					task.send(resultMessenger)
				} catch (err: Throwable) {
					resultMessenger.sendRejection(err)
				}
			}
		}

		class DelayedLoopedInPromise<Result>(private val task: MessengerOperator<Result>, private val handler: Handler, private val delay: Duration) : MessengerOperator<Result>, Runnable {
			private lateinit var resultMessenger: Messenger<Result>

			override fun send(resultMessenger: Messenger<Result>) {
				this.resultMessenger = resultMessenger
				handler.postDelayed(this, delay.millis)
			}

			override fun run() {
				try {
					task.send(resultMessenger)
				} catch (err: Throwable) {
					resultMessenger.sendRejection(err)
				}
			}
		}
	}

	private class OneParameterExecutors {
		class ReducingFunction<TResult, TNewResult>(private val callable: ImmediateResponse<TResult, TNewResult>,	private val handler: Handler) :
			PromisedResponse<TResult, TNewResult>, MessageWriter<TNewResult> {

			private var result: TResult? = null

			override fun prepareMessage(): TNewResult = callable.respond(result)

			override fun promiseResponse(result: TResult): Promise<TNewResult> {
				this.result = result
				return LoopedInPromise(this, handler)
			}
		}
	}

	companion object {
		@JvmStatic
		fun <TResult, TNewResult> response(task: ImmediateResponse<TResult, TNewResult>, context: Context): PromisedResponse<TResult, TNewResult> {
			return response(task, Handler(context.mainLooper))
		}

		@JvmStatic
		fun <TResult, TNewResult> response(task: ImmediateResponse<TResult, TNewResult>, handler: Handler): PromisedResponse<TResult, TNewResult> {
			return OneParameterExecutors.ReducingFunction(task, handler)
		}
	}
}
