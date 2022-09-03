package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.PeriodFormatterBuilder
import kotlin.math.ceil

class FormattedScopedFilePropertiesProvider(private val inner:  ProvideScopedFileProperties)
	: ProvideScopedFileProperties {

	companion object {
		private val yearFormatter by lazy { DateTimeFormatterBuilder().appendYear(4, 4).toFormatter() }

		private val dateFormatterBuilder by lazy {
			DateTimeFormatterBuilder()
				.appendMonthOfYear(1)
				.appendLiteral('/')
				.appendDayOfMonth(1)
				.appendLiteral('/')
				.append(yearFormatter)
		}

		private val dateFormatter by lazy { dateFormatterBuilder.toFormatter() }

		private val dateTimeFormatter by lazy {
			dateFormatterBuilder
				.appendLiteral(" at ")
				.appendClockhourOfHalfday(1)
				.appendLiteral(':')
				.appendMinuteOfHour(2)
				.appendLiteral(' ')
				.appendHalfdayOfDayText()
				.toFormatter()
		}

		private val minutesAndSecondsFormatter by lazy {
			PeriodFormatterBuilder()
				.appendMinutes()
				.appendSeparator(":")
				.minimumPrintedDigits(2)
				.maximumParsedDigits(2)
				.appendSeconds()
				.toFormatter()
		}

		private val excelEpoch by lazy { DateTime(1899, 12, 30, 0, 0) }

		private val dateTimeProperties by lazy {
			setOf(
				KnownFileProperties.LAST_PLAYED,
				KnownFileProperties.LAST_PLAYED_ALBUM,
				KnownFileProperties.LAST_LYRICS_LOOKUP,
				KnownFileProperties.LAST_SKIPPED,
				KnownFileProperties.DATE_CREATED,
				KnownFileProperties.DATE_IMPORTED,
				KnownFileProperties.DATE_MODIFIED,
				KnownFileProperties.DATE_TAGGED,
				KnownFileProperties.DATE_FIRST_RATED,
				KnownFileProperties.DATE_LAST_OPENED)
		}

		private val defaultProperties by lazy {
			mapOf(
				Pair(KnownFileProperties.RATING, "0"),
				Pair(KnownFileProperties.NUMBER_PLAYS, "0"),
			)
		}

		/* Formatted properties helpers */
		private fun buildFormattedReadonlyProperties(unformattedProperties: Map<String, String>): Map<String, String> {
			val formattedProperties = HashMap<String, String>(unformattedProperties.size)
			formattedProperties.putAll(defaultProperties)
			for ((key, value) in unformattedProperties)
				formattedProperties[key] = getFormattedValue(key, value)

			return formattedProperties
		}

		private fun getFormattedValue(name: String, value: String?): String {
			if (value.isNullOrEmpty()) return ""

			return if (dateTimeProperties.contains(name)) {
				val dateTime = DateTime((value.toDouble() * 1000).toLong())
				dateTime.toString(dateTimeFormatter)
			} else when (name) {
				KnownFileProperties.DATE -> {
					var daysValue = value
					val periodPos = daysValue.indexOf('.')
					if (periodPos > -1) daysValue = daysValue.substring(0, periodPos)

					val returnDate = excelEpoch.plusDays(daysValue.toInt())
					returnDate.toString(
						if (returnDate.monthOfYear == 1 && returnDate.dayOfMonth == 1) yearFormatter
						else dateFormatter)
				}
				KnownFileProperties.FILE_SIZE -> {
					val fileSizeBytes = ceil(value.toLong().toDouble() / 1024 / 1024 * 100) / 100
					"$fileSizeBytes MB"
				}
				KnownFileProperties.DURATION -> {
					Duration.standardSeconds(value.toDouble().toLong()).toPeriod().toString(minutesAndSecondsFormatter)
				}
				else -> value
			}
		}
	}

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> =
		CancellableProxyPromise { cp ->
			inner
				.promiseFileProperties(serviceFile)
				.apply(cp::doCancel)
				.then(::buildFormattedReadonlyProperties)
		}
}
