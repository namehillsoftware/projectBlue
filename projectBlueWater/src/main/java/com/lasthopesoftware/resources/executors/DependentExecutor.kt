package com.lasthopesoftware.resources.executors

import java.util.concurrent.*

class DependentExecutor(private val parent: Executor) : Runnable {

	private val taskQueue = ConcurrentLinkedQueue<InnerExecutor.DeferredFuture>()

	fun innerExecutor(inner: Executor) : Executor = InnerExecutor(inner)

	@Synchronized
	override fun run() {
		val startedTasks = ArrayList<FutureTask<Unit>>(taskQueue.size)
		var task: InnerExecutor.DeferredFuture?
		while (taskQueue.poll().also { task = it } != null)
			task?.start()?.apply(startedTasks::add)

		for (startedTask in startedTasks) startedTask.get()
	}

	private inner class InnerExecutor(private val inner: Executor) : Executor {
		override fun execute(command: Runnable?) {
			if (command == null) return

			taskQueue.offer(DeferredFuture(FutureTask(command, Unit)))
			parent.execute(this@DependentExecutor)
		}

		inner class DeferredFuture(private val innerFutureTask: FutureTask<Unit>) {
			fun start(): FutureTask<Unit> {
				inner.execute(innerFutureTask)
				return innerFutureTask
			}
		}
	}
}
