package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.Lazy;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FormattedSessionFilePropertiesProvider extends SessionFilePropertiesProvider {
	private static final Lazy<DateTimeFormatter> yearFormatter = new Lazy<>(() -> new DateTimeFormatterBuilder().appendYear(4, 4).toFormatter());

	private static final Lazy<DateTimeFormatterBuilder> dateFormatterBuilder =
			new Lazy<>(() ->
					new DateTimeFormatterBuilder()
							.appendMonthOfYear(1)
							.appendLiteral('/')
							.appendDayOfMonth(1)
							.appendLiteral('/')
							.append(yearFormatter.getObject()));

	private static final Lazy<DateTimeFormatter> dateFormatter = new Lazy<>(() -> dateFormatterBuilder.getObject().toFormatter());

	private static final Lazy<DateTimeFormatter> dateTimeFormatter =
			new Lazy<>(() ->
					dateFormatterBuilder.getObject()
							.appendLiteral(" at ")
							.appendClockhourOfHalfday(1)
							.appendLiteral(':')
							.appendMinuteOfHour(2)
							.appendLiteral(' ')
							.appendHalfdayOfDayText()
							.toFormatter());

	private static final Lazy<PeriodFormatter> minutesAndSecondsFormatter =
			new Lazy<>(() ->
					new PeriodFormatterBuilder()
							.appendMinutes()
							.appendSeparator(":")
							.minimumPrintedDigits(2)
							.maximumParsedDigits(2)
							.appendSeconds()
							.toFormatter());

	private static final Lazy<DateTime> excelEpoch = new Lazy<>(() -> new DateTime(1899, 12, 30, 0, 0));

	private static final Lazy<Set<String>> dateTimeProperties =
			new Lazy<>(() -> Collections.unmodifiableSet(
					new HashSet<>(
							Arrays.asList(
								KnownFileProperties.LAST_PLAYED,
								KnownFileProperties.LAST_SKIPPED,
								KnownFileProperties.DATE_CREATED,
								KnownFileProperties.DATE_IMPORTED,
								KnownFileProperties.DATE_MODIFIED,
								KnownFileProperties.DATE_TAGGED,
								KnownFileProperties.DATE_FIRST_RATED,
								KnownFileProperties.DATE_LAST_OPENED))));

	public FormattedSessionFilePropertiesProvider(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerProvider) {
		super(connectionProvider, filePropertiesContainerProvider);
	}

	@NotNull
	@Override
	public Promise<Map<String, String>> promiseFileProperties(@NotNull ServiceFile serviceFile) {
		return
			super
				.promiseFileProperties(serviceFile)
				.then(FormattedSessionFilePropertiesProvider::buildFormattedReadonlyProperties);
	}

	/* Formatted properties helpers */

	private static Map<String, String> buildFormattedReadonlyProperties(final Map<String, String> unformattedProperties) {
		final HashMap<String, String> formattedProperties = new HashMap<>(unformattedProperties.size());

		for (Entry<String, String> property : unformattedProperties.entrySet())
			formattedProperties.put(property.getKey(), getFormattedValue(property.getKey(), property.getValue()));

		return new HashMap<>(formattedProperties);
	}

	private static String getFormattedValue(final String name, final String value) {
		if (value == null || value.isEmpty()) return "";

		if (dateTimeProperties.getObject().contains(name)) {
			final DateTime dateTime = new DateTime(Double.parseDouble(value) * 1000);
			return dateTime.toString(dateTimeFormatter.getObject());
		}

		if (KnownFileProperties.DATE.equals(name)) {
			String daysValue = value;
			final int periodPos = daysValue.indexOf('.');
			if (periodPos > -1)
				daysValue = daysValue.substring(0, periodPos);

			final DateTime returnDate = excelEpoch.getObject().plusDays(Integer.parseInt(daysValue));

			return returnDate.toString(returnDate.getMonthOfYear() == 1 && returnDate.getDayOfMonth() == 1 ? yearFormatter.getObject() : dateFormatter.getObject());
		}

		if (KnownFileProperties.FILE_SIZE.equals(name)) {
			final double fileSizeBytes = Math.ceil(Long.valueOf(value).doubleValue() / 1024 / 1024 * 100) / 100;
			return fileSizeBytes + " MB";
		}

		if (KnownFileProperties.DURATION.equals(name)) {
			return Duration.standardSeconds(Double.valueOf(value).longValue()).toPeriod().toString(minutesAndSecondsFormatter.getObject());
		}

		return value;
	}
}
