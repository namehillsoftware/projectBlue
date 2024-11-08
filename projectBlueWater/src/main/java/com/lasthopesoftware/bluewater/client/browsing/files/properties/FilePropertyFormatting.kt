package com.lasthopesoftware.bluewater.client.browsing.files.properties

import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.PeriodFormatterBuilder
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import kotlin.math.ceil

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
		KnownFileProperties.LastPlayed,
		KnownFileProperties.LastPlayedAlbum,
		KnownFileProperties.LastLyricsLookup,
		KnownFileProperties.LastSkipped,
		KnownFileProperties.DateCreated,
		KnownFileProperties.DateImported,
		KnownFileProperties.DateModified,
		KnownFileProperties.DateTagged,
		KnownFileProperties.DateFirstRated,
		KnownFileProperties.DateLastRated,
		KnownFileProperties.DateLastOpened
	)
}

private val cleaner by lazy {
	Cleaner(Safelist.none())
}

fun FileProperty.getFormattedValue(): String {
	if (value.isEmpty()) return ""

	return if (dateTimeProperties.contains(name)) {
		val dateTime = DateTime((value.toDouble() * 1000).toLong())
		dateTime.toString(dateTimeFormatter)
	} else when (name) {
		KnownFileProperties.Date -> {
			var daysValue = value
			val periodPos = daysValue.indexOf('.')
			if (periodPos > -1) daysValue = daysValue.substring(0, periodPos)

			val returnDate = excelEpoch.plusDays(daysValue.toInt())
			returnDate.toString(
				if (returnDate.monthOfYear == 1 && returnDate.dayOfMonth == 1) yearFormatter
				else dateFormatter)
		}
		KnownFileProperties.FileSize -> {
			val fileSizeBytes = ceil(value.toLong().toDouble() / 1024 / 1024 * 100) / 100
			"$fileSizeBytes MB"
		}
		KnownFileProperties.Duration -> {
			Duration.standardSeconds(value.toDouble().toLong()).toPeriod().toString(minutesAndSecondsFormatter)
		}
		else -> {
			val cleanestDocument = cleaner.clean(Jsoup.parse(Jsoup.parse(value).wholeText()))

			val documentText = StringUtil.borrowBuilder()
			try {
				for (node in cleanestDocument.body().textNodes()) {
					val text = node.wholeText
					if (text.isNotBlank())
						documentText.append(text)
				}

				documentText.toString()
			} finally {
				StringUtil.releaseBuilder(documentText)
			}
		}
	}
}

