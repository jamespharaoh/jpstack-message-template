package wbs.clients.apn.chat.graphs.console;

import static wbs.framework.utils.etc.StringUtils.stringFormat;

import javax.inject.Inject;

import org.joda.time.LocalDate;

import wbs.console.part.AbstractPagePart;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.utils.TimeFormatter;
import static wbs.framework.utils.etc.OptionalUtils.isPresent;

@PrototypeComponent ("chatGraphsUsersPart")
public
class ChatGraphsUsersPart
	extends AbstractPagePart {

	@Inject
	TimeFormatter timeFormatter;

	@Override
	public
	void renderHtmlBodyContent () {

		LocalDate date;
		String dateString;

		if (
			isPresent (
				requestContext.parameter (
					"date"))
		) {

			dateString =
				requestContext.parameterRequired (
					"date");

			try {

				date =
					timeFormatter.dateStringToLocalDateRequired (
						dateString);

			} catch (IllegalArgumentException exception) {

				date = null;

			}

		} else {

			date =
				LocalDate.now ();

			dateString =
				timeFormatter.dateString (
					date);

		}

		printFormat (
			"<form",
			" action=\"%h\"",
			requestContext.resolveLocalUrl (
				"/chat.graphs.users"),
			" method=\"get\"",
			">\n");

		printFormat (
			"<p>Date<br>\n",

			"<input",
			" type=\"text\"",
			" name=\"date\"",
			" value=\"%h\"",
			dateString,
			">\n",

			"<input",
			" type=\"submit\"",
			" value=\"ok\"",
			"></p>");

		printFormat (
			"</form>\n");

		if (date != null) {

			printFormat (
				"<p class=\"links\">\n",

				"<a href=\"%h\">Prev week</a>\n",
				stringFormat (
					"?date=%u",
					timeFormatter.dateString (
						date.minusWeeks (1))),

				"<a href=\"%h\">Prev day</a>\n",
				stringFormat (
					"?date=%h",
					timeFormatter.dateString (
						date.minusDays (1))),

				"<a href=\"%h\">Next day</a>\n",
				stringFormat (
					"?date=%u",
					timeFormatter.dateString (
						date.plusDays (1))),

				"<a href=\"%h\">Next week</a>",
				stringFormat (
					"?date=%u",
					timeFormatter.dateString (
						date.plusWeeks (1))),

				"</p>\n");

			printFormat (
				"<p><img",
				" style=\"graph\"",
				" src=\"%h\"",
				requestContext.resolveLocalUrl (
					stringFormat (
						"/chat.graphs.usersImage",
						"?date=%u",
						dateString)),
				"></p>\n");

		}

	}

}
