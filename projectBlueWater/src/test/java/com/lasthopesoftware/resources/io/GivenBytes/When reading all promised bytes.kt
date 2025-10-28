package com.lasthopesoftware.resources.io.GivenBytes

import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.closables.promiseUse
import com.lasthopesoftware.resources.io.PromisingChannel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When reading all promised bytes` {

	companion object {
		private const val chunkSize = 3
	}

	private val bytes by lazy {
		"Lorem ipsum dolor sit amet consectetur adipiscing elit quisque faucibus ex sapien vitae pellentesque sem placerat in id cursus mi pretium tellus duis convallis tempus leo eu aenean sed diam urna tempor pulvinar vivamus fringilla lacus nec metus bibendum egestas iaculis massa nisl malesuada lacinia integer nunc posuere ut hendrerit semper vel class aptent taciti sociosqu ad litora torquent per conubia nostra inceptos himenaeos orci varius natoque penatibus et magnis dis parturient montes nascetur ridiculus mus donec rhoncus eros lobortis nulla molestie mattis scelerisque maximus eget fermentum odio phasellus non purus est efficitur laoreet mauris pharetra vestibulum fusce dictum risus blandit quis suspendisse aliquet nisi sodales consequat magna ante condimentum neque at luctus nibh finibus facilisis dapibus etiam interdum tortor ligula congue sollicitudin erat viverra ac tincidunt nam porta elementum a enim euismod quam justo lectus commodo augue arcu dignissim velit aliquam imperdiet mollis nullam volutpat porttitor ullamcorper rutrum gravida cras eleifend turpis fames primis vulputate ornare sagittis vehicula praesent dui felis venenatis ultrices proin libero feugiat tristique accumsan maecenas potenti ultricies habitant morbi senectus netus suscipit auctor curabitur facilisi cubilia curae hac habitasse platea dictumst lorem ipsum dolor sit amet consectetur adipiscing elit quisque faucibus ex sapien vitae pellentesque sem placerat in id cursus mi pretium tellus duis convallis tempus leo eu aenean sed diam urna tempor pulvinar vivamus fringilla lacus nec metus bibendum egestas iaculis massa nisl malesuada lacinia integer nunc posuere ut hendrerit semper vel class aptent taciti sociosqu ad litora torquent per conubia nostra inceptos himenaeos.".toByteArray()
	}

	private var readBytes: ByteArray? = null

	@BeforeAll
	fun act() {
		val pipingInputStream = PromisingChannel()
		val promisedBytes = pipingInputStream.promiseUse { it.promiseReadAllBytes() }

		pipingInputStream.writableStream.promiseUse {
			it.promiseWrite(bytes, 0, bytes.size)
		}.toExpiringFuture().get()

		readBytes = promisedBytes.toExpiringFuture().get()
	}

	@Test
	fun `then the initial bytes are correct`() {
		assertThat(readBytes?.takeWhile { it == 0.toByte() }).isEmpty()
	}

	@Test
	fun `then the initial read bytes are correct`() {
		assertThat(readBytes?.dropWhile { it == 0.toByte() }?.take(chunkSize))
			.isEqualTo(bytes.dropWhile { it == 0.toByte() }.take(chunkSize))
	}

	@Test
	fun `then all read bytes are correct`() {
		assertThat(readBytes?.filterNot { it == 0.toByte() })
			.isEqualTo(bytes.dropWhile { it == 0.toByte() })
	}
}
