package wbs.framework.utils;

import static wbs.framework.utils.etc.Misc.in;
import static wbs.framework.utils.etc.Misc.isNotPresent;
import static wbs.framework.utils.etc.Misc.isPresent;
import static wbs.framework.utils.etc.Misc.lowercase;
import static wbs.framework.utils.etc.Misc.optionalRequired;
import static wbs.framework.utils.etc.Misc.split;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

@Accessors (fluent = true)
@Value
public
class TextualInterval {

	String sourceText;
	String genericText;
	Interval value;

	public static
	boolean validPartial (
			@NonNull String string) {

		if (
			in (
				lowercase (string),
				"today",
				"yesterday",
				"this month",
				"last month")
		) {
			return true;
		}

		for (
			Pattern pattern
				: partialPatterns
		) {

			Matcher matcher =
				pattern.matcher (
					string);

			if (matcher.matches ())
				return true;

		}

		return false;

	}

	public static
	boolean valid (
			@NonNull String string) {

		List<String> parts =
			split (
				string,
				" to ");

		if (parts.size () == 1) {

			return validPartial (
				string.trim ());

		} else if (parts.size () == 2) {

			return (

				validPartial (
					parts.get (0).trim ())

				&& validPartial (
					parts.get (1).trim ())

			);

		} else {

			return false;

		}

	}

	public static
	Optional<Pair<Interval,String>> parsePartialSymbolic (
			@NonNull DateTimeZone timezone,
			@NonNull String string,
			@NonNull Integer hourOffset) {

		DateTime now =
			DateTime.now (
				timezone);

		LocalDate today =
			now.getHourOfDay () >= hourOffset
				? now.toLocalDate ()
				: now.toLocalDate ().minusDays (1);

		YearMonth thisMonth =
			now.getHourOfDay () >= hourOffset || now.getDayOfMonth () > 1
				? new YearMonth (now)
				: new YearMonth (now).minusMonths (1);

		switch (lowercase (string)) {

		case "today":

			LocalDate tomorrow =
				today.plusDays (1);

			return Optional.of (
				Pair.of (
					new Interval (
						today.toDateTime (
							new LocalTime (hourOffset, 0),
							timezone),
						tomorrow.toDateTime (
							new LocalTime (hourOffset, 0),
							timezone)),
					today.toString (
						dateFormat)));

		case "yesterday":

			LocalDate yesterday =
				today.minusDays (1);

			return Optional.of (
				Pair.of (
					new Interval (
						yesterday.toDateTime (
							new LocalTime (hourOffset, 0),
							timezone),
						today.toDateTime (
							new LocalTime (hourOffset, 0),
							timezone)),
					yesterday.toString (
						dateFormat)));

		case "this month":

			YearMonth nextMonth =
				thisMonth.plusMonths (1);

			return Optional.of (
				Pair.of (
					new Interval (
						thisMonth.toLocalDate (1).toDateTime (
							new LocalTime (hourOffset, 0),
							timezone),
						nextMonth.toLocalDate (1).toDateTime (
							new LocalTime (hourOffset, 0),
							timezone)),
					nextMonth.toString (
						monthFormat)));

		case "last month":

			YearMonth lastMonth =
				thisMonth.minusMonths (1);

			return Optional.of (
				Pair.of (
					new Interval (
						lastMonth.toLocalDate (1).toDateTime (
							new LocalTime (hourOffset, 0),
							timezone),
						thisMonth.toLocalDate (1).toDateTime (
							new LocalTime (hourOffset, 0),
							timezone)),
					lastMonth.toString (
						monthFormat)));

		}

		return Optional.absent ();

	}

	public static
	Optional<Pair<Interval,String>> parsePartialNumeric (
			@NonNull DateTimeZone timeZone,
			@NonNull String string,
			@NonNull Integer hourOffset) {

		int fromYear = 0;
		int fromMonth = 1;
		int fromDate = 1;
		int fromHour = hourOffset;
		int fromMinute = 0;
		int fromSecond = 0;

		for (
			Pattern pattern
				: partialPatterns
		) {

			Matcher matcher =
				pattern.matcher (
					string);

			if (! matcher.matches ())
				continue;

			int groupCount =
				matcher.groupCount ();

			// work out time from

			if (groupCount >= 1) {

				fromYear =
					Integer.parseInt (
						matcher.group (1));

			}

			if (groupCount >= 2) {

				fromMonth =
					Integer.parseInt (
						matcher.group (2));

			}

			if (groupCount >= 3) {

				fromDate =
					Integer.parseInt (
						matcher.group (3));

			}

			if (groupCount >= 4) {

				fromHour =
					Integer.parseInt (
						matcher.group (4));

			}

			if (groupCount >= 5) {

				fromMinute =
					Integer.parseInt (
						matcher.group (5));

			}

			if (groupCount >= 6) {

				fromSecond =
					Integer.parseInt (
						matcher.group (6));

			}

			DateTime fromDateTime =
				new DateTime (
					fromYear,
					fromMonth,
					fromDate,
					fromHour,
					fromMinute,
					fromSecond,
					timeZone);

			// work out time to

			DateTime toDateTime;

			if (groupCount == 0) {

				toDateTime =
					fromDateTime.plusYears (
						10000);

			} else if (groupCount == 1) {

				toDateTime =
					fromDateTime.plusYears (
						1);

			} else if (groupCount == 2) {

				toDateTime =
					fromDateTime.plusMonths (
						1);

			} else if (groupCount == 3) {

				toDateTime =
					fromDateTime.plusDays (
						1);

			} else if (groupCount == 4) {

				toDateTime =
					fromDateTime.plusHours (
						1);

			} else if (groupCount == 5) {

				toDateTime =
					fromDateTime.plusMinutes (
						1);

			} else if (groupCount == 6) {

				toDateTime =
					fromDateTime.plusSeconds (
						1);

			} else {

				return Optional.absent ();

			}

			return Optional.of (
				Pair.of (
					new Interval (
						fromDateTime,
						toDateTime),
					string));

		}

		return Optional.absent ();

	}

	public static
	Optional<Pair<Interval,String>> parsePartial (
			@NonNull DateTimeZone timeZone,
			@NonNull String string,
			@NonNull Integer hourOffset) {

		Optional<Pair<Interval,String>> symbolicResult =
			parsePartialSymbolic (
				timeZone,
				string,
				hourOffset);

		if (
			isPresent (
				symbolicResult)
		) {
			return symbolicResult;
		}

		Optional<Pair<Interval,String>> numericResult =
			parsePartialNumeric (
				timeZone,
				string,
				hourOffset);

		if (
			isPresent (
				numericResult)
		) {
			return numericResult;
		}

		return Optional.absent ();

	}

	public static
	Optional<TextualInterval> parse (
			@NonNull DateTimeZone timezone,
			@NonNull String source,
			@NonNull Integer hourOffset) {

		List<String> parts =
			split (
				source,
				" to ");

		if (parts.size () == 1) {

			Optional<Pair<Interval,String>> optionalInterval =
				parsePartial (
					timezone,
					source.trim (),
					hourOffset);

			if (
				isNotPresent (
					optionalInterval)
			) {
				return Optional.absent ();
			}

			return Optional.of (
				new TextualInterval (
					source.trim (),
					optionalInterval.get ().getRight (),
					optionalInterval.get ().getLeft ()));

		} else if (parts.size () == 2) {

			Optional<Pair<Interval,String>> optionalFirstInterval =
				parsePartial (
					timezone,
					parts.get (0).trim (),
					hourOffset);

			if (
				isNotPresent (
					optionalFirstInterval)
			) {
				return Optional.absent ();
			}

			Optional<Pair<Interval,String>> optionalSecondInterval =
				parsePartial (
					timezone,
					parts.get (1).trim (),
					hourOffset);

			if (
				isNotPresent (
					optionalSecondInterval)
			) {
				return Optional.absent ();
			}

			Interval interval =
				new Interval (
					optionalFirstInterval.get ().getLeft ().getStart (),
					optionalSecondInterval.get ().getLeft ().getEnd ());

			return Optional.of (
				new TextualInterval (
					source.trim (),
					stringFormat (
						"%s to %s",
						optionalFirstInterval.get ().getRight (),
						optionalSecondInterval.get ().getRight ()),
					interval));

		} else {

			return Optional.absent ();

		}

	}

	public static
	TextualInterval parseRequired (
			@NonNull DateTimeZone timeZone,
			@NonNull String string,
			@NonNull Integer hourOffset) {

		return optionalRequired (
			parse (
				timeZone,
				string,
				hourOffset));

	}

	public static
	TextualInterval forInterval (
			@NonNull DateTimeZone timezone,
			@NonNull Interval interval) {

		// TODO make this cleverer

		String intervalString =
			intervalToString (
				timezone,
				interval);

		return new TextualInterval (
			intervalString,
			intervalString,
			interval);

	}

	public static
	String intervalToString (
			@NonNull DateTimeZone timezone,
			@NonNull Interval interval) {

		return stringFormat (
			"%s to %s",
			formatInstant (
				timezone,
				interval.getStart ()),
			formatInstant (
				timezone,
				interval.getEnd ()));

	}

	public static
	String formatInstant (
			@NonNull DateTimeZone timezone,
			@NonNull ReadableInstant instant) {

		DateTime dateTime =
			new DateTime (
				instant,
				timezone);

		return dateTime.toString (
			timestampFormat);

	}

	// accessors

	public
	Instant start () {
		return value.getStart ().toInstant ();
	}

	public
	Instant end () {
		return value.getEnd ().toInstant ();
	}

	// data

	private final static
	DateTimeFormatter timestampFormat =
		DateTimeFormat

		.forPattern (
			"yyyy-MM-dd HH:mm:ss");

	public final static
	DateTimeFormatter dateFormat =
		DateTimeFormat

		.forPattern (
			"yyyy-MM-dd");

	public final static
	DateTimeFormatter monthFormat =
		DateTimeFormat

		.forPattern (
			"yyyy-MM");

	private final static
	List<Pattern> partialPatterns =
		ImmutableList.<Pattern>of (

		Pattern.compile (
			"([0-9]{4})-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01]) " +
			"([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])"),

		Pattern.compile (
			"([0-9]{4})-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01]) " +
			"([01][0-9]|2[0-3]):([0-5][0-9])"),

		Pattern.compile (
			"([0-9]{4})-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01]) " +
			"([01][0-9]|2[0-3])"),

		Pattern.compile (
			"([0-9]{4})-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])"),

		Pattern.compile (
			"([0-9]{4})-(0?[1-9]|1[0-2])"),

		Pattern.compile (
			"([0-9]{4})"),

		Pattern.compile (
			"")

	);

}