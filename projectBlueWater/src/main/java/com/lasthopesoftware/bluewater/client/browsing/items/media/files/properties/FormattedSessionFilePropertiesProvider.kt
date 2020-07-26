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
		private val yearFormatter = Lazy { DateTimeFormatterBuilder().appendYear(4, 4).toFormatter() }

		private val dateFormatterBuilder = Lazy {
			DateTimeFormatterBuilder()
				.appendMonthOfYear(1)
				.appendLiteral('/')
				.appendDayOfMonth(1)
				.appendLiteral('/')
				.append(yearFormatter.getObject())
		}

		private val dateFormatter = Lazy { dateFormatterBuilder.getObject().toFormatter() }

		private val dateTimeFormatter = Lazy {
			dateFormatterBuilder.getObject()
				.appendLiteral(" at ")
				.appendClockhourOfHalfday(1)
				.appendLiteral(':')
				.appendMinuteOfHour(2)
				.appendLiteral(' ')
				.appendHalfdayOfDayText()
				.toFormatter()
		}

		private val minutesAndSecondsFormatter = Lazy {
			PeriodFormatterBuilder()
				.appendMinutes()
				.appendSeparator(":")
				.minimumPrintedDigits(2)
				.maximumParsedDigits(2)
				.appendSeconds()
				.toFormatter()
		}

		private val excelEpoch = Lazy { DateTime(1899, 12, 30, 0, 0) }

		private val dateTimeProperties = Lazy {
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

			if (dateTimeProperties.getObject().contains(name)) {
				val dateTime = DateTime(value.toDouble() * 1000)
				return dateTime.toString(dateTimeFormatter.getObject())
			}

			if (KnownFileProperties.DATE == name) {
				var daysValue: String = value
				val periodPos = daysValue.indexOf('.')

				if (periodPos > -1) daysValue = daysValue.substring(0, periodPos)

				val returnDate = excelEpoch.getObject().plusDays(daysValue.toInt())
				return returnDate.toString(
					if (returnDate.monthOfYear == 1 && returnDate.dayOfMonth == 1) yearFormatter.getObject()
					else dateFormatter.getObject())
			}

			if (KnownFileProperties.FILE_SIZE == name) {
				val fileSizeBytes = ceil(value.toLong().toDouble() / 1024 / 1024 * 100) / 100
				return "$fileSizeBytes MB"
			}

			return if (KnownFileProperties.DURATION != name) value
			else Duration.standardSeconds(value.toDouble().toLong()).toPeriod().toString(minutesAndSecondsFormatter.getObject())
		}
	}

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> {
		return super
			.promiseFileProperties(serviceFile)
			.then { unformattedProperties -> buildFormattedReadonlyProperties(unformattedProperties) }
	}
}
