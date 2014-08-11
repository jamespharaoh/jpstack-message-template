package wbs.apn.chat.user.core.console;

import static wbs.framework.utils.etc.Misc.dateToInstant;
import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import wbs.apn.chat.core.logic.ChatLogicHooks;
import wbs.apn.chat.core.logic.ChatLogicHooks.ChatUserCharge;
import wbs.apn.chat.core.logic.ChatMiscLogic;
import wbs.apn.chat.scheme.model.ChatSchemeChargesRec;
import wbs.apn.chat.user.core.model.ChatUserRec;
import wbs.apn.chat.user.core.model.ChatUserType;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.utils.cal.CalDate;
import wbs.platform.console.helper.ConsoleObjectManager;
import wbs.platform.console.misc.TimeFormatter;
import wbs.platform.console.part.AbstractPagePart;
import wbs.platform.currency.logic.CurrencyLogic;
import wbs.platform.media.console.MediaConsoleLogic;
import wbs.sms.gazetteer.logic.GazetteerLogic;

@PrototypeComponent ("chatUserSummaryPart")
public
class ChatUserSummaryPart
	extends AbstractPagePart {

	// dependencies

	@Inject
	ChatLogicHooks chatHooks;

	@Inject
	ChatMiscLogic chatLogic;

	@Inject
	ChatUserConsoleHelper chatUserHelper;

	@Inject
	CurrencyLogic currencyLogic;

	@Inject
	ConsoleObjectManager objectManager;

	@Inject
	GazetteerLogic gazetteerLogic;

	@Inject
	MediaConsoleLogic mediaConsoleLogic;

	@Inject
	TimeFormatter timeFormatter;

	// state

	ChatUserRec chatUser;

	List<ChatLogicHooks.ChatUserCharge>
		internalChatUserCharges,
		externalChatUserCharges;

	// implementation

	@Override
	public
	void prepare () {

		chatUser =
			chatUserHelper.find (
				requestContext.stuffInt ("chatUserId"));

		internalChatUserCharges =
			new ArrayList<ChatLogicHooks.ChatUserCharge> ();

		externalChatUserCharges =
			new ArrayList<ChatLogicHooks.ChatUserCharge> ();

		chatHooks.collectChatUserCharges (
			chatUser,
			internalChatUserCharges,
			externalChatUserCharges);

		Comparator<ChatLogicHooks.ChatUserCharge> comparator =
			new Comparator<ChatLogicHooks.ChatUserCharge> () {

			@Override
			public int compare (
					ChatUserCharge left,
					ChatUserCharge right) {

				return left.name.compareTo (right.name);

			}

		};

		Collections.sort (
			internalChatUserCharges,
			comparator);

		Collections.sort (
			externalChatUserCharges,
			comparator);
	}

	@Override
	public
	void goBodyStuff () {

		boolean isUser =
			chatUser.getType () == ChatUserType.user;

		printFormat (
			"<table class=\"details\">\n",

			"<tr> <th>Id</th> <td>%h</td> </tr>\n",
			chatUser.getId (),

			"<tr> <th>Code</th> <td>%h</td> </tr>\n",
			chatUser.getCode (),

			"<tr> <th>Type</th> <td>%h</td> </tr>\n",
			ifNull (chatUser.getType ()),

			"<tr> <th>Gender</th> <td>%h</td> </tr>\n",
			ifNull (chatUser.getGender (), "-"),

			"<tr> <th>Orient</th> <td>%h</td> </tr>\n",
			ifNull (chatUser.getOrient (), "-"));

		printFormat (
			"<tr>\n",
			"<th>Date of birth</th>\n",

			"<td>%h</td>\n",
			ifNull (
				CalDate.forLocalDate (
					chatUser.getDob ()),
					"-"),

			"</tr>\n");

		printFormat (
			"<tr> <th>Name</th> <td>%h</td> </tr>\n",
			ifNull (chatUser.getName (), "-"),

			"<tr> <th>Info</th> <td>%h</td> </tr>\n",
			chatUser.getInfoText () != null
				? chatUser.getInfoText ().getText ()
				: "-");

		if (! chatUser.getChatUserImageList ().isEmpty ()) {

			printFormat (
				"<tr>\n",
				"<th>Pic</th>\n",
				"<td>%s</td>\n",
				mediaConsoleLogic.mediaThumb100 (
					chatUser.getChatUserImageList ().get (0).getMedia ()),
				"</tr>\n");

		} else {

			printFormat (
				"<tr>\n",
				"<th>Pic</th>\n",
				"<td>-</td>\n",
				"</tr>\n");

		}

		printFormat (
			"<tr>\n",
			"<th>Location</th>\n",
			"<td>%s</td>\n",
			chatUser.getLocLongLat () != null
				? gazetteerLogic.findNearestCanonicalEntry (
						chatUser.getChat ().getGazetteer (),
						chatUser.getLocLongLat ()
					).getName ()
				: "-",
			"</tr>\n");

		printFormat (
			"<tr> <th>Online</th> <td>%h</td> </tr>\n",
				chatUser.getOnline() ? "yes" : "no");

		if (isUser) {

			printFormat (
				"<tr>\n",

				"<th>First join</th>\n",

				"<td>%h</td>\n",
				chatUser.getFirstJoin () != null
					? timeFormatter.instantToTimestampString (
						dateToInstant (chatUser.getFirstJoin ()))
					: "-",

				"</tr>\n");

			printFormat (
				"<tr>\n",

				"<th>Last join</th>\n",

				"<td>%h</td>\n",
				chatUser.getLastJoin () != null
					? timeFormatter.instantToTimestampString (
						dateToInstant (chatUser.getLastJoin ()))
					: "-",

				"</tr>\n");

		}

		if (isUser) {

			ChatSchemeChargesRec charges =
				chatUser.getChatScheme () != null ?
					chatUser.getChatScheme ().getCharges ()
					: null;

			printFormat (
				"<tr class=\"sep\">\n");

			printFormat (
				"<tr> <th>Number</th> %s </tr>\n",
				objectManager.tdForObject (
					chatUser.getOldNumber (),
					null,
					true,
					true));

			printFormat (
				"<tr> <th>Scheme</th> %s </tr>\n",
				objectManager.tdForObject (
					chatUser.getChatScheme (),
					chatUser.getChat (),
					true,
					true));

			printFormat (
				"<tr> <th>Affiliate</th> %s </tr>\n",
				objectManager.tdForObject (
					chatUser.getChatAffiliate (),
					chatUser.getChatScheme (),
					true,
					true));

			// "<tr> <th>Block all</th> <td>%h</td> </tr>\n",
			// chatUser.getBlockAll ()? "yes" : "no",
			// "<tr> <th>Age confirmed</th> <td>%h</td> </tr>\n",
			// chatUser.getAgeChecked ()? "yes" : "no",

			printFormat (
				"<tr> <th>Barred</th> <td>%h</td> </tr>\n",
				chatUser.getBarred () ? "yes" : "no");

			printFormat (
				"<tr> <th>Adult verified</th> <td>%h</td> </tr>\n",
				chatUser.getAdultVerified () ? "yes" : "no");

			// "<tr> <th>Rejection count</th> <td>%h</td> </tr>\n",
			// chatUser.getRejectionCount (),

			printFormat (
				"<tr class=\"sep\">\n");

			printFormat (
				"<tr> <th>User messages</th> <td>%s (%h)</td> </tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getUserMessageCharge ()),
				chatUser.getUserMessageCount ());

			printFormat (
				"<tr> <th>Monitor messages</th> <td>%s (%h)</td> </tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getMonitorMessageCharge ()),
				chatUser.getMonitorMessageCount ());

			printFormat (
				"<tr> <th>Text profile</th> <td>%s (%h)</td> </tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getTextProfileCharge ()),
				chatUser.getTextProfileCount ());

			printFormat (
				"<tr>\n",
				"<th>Image profile</th>\n",
				"<td>%s (%h)</td>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getImageProfileCharge ()),
				chatUser.getImageProfileCount (),
				"</tr>\n");

			printFormat (
				"<tr> <th>Video profile</th> <td>%s (%h)</td> </tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getVideoProfileCharge ()),
				chatUser.getVideoProfileCount ());

			printFormat (
				"<tr>\n",
				"<th>Received message</th>\n",

				"<td>%s (%h)</td>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getReceivedMessageCharge ()),
				chatUser.getReceivedMessageCount (),

				"</tr>\n");

			for (ChatLogicHooks.ChatUserCharge chatUserCharge
					: internalChatUserCharges) {

				printFormat (
					"<tr>\n",
					"<th>%h</th>\n",

					"<td>%s (%h)</td>\n",
					chatUserCharge.name,
					currencyLogic.formatHtml (
						chatUser.getChat ().getCurrency (),
						chatUserCharge.charge),
					chatUserCharge.count,

					"</tr>\n");

			}

			printFormat (
				"<tr>\n",
				"<th>Total spent</th>\n",

				"<td>%s</td>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getValueSinceEver ()),

				"</tr>\n");

			printFormat (
				"<tr class=\"sep\">\n");

			int total =
				chatUser.getValueSinceEver ();

			for (ChatLogicHooks.ChatUserCharge chatUserCharge
					: externalChatUserCharges) {

				printFormat (
					"<tr> <th>%h</th> <td>%s (%h)</td> </tr>\n",
					chatUserCharge.name,
					currencyLogic.formatHtml (
						chatUser.getChat ().getCurrency (),
						chatUserCharge.charge),
					chatUserCharge.count);

				total += chatUserCharge.charge;
			}

			printFormat (
				"<tr> <th>Grand total</th> <td>%s</td> </tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					total));

			printFormat (
				"<tr class=\"sep\">\n");

			printFormat (
				"<tr> <th>Credit</th> <td>%s</td> </tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getCredit ()));

			printFormat (
				"<tr> <th>Credit mode</th> <td>%h</td> </tr>\n",
				chatUser.getCreditMode ());

			// "<tr> <th>Credit pending</th> <td>%s</td> </tr>\n",
			// ChatConsoleStuff.credit (chatUser.getCreditPending
			// ()),

			printFormat (
				"<tr> <th>Credit pending strict</th> <td>%s</td> </tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getCreditPendingStrict ()));

			printFormat (
				"<tr> <th>Credit success</th> <td>%s</td> </tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getCreditSuccess ()));

			// "<tr> <th>Credit failed no retry</th> <td>%s</td>
			// </tr>\n",
			// ChatConsoleStuff.credit (chatUser.getCreditFailed
			// ()),

			printFormat (
				"<tr> <th>Credit awaiting retry</th> <td>%s</td> </tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getCreditRevoked ()));

			// "<tr> <th>Credit already retried</th> <td>%s</td>
			// </tr>\n",
			// ChatConsoleStuff.credit (chatUser.getCreditRetried
			// ()),

			// "<tr> <th>Credit no reports</th> <td>%s</td>
			// </tr>\n",
			// ChatConsoleStuff.credit (chatUser.getCreditSent ()),

			printFormat (
				"<tr> <th>Free usage</th> <td>%s</td> </tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getCreditAdded ()));

			printFormat (
				"<tr> <th>Credit bought/given</th> <td>%s</td> </tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getCreditBought ()));

			printFormat (
				"<tr>\n",
				"<th>Credit limit</th>\n",

				"<td>%s</td>\n",
				charges != null
					&& charges.getCreditLimit ()
						< chatUser.getCreditLimit ()

					? stringFormat (
						"%s (%s)",
						currencyLogic.formatHtml (
							chatUser.getChat ().getCurrency (),
							charges.getCreditLimit ()),
						currencyLogic.formatHtml (
							chatUser.getChat ().getCurrency (),
							chatUser.getCreditLimit ()))

					: currencyLogic.formatHtml (
						chatUser.getChat ().getCurrency (),
						chatUser.getCreditLimit ()),

				"</tr>\n");

			printFormat (
				"<tr class=\"sep\">\n");

			/*
			pf ("<tr> <th>Credit owed</th> <td>%s</td> </tr>\n",
				ChatConsoleStuff.credit (
					chatUser.getCreditPendingStrict ()
						+ chatUser.getCreditRevoked ()
						- chatUser.getCredit ()));

			pf ("<tr class=\"sep\">\n");
			*/

			printFormat (
				"<tr> <th>Daily Billed</th> <td>%s</td> <tr>\n",
				currencyLogic.formatHtml (
					chatUser.getChat ().getCurrency (),
					chatUser.getDailyBilledAmount ()));

			printFormat (
				"<tr>\n",

				"<th>Last billing started at</th>\n",

				"<td>%s</td>\n",
				chatUser.getCreditDailyDate () != null
					? timeFormatter.instantToTimestampString (
						dateToInstant (
							chatUser.getCreditDailyDate ().toDate ()))
					: "-",

				"</tr>\n");

		}

		printFormat (
			"</table>\n");

	}

}