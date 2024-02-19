package com.lasthopesoftware.resources.closables

import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class PromisingCloseableManager : ManagePromisingCloseables {

	companion object {
		private val logger by lazyLogger<PromisingCloseableManager>()
	}

	private val stackSync = Any()

	@Volatile
	private var head: LinkedNode? = null

	override fun <T : PromisingCloseable> manage(closeable: T): T {
		if (closeable === this) {
			if (BuildConfig.DEBUG)
				logger.debug("Attempted to manage self! Returning.")
			return closeable
		}

		LinkedNode(this, closeable)
		return closeable
	}

	override fun <T : AutoCloseable> manage(closeable: T): T {
		manage(AutoCloseableWrapper(closeable))
		return closeable
	}

	override fun createNestedManager(): ManagePromisingCloseables = NestedPromisingCloseableManager(this)

	override fun promiseClose(): Promise<Unit> =
		head
			?.remove()
			?.also {
				if (BuildConfig.DEBUG)
					logger.debug("Closing {}.", it)
			}
			?.promiseClose()
			?.eventually(
				{ promiseClose() },
				{ e ->
					logger.warn("There was an error closing a resource", e)
					promiseClose()
				})
			.keepPromise(Unit)

	private class AutoCloseableWrapper(private val closeable: AutoCloseable) : PromisingCloseable {
		override fun promiseClose(): Promise<Unit> {
			if (BuildConfig.DEBUG)
				logger.debug("Closing {}.", closeable)

			closeable.close()
			return Unit.toPromise()
		}
	}

	private class LinkedNode(
		private val container: PromisingCloseableManager,
		private val closeable: PromisingCloseable
	)
	{
		@Volatile
		var prev: LinkedNode? = null
			private set
		@Volatile
		var next: LinkedNode? = null
			private set

		init {
			synchronized(container.stackSync) {
				next = container.head
				container.head?.prev = this
				container.head = this
			}
		}

		fun remove(): PromisingCloseable = synchronized(container.stackSync) {
			// Take out of the chain
			prev?.next = next
			next?.prev = prev

			if (container.head === this)
				container.head = next

			// Break the links
			next = null
			prev = null

			return closeable
		}
	}

	private class NestedPromisingCloseableManager(private val parent: PromisingCloseableManager) : PromisingCloseableManager() {

		private val node = LinkedNode(parent, this)

		override fun <T : PromisingCloseable> manage(closeable: T): T {
			parent.manage(closeable)
			return super.manage(closeable)
		}

		override fun createNestedManager(): ManagePromisingCloseables = NestedPromisingCloseableManager(this)

		override fun promiseClose(): Promise<Unit> {
			node.remove()
			return super.promiseClose()
		}
	}
}
