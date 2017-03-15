package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.lazyj.Lazy;

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

public class FormattedFilePropertiesProvider extends FilePropertiesProvider {
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
							Arrays.asList(new String[] {
									LAST_PLAYED,
									LAST_SKIPPED,
									DATE_CREATED,
									DATE_IMPORTED,
									DATE_MODIFIED}))));
	
	public FormattedFilePropertiesProvider(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerProvider) {
		super(connectionProvider, filePropertiesContainerProvider);
	}

	@Override
	public IPromise<Map<String, String>> promiseFileProperties(int fileKey) {
		return
			super
				.promiseFileProperties(fileKey)
				.then(FormattedFilePropertiesProvider::buildFormattedReadonlyProperties);
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
			final DateTime dateTime = new DateTime((long)(Double.valueOf(value) * 1000));
			return dateTime.toString(dateTimeFormatter.getObject());
		}
		
		if (DATE.equals(name)) {
			String daysValue = value;
			final int periodPos = daysValue.indexOf('.');
			if (periodPos > -1)
				daysValue = daysValue.substring(0, periodPos);
			
			final DateTime returnDate = excelEpoch.getObject().plusDays(Integer.parseInt(daysValue));
			
			return returnDate.toString(returnDate.getMonthOfYear() == 1 && returnDate.getDayOfMonth() == 1 ? yearFormatter.getObject() : dateFormatter.getObject());
		}
		
		if (FILE_SIZE.equals(name)) {
			final double filesizeBytes = Math.ceil(Long.valueOf(value).doubleValue() / 1024 / 1024 * 100) / 100;
			return String.valueOf(filesizeBytes) + " MB";
		}
		
		if (DURATION.equals(name)) {
			return Duration.standardSeconds(Double.valueOf(value).longValue()).toPeriod().toString(minutesAndSecondsFormatter.getObject());
		}
		
		return value;
	}
}
