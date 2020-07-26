package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.lazyj.Lazy
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.PeriodFormatterBuilder
import java.util.*
import kotlin.math.ceil

class FormattedSessionFilePropertiesProvider(connectionProvider: IConnectionProvider?, filePropertiesContainerProvider: IFilePropertiesContainerRepository?) : SessionFilePropertiesProvider(connectionProvider!!, filePropertiesContainerProvider!!) {

	companion object {
		private val yearFormatter = lazy { DateTimeFormatterBuilder().appendYear(4, 4).toFormatter() }

		private val dateFormatterBuilder = lazy {
			DateTimeFormatterBuilder()
				.appendMonthOfYear(1)
				.appendLiteral('/')
				.appendDayOfMonth(1)
				.appendLiteral('/')
				.append(yearFormatter.value)
		}

		private val dateFormatter = lazy { dateFormatterBuilder.value.toFormatter() }

		private val dateTimeFormatter = lazy {
			dateFormatterBuilder.value
				.appendLiteral(" at ")
				.appendClockhourOfHalfday(1)
				.appendLiteral(':')
				.appendMinuteOfHour(2)
				.appendLiteral(' ')
				.appendHalfdayOfDayText()
				.toFormatter()
		}

		private val minutesAndSecondsFormatter = lazy {
			PeriodFormatterBuilder()
				.appendMinutes()
				.appendSeparator(":")
				.minimumPrintedDigits(2)
				.maximumParsedDigits(2)
				.appendSeconds()
				.toFormatter()
		}

		private val excelEpoch = lazy { DateTime(1899, 12, 30, 0, 0) }

		private val dateTimeProperties = lazy {
			setOf(
				KnownFileProperties.LAST_PLAYED,
				KnownFileProperties.LAST_SKIPPED,
				KnownFileProperties.DATE_CREATED,
				KnownFileProperties.DATE_IMPORTED,
				KnownFileProperties.DATE_MODIFIED,
				KnownFileProperties.DATE_TAGGED,
				KnownFileProperties.DATE_FIRST_RATED,
				KnownFileProperties.DATE_LAST_OPENED) }

		/* Formatted properties helpers */
		private fun buildFormattedReadonlyProperties(unformattedProperties: Map<String, String>): Map<String, String> {
			val formattedProperties = HashMap<String, String>(unformattedProperties.size)
			for ((key, value) in unformattedProperties)
				formattedProperties[key] = getFormattedValue(key, value)

			return formattedProperties
		}

		private fun getFormattedValue(name: String, value: String?): String {
			if (value.isNullOrEmpty()) return ""

			return if (dateTimeProperties.value.contains(name)) {
				val dateTime = DateTime((value.toDouble() * 1000).toLong())
				dateTime.toString(dateTimeFormatter.value)
			} else when (name) {
				KnownFileProperties.DATE -> {
					var daysValue = value
					val periodPos = daysValue.indexOf('.')
					if (periodPos > -1) daysValue = daysValue.substring(0, periodPos)

					val returnDate = excelEpoch.value.plusDays(daysValue.toInt())
					returnDate.toString(
						if (returnDate.monthOfYear == 1 && returnDate.dayOfMonth == 1) yearFormatter.value
						else dateFormatter.value)
				}
				KnownFileProperties.FILE_SIZE -> {
					val fileSizeBytes = ceil(value.toLong().toDouble() / 1024 / 1024 * 100) / 100
					"$fileSizeBytes MB"
				}
				KnownFileProperties.DURATION -> {
					Duration.standardSeconds(value.toDouble().toLong()).toPeriod().toString(minutesAndSecondsFormatter.value)
				}
				else -> value
			}
		}
	}

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> {
		return super
			.promiseFileProperties(serviceFile)
			.then { unformattedProperties -> buildFormattedReadonlyProperties(unformattedProperties) }
	}
}
