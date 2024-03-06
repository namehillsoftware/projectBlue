package com.lasthopesoftware.resources.closables

import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class PromisingCloseableManager : ManagePromisingCloseables {

	companion object {
		private val logger by lazyLogger<PromisingCloseableManager>()
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
				?.also {
					if (BuildConfig.DEBUG)
						logger.debug("Closing {}.", it)
				}
				?.promiseClose())
				?.eventually(
					{ promiseClose() },
					{ e ->
						logger.warn("There was an error closing a resource", e)
						promiseClose()
					})
			.keepPromise(Unit)

	private fun stack(closeable: PromisingCloseable) {
		if (!isSelf(closeable))
			closeablesStack.push(closeable)
	}

	private fun isSelf(closeable: PromisingCloseable): Boolean {
		if (closeable === this) {
			if (BuildConfig.DEBUG)
				logger.debug("Attempted to manage self! Returning.")
			return true
		}

		return false
	}

	private class AutoCloseableWrapper(private val closeable: AutoCloseable) : PromisingCloseable {
		override fun promiseClose(): Promise<Unit> {
			if (BuildConfig.DEBUG)
				logger.debug("Closing {}.", closeable)

			closeable.close()
			return Unit.toPromise()
		}
	}

	private class LinkedNodeStack {

		private val stackSync = Any()

		@Volatile
		private var head: LinkedNode? = null

		fun push(closeable: PromisingCloseable): LinkedNode = LinkedNode(closeable)

		fun pop(): PromisingCloseable? = head?.remove()

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
