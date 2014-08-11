package wbs.apn.chat.user.admin.console;

import javax.inject.Inject;

import wbs.apn.chat.user.core.console.ChatUserConsoleHelper;
import wbs.apn.chat.user.core.model.ChatUserRec;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.platform.console.part.AbstractPagePart;

@PrototypeComponent ("chatUserAdminOnlinePart")
public
class ChatUserAdminOnlinePart
	extends AbstractPagePart {

	@Inject
	ChatUserConsoleHelper chatUserHelper;

	ChatUserRec chatUser;

	@Override
	public
	void prepare () {

		chatUser =
			chatUserHelper.find (
				requestContext.stuffInt ("chatUserId"));
	}

	@Override
	public
	void goBodyStuff () {

		printFormat (
			"<form",
			" method=\"post\"",
			" action=\"%h\"",
			requestContext.resolveLocalUrl (
				"/chatUser.admin.online"),
			">\n");

		if (chatUser.getOnline ()) {

			printFormat (
				"<p>This user is online</p>\n");

			printFormat (
				"<p><input",
				" type=\"submit\"",
				" name=\"offline\"",
				" value=\"take offline\"",
				"></p>\n");

		} else {

			printFormat (
				"<p>This user is offline</p>\n");

			printFormat (
				"<p><input",
				" type=\"submit\"",
				" name=\"online\"",
				" value=\"bring online\"",
				"></p>\n");

		}

		printFormat (
			"</table>\n");

	}

}