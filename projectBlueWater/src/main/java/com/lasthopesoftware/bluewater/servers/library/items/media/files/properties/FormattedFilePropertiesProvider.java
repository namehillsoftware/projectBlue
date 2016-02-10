package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.vedsoft.lazyj.Lazy;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class FormattedFilePropertiesProvider extends FilePropertiesProvider {
	private static final Lazy<DateTimeFormatter> yearFormatter = new Lazy<DateTimeFormatter>() {
		@Override
		protected DateTimeFormatter initialize() {
			return new DateTimeFormatterBuilder().appendYear(4, 4).toFormatter();
		}
	};
	
	private static final Lazy<DateTimeFormatterBuilder> dateFormatterBuilder = new Lazy<DateTimeFormatterBuilder>() {
		@Override
		protected DateTimeFormatterBuilder initialize() {
			return new DateTimeFormatterBuilder()
					.appendMonthOfYear(1)
					.appendLiteral('/')
					.appendDayOfMonth(1)
					.appendLiteral('/')
					.append(yearFormatter.getObject());
		}
	};
	
	private static final Lazy<DateTimeFormatter> dateFormatter = new Lazy<DateTimeFormatter>() {
		@Override
		protected DateTimeFormatter initialize() {
			return dateFormatterBuilder.getObject().toFormatter();
		}
	};
	
	private static final Lazy<DateTimeFormatter> dateTimeFormatter = new Lazy<DateTimeFormatter>() {
		@Override
		protected DateTimeFormatter initialize() {
			return dateFormatterBuilder.getObject()
					.appendLiteral(" at ")
					.appendClockhourOfHalfday(1)
					.appendLiteral(':')
					.appendMinuteOfHour(2)
					.appendLiteral(' ')
					.appendHalfdayOfDayText()
					.toFormatter();
		}
	};
	
	private static final Lazy<PeriodFormatter> minutesAndSecondsFormatter = new Lazy<PeriodFormatter>() {
		@Override
		protected PeriodFormatter initialize() {
			return new PeriodFormatterBuilder()
					.appendMinutes()
					.appendSeparator(":")
					.minimumPrintedDigits(2)
					.maximumParsedDigits(2)
					.appendSeconds()
					.toFormatter();
		}
	};
	
	private static final Lazy<DateTime> excelEpoch = new Lazy<DateTime>() {

		@Override
		protected DateTime initialize() {
			return new DateTime(1899, 12, 30, 0, 0);
		}
	};

	private static final Lazy<Set<String>> dateTimeProperties = new Lazy<Set<String>>() {

		@Override
		protected Set<String> initialize() {
			return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(new String[] { LAST_PLAYED, LAST_SKIPPED, DATE_CREATED, DATE_IMPORTED, DATE_MODIFIED })));
		}
	};
	
	public FormattedFilePropertiesProvider(ConnectionProvider connectionProvider, int fileKey) {
		super(connectionProvider, fileKey);
	}

	@Override
	public String getProperty(String name) throws IOException {
		return getFormattedValue(name, super.getProperty(name));
	}
	
	@Override
	public String getRefreshedProperty(String name) throws IOException {
		return getFormattedValue(name, super.getRefreshedProperty(name));
	}
	
	@Override
	public SortedMap<String, String> getProperties() throws IOException {
		return buildFormattedReadonlyProperties(super.getProperties());
	}
	
	@Override
	public SortedMap<String, String> getRefreshedProperties() throws IOException {
		return buildFormattedReadonlyProperties(super.getRefreshedProperties());
	}
	
	/* Formatted properties helpers */
	
	private static SortedMap<String, String> buildFormattedReadonlyProperties(final SortedMap<String, String> unformattedProperties) {
		final SortedMap<String, String> formattedProperties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		for (Entry<String, String> property : unformattedProperties.entrySet())
			formattedProperties.put(property.getKey(), getFormattedValue(property.getKey(), property.getValue()));
		
		return Collections.unmodifiableSortedMap(formattedProperties);
	}
	
	private static String getFormattedValue(final String name, final String value) {
		if (value == null || value.isEmpty()) return "";
		
		if (dateTimeProperties.getObject().contains(name)) {
			final DateTime dateTime = new DateTime(Double.valueOf(value).longValue() * 1000);
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
