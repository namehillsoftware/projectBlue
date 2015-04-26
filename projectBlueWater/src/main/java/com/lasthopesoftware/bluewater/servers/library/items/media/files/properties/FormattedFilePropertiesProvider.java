package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties;

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
	private static final DateTimeFormatter mYearFormatter = new DateTimeFormatterBuilder().appendYear(4, 4).toFormatter();
	
	private static final DateTimeFormatterBuilder mDateFormatterBuilder = new DateTimeFormatterBuilder()
																	.appendMonthOfYear(1)
																	.appendLiteral('/')
																	.appendDayOfMonth(1)
																	.appendLiteral('/')
																	.append(mYearFormatter);
	
	private static final DateTimeFormatter mDateFormatter = mDateFormatterBuilder.toFormatter();
	
	private static final DateTimeFormatter mDateTimeFormatter = mDateFormatterBuilder
																	.appendLiteral(" at ")
																	.appendClockhourOfHalfday(1)
																	.appendLiteral(':')
																	.appendMinuteOfHour(2)
																	.appendLiteral(' ')
																	.appendHalfdayOfDayText()
																	.toFormatter();
	
	private static final PeriodFormatter mMinutesAndSecondsFormatter = new PeriodFormatterBuilder()
													    .appendMinutes()
													    .appendSeparator(":")
													    .minimumPrintedDigits(2)
													    .maximumParsedDigits(2)
													    .appendSeconds()
													    .toFormatter();
	
	private static final DateTime mExcelEpoch = new DateTime(1899, 12, 30, 0, 0);
	
	public FormattedFilePropertiesProvider(int fileKey) {
		super(fileKey);
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
	
	private static final SortedMap<String, String> buildFormattedReadonlyProperties(final SortedMap<String, String> unformattedProperties) {
		final SortedMap<String, String> formattedProperties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		for (Entry<String, String> property : unformattedProperties.entrySet())
			formattedProperties.put(property.getKey(), getFormattedValue(property.getKey(), property.getValue()));
		
		return Collections.unmodifiableSortedMap(formattedProperties);
	}
	
	private static final String getFormattedValue(final String name, final String value) {
		if (value == null || value.isEmpty()) return "";
		
		if (DATE_TIME_PROPERTIES.contains(name)) {
			final DateTime dateTime = new DateTime(Double.valueOf(value).longValue() * 1000);
			return dateTime.toString(mDateTimeFormatter);
		}
		
		if (DATE.equals(name)) {
			String daysValue = value;
			final int periodPos = daysValue.indexOf('.');
			if (periodPos > -1)
				daysValue = daysValue.substring(0, periodPos);
			
			final DateTime returnDate = mExcelEpoch.plusDays(Integer.parseInt(daysValue));
			
			return returnDate.toString(returnDate.getMonthOfYear() == 1 && returnDate.getDayOfMonth() == 1 ? mYearFormatter : mDateFormatter);
		}
		
		if (FILE_SIZE.equals(name)) {
			final double filesizeBytes = Math.ceil(Long.valueOf(value).doubleValue() / 1024 / 1024 * 100) / 100;
			return String.valueOf(filesizeBytes) + " MB";
		}
		
		if (DURATION.equals(name)) {
			return Duration.standardSeconds(Double.valueOf(value).longValue()).toPeriod().toString(mMinutesAndSecondsFormatter);
		}
		
		return value;
	}
	
	private static final Set<String> DATE_TIME_PROPERTIES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new String[] { LAST_PLAYED, LAST_SKIPPED, DATE_CREATED, DATE_IMPORTED, DATE_MODIFIED })));
	
}
