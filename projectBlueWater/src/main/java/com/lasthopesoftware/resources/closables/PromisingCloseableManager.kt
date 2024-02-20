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
	private var head: HierarchicalLinkedNode? = null

	override fun <T : PromisingCloseable> manage(closeable: T): T {
		newNode(closeable)
		return closeable
	}

	override fun <T : AutoCloseable> manage(closeable: T): T {
		newNode(AutoCloseableWrapper(closeable))
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

	protected open fun newNode(closeable: PromisingCloseable): HierarchicalLinkedNode? {
		if (isSelf(closeable)) {
			return null
		}

		return HierarchicalLinkedNode(this, closeable)
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

	protected class HierarchicalLinkedNode(
		private val container: PromisingCloseableManager,
		private val closeable: PromisingCloseable
	)
	{
		@Volatile
		var prev: HierarchicalLinkedNode? = null
			private set
		@Volatile
		var next: HierarchicalLinkedNode? = null
			private set

		@Volatile
		var parentNode: HierarchicalLinkedNode? = null

		@Volatile
		var childNode: HierarchicalLinkedNode? = null

		init {
			synchronized(container.stackSync) {
				next = container.head
				container.head?.prev = this
				container.head = this
			}
		}

		fun remove(): PromisingCloseable = synchronized(container.stackSync) {
			childNode?.apply {
				parentNode = null
				remove()
			}

			parentNode?.apply {
				childNode = null
				remove()
			}

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

		override fun newNode(closeable: PromisingCloseable): HierarchicalLinkedNode? =
			super.newNode(closeable)
				?.also { childNode ->
					val parentNode = parent.newNode(closeable)
					childNode.parentNode = parentNode
					parentNode?.childNode = childNode
				}

		override fun createNestedManager(): ManagePromisingCloseables = NestedPromisingCloseableManager(this)
	}
}
