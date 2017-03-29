package wbs.apn.chat.user.admin.console;

import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import lombok.NonNull;

import wbs.console.action.ConsoleAction;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.logging.TaskLogger;

import wbs.platform.event.logic.EventLogic;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.apn.chat.contact.model.ChatMessageMethod;
import wbs.apn.chat.core.logic.ChatMiscLogic;
import wbs.apn.chat.user.core.console.ChatUserConsoleHelper;
import wbs.apn.chat.user.core.model.ChatUserRec;
import wbs.apn.chat.user.core.model.ChatUserType;
import wbs.web.responder.Responder;

@PrototypeComponent ("chatUserAdminOnlineAction")
public
class ChatUserAdminOnlineAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	ChatMiscLogic chatMiscLogic;

	@SingletonDependency
	ChatUserConsoleHelper chatUserHelper;

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventLogic eventLogic;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// details

	@Override
	public
	Responder backupResponder () {
		return responder ("chatUserAdminOnlineResponder");
	}

	// implementation

	@Override
	public
	Responder goReal (
			@NonNull TaskLogger taskLogger) {

		if (! requestContext.canContext ("chat.userAdmin")) {
			requestContext.addError ("Access denied");
			return null;
		}

		try (

			Transaction transaction =
				database.beginReadWrite (
					"ChatUserAdminOnlineAction.goReal ()",
					this);

		) {

			ChatUserRec chatUser =
				chatUserHelper.findFromContextRequired ();

			String userType =
				capitalise (
					chatUser.getType ().name ());

			if (
				optionalIsPresent (
					requestContext.parameter (
						"online"))
			) {

				if (chatUser.getOnline ()) {

					requestContext.addNotice (
						stringFormat (
							"%s already online",
							userType));

					return null;

				}

				if (chatUser.getDeliveryMethod () == ChatMessageMethod.iphone) {

					requestContext.addWarning (
						"Can't change online status for iphone users");

					return null;

				}

				if (chatUser.getType () == ChatUserType.monitor) {

					chatUser

						.setOnline (
							true);

					eventLogic.createEvent (
						taskLogger,
						"chat_user_online",
						userConsoleLogic.userRequired (),
						chatUser);

					transaction.commit ();

					requestContext.addNotice (
						"monitor brought online");

					return null;

				}

				/*
				if (chatUser.getFirstJoin () == null) {

					requestContext.addError (
						"user must complete signup process before joining");

					return null;

				}
				*/

				chatUser

					.setLastAction (
						transaction.now ());

				chatMiscLogic.userJoin (
					taskLogger,
					chatUser,
					true,
					null,
					chatUser.getDeliveryMethod ());

				eventLogic.createEvent (
					taskLogger,
					"chat_user_online",
					userConsoleLogic.userRequired (),
					chatUser);

				transaction.commit ();

				requestContext.addNotice (
					"user brought online");

				return null;

			}

			if (
				optionalIsPresent (
					requestContext.parameter (
						"offline"))
			) {

				if (! chatUser.getOnline ()) {

					requestContext.addNotice (
						stringFormat (
							"%s already offline",
							userType));

					return null;

				}

				if (chatUser.getType () == ChatUserType.monitor) {

					chatUser

						.setOnline (
							false);

				} else {

					chatMiscLogic.userLogoffWithMessage (
						taskLogger,
						chatUser,
						null,
						false);

				}

				eventLogic.createEvent (
					taskLogger,
					"chat_user_offline",
					userConsoleLogic.userRequired (),
					chatUser);

				transaction.commit ();

				requestContext.addNotice (
					stringFormat (
						"%s taken offline",
						userType));

				return null;

			}

			throw new RuntimeException ();

		}

	}

}
