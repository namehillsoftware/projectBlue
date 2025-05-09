package com.lasthopesoftware.resources.closables

import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.compilation.DebugFlag
import com.lasthopesoftware.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.regardless
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

open class PromisingCloseableManager : ManagePromisingCloseables, ImmediateResponse<Throwable, Unit> {

	companion object {
		private val logger by lazyLogger<PromisingCloseableManager>()
		private const val closingMessage = "Closing {}."
		private const val closedMessage = "{} closed."
	}

	private val closeablesStack = LinkedNodeStack()

	private var nestedContainerStack = LinkedNodeStack()

	override fun <T : PromisingCloseable> manage(closeable: T): T {
		stack(closeable)
		return closeable
	}

	override fun <T : AutoCloseable> manage(closeable: T): T {
		stack(AutoCloseableWrapper(closeable))
		return closeable
	}

	override fun createNestedManager(): ManagePromisingCloseables = NestedPromisingCloseableManager(this)

	override fun promiseClose(): Promise<Unit> =
		(nestedContainerStack
			.pop()
			?.promiseClose()
			?: closeablesStack
				.pop()
				?.let { resource ->
					if (DebugFlag.isDebugCompilation && resource !is AutoCloseableWrapper)
						logger.debug(closingMessage, resource)

					resource
						.promiseClose()
						.then(if (!DebugFlag.isDebugCompilation || resource is AutoCloseableWrapper) forward() else ImmediateResponse{
							logger.debug(closedMessage, resource)
						}, this)
				})
			?.regardless { promiseClose() }
			.keepPromise(Unit)

	override fun respond(resolution: Throwable?) {
		logger.warn("There was an error closing a resource", resolution)
	}

	private fun stack(closeable: PromisingCloseable) {
		if (!isSelf(closeable))
			closeablesStack.push(closeable)
	}

	private fun isSelf(closeable: PromisingCloseable): Boolean {
		if (closeable === this) {
			if (DebugFlag.isDebugCompilation)
				logger.debug("Attempted to manage self! Returning.")
			return true
		}

		return false
	}

	private class AutoCloseableWrapper(private val closeable: AutoCloseable) : PromisingCloseable {
		override fun promiseClose(): Promise<Unit> {
			if (DebugFlag.isDebugCompilation)
				logger.debug(closingMessage, closeable)

			closeable.close()

			if (DebugFlag.isDebugCompilation)
				logger.debug(closedMessage, closeable)

			return Unit.toPromise()
		}
	}

	private class LinkedNodeStack {

		private val stackSync = Any()

		@Volatile
		private var head: LinkedNode? = null

		fun push(closeable: PromisingCloseable): LinkedNode = LinkedNode(closeable)

		fun pop(): PromisingCloseable? = synchronized(stackSync) { head?.remove() }

		inner class LinkedNode(private val closeable: PromisingCloseable) {

			@Volatile
			private var prev: LinkedNode? = null

			@Volatile
			private var next: LinkedNode? = null

			init {
				synchronized(stackSync) {
					next = head
					head?.prev = this
					head = this
				}
			}

			fun remove(): PromisingCloseable = synchronized(stackSync) {
				// Take out of the chain
				prev?.next = next
				next?.prev = prev

				if (head === this)
					head = next

				// Break the links
				next = null
				prev = null

				return closeable
			}
		}
	}

	private class NestedPromisingCloseableManager(parent: PromisingCloseableManager) : PromisingCloseableManager() {

		private val node = parent.nestedContainerStack.push(this)

		override fun createNestedManager(): ManagePromisingCloseables = NestedPromisingCloseableManager(this)

		override fun promiseClose(): Promise<Unit> {
			node.remove()
			return super.promiseClose()
		}
	}
}
