package wbs.clients.apn.chat.user.info.logic;

import static wbs.framework.utils.etc.Misc.instantToDate;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import org.joda.time.Duration;
import org.joda.time.Instant;

import wbs.clients.apn.chat.bill.logic.ChatCreditLogic;
import wbs.clients.apn.chat.contact.logic.ChatSendLogic;
import wbs.clients.apn.chat.contact.model.ChatContactObjectHelper;
import wbs.clients.apn.chat.contact.model.ChatContactRec;
import wbs.clients.apn.chat.core.model.ChatRec;
import wbs.clients.apn.chat.help.logic.ChatHelpTemplateLogic;
import wbs.clients.apn.chat.help.model.ChatHelpTemplateObjectHelper;
import wbs.clients.apn.chat.help.model.ChatHelpTemplateRec;
import wbs.clients.apn.chat.infosite.model.ChatInfoSiteObjectHelper;
import wbs.clients.apn.chat.infosite.model.ChatInfoSiteRec;
import wbs.clients.apn.chat.scheme.model.ChatSchemeRec;
import wbs.clients.apn.chat.user.core.logic.ChatUserLogic;
import wbs.clients.apn.chat.user.core.model.ChatUserObjectHelper;
import wbs.clients.apn.chat.user.core.model.ChatUserRec;
import wbs.clients.apn.chat.user.core.model.ChatUserType;
import wbs.clients.apn.chat.user.info.model.ChatUserInfoObjectHelper;
import wbs.clients.apn.chat.user.info.model.ChatUserInfoRec;
import wbs.clients.apn.chat.user.info.model.ChatUserInfoStatus;
import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.platform.affiliate.model.AffiliateRec;
import wbs.platform.exception.logic.ExceptionLogic;
import wbs.platform.exception.logic.ExceptionLogicImpl;
import wbs.platform.media.logic.MediaLogic;
import wbs.platform.media.model.MediaRec;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.service.model.ServiceObjectHelper;
import wbs.platform.service.model.ServiceRec;
import wbs.platform.text.model.TextObjectHelper;
import wbs.platform.text.model.TextRec;
import wbs.sms.command.model.CommandObjectHelper;
import wbs.sms.gsm.MessageSplitter;
import wbs.sms.locator.logic.LocatorLogic;
import wbs.sms.magicnumber.logic.MagicNumberLogic;
import wbs.sms.magicnumber.model.MagicNumberRec;
import wbs.sms.message.outbox.logic.MessageSender;

import com.google.common.base.Optional;

@Log4j
@SingletonComponent ("chatInfoLogic")
public
class ChatInfoLogicImpl
	implements ChatInfoLogic {

	@Inject
	ChatContactObjectHelper chatContactHelper;

	@Inject
	ChatCreditLogic chatCreditLogic;

	@Inject
	ChatHelpTemplateObjectHelper chatHelpTemplateHelper;

	@Inject
	ChatInfoSiteObjectHelper chatInfoSiteHelper;

	@Inject
	ChatSendLogic chatSendLogic;

	@Inject
	ChatHelpTemplateLogic chatTemplateLogic;

	@Inject
	ChatUserObjectHelper chatUserHelper;

	@Inject
	ChatUserInfoObjectHelper chatUserInfoHelper;

	@Inject
	ChatUserLogic chatUserLogic;

	@Inject
	CommandObjectHelper commandHelper;

	@Inject
	Database database;

	@Inject
	ExceptionLogic exceptionLogic;

	@Inject
	LocatorLogic locatorLogic;

	@Inject
	MagicNumberLogic magicNumberLogic;

	@Inject
	MediaLogic mediaLogic;

	@Inject
	QueueLogic queueLogic;

	@Inject
	Random random;

	@Inject
	ServiceObjectHelper serviceHelper;

	@Inject
	TextObjectHelper textHelper;

	@Inject
	Provider<MessageSender> messageSender;

	@Override
	public
	void sendUserInfo (
			ChatUserRec thisUser,
			ChatUserRec otherUser,
			Integer threadId,
			boolean asDating) {

		Date now =
			new Date ();

		ChatRec chat =
			thisUser.getChat ();

		// update chat user with last info stats and charge

		chatCreditLogic.userSpend (
			thisUser,
			0,
			0,
			1,
			0,
			0);

		thisUser.setLastInfo (now);

		// update contact record with last info stats

		ChatContactRec contact =
			chatContactHelper.findOrCreate (
				otherUser,
				thisUser);

		contact.setLastInfoTime (now);

		// work out distance

		int miles =
			(int) locatorLogic.distanceMiles (
				thisUser.getLocLongLat (),
				otherUser.getLocLongLat ());

		// construct message parts

		String userId =
			otherUser.getName () == null
				? otherUser.getCode ()
				: otherUser.getName () + " " + otherUser.getCode ();

		String distanceText = "" + miles + (miles == 1 ? " mile" : " miles");

		MessageSplitter.Templates templates =
			new MessageSplitter.Templates (
				"User " + userId + " (" + distanceText + "): {message}",
				"User " + userId + " (" + distanceText + ") {page}/{pages}: {message}",
				"User " + userId + " {page}/{pages}: {message}",
				"User " + userId + " {page}/{pages}: {message}");

		List<String> stringParts;

		try {

			stringParts =
				MessageSplitter.split (
					otherUser.getInfoText ().getText (),
					templates);

		} catch (IllegalArgumentException e) {

			log.error ("MessageSplitter.split threw exception: " + e);

			exceptionLogic.logSimple (
				"unknown",
				"chatLogic.sendUserInfo (...)",

				"MessageSplitter.split (...) threw IllegalArgumentException",
				"Error probably caused by illegal characters in user's info. Ignoring error.\n" +
				"\n" +
				"thisUser.id = " + thisUser.getId () + "\n" +
				"otherUser.id = " + otherUser.getId () + "\n" +
				"\n" +
				ExceptionLogicImpl.throwableDump (e),

				Optional.<Integer>absent (),
				false);

			return;

		}

		// send the message

		String serviceCode =
			asDating
				? "date_text"
				: "online_text";

		List<TextRec> textParts =
			new ArrayList<TextRec> ();

		for (String part : stringParts)
			textParts.add (
				textHelper.findOrCreate (part));

		chatSendLogic.sendMessageMagic (
			thisUser,
			Optional.fromNullable (threadId),
			textParts,
			commandHelper.findByCode (chat, "chat"),
			serviceHelper.findByCode (chat, serviceCode),
			otherUser.getId ());

	}

	@Override
	public
	int sendUserInfos (
			ChatUserRec thisUser,
			int numToSend,
			Integer threadId) {

		// ignore deleted users

		if (thisUser.getNumber () == null)
			return 0;

		Date now =
			new Date ();

		Date cutoffTime =
			new Date (now.getTime () - 7 * 24 * 60 * 60 * 1000);

		Collection<ChatUserRec> otherUsers =
			getNearbyOnlineUsersForInfo (
				thisUser,
				cutoffTime,
				numToSend);

		for (ChatUserRec otherUser
				: otherUsers) {

			sendUserInfo (
				thisUser,
				otherUser,
				threadId,
				false);

		}

		return otherUsers.size ();

	}

	@Override
	public
	String chatUserBlurb (
			ChatUserRec thisUser,
			ChatUserRec otherUser) {

		ChatRec chat =
			thisUser.getChat ();

		ChatSchemeRec chatScheme =
			thisUser.getChatScheme ();

		// allocate a magic number

		MagicNumberRec magicNumber =
			magicNumberLogic.allocateMagicNumber (
				chatScheme.getMagicNumberSet (),
				thisUser.getNumber (),
				commandHelper.findByCode (chat, "chat"),
				otherUser.getId ());

		StringBuilder message =
			new StringBuilder ();

		message.append (
			stringFormat (
				"Text me on: 0%s\n",
				magicNumber.getNumber ().substring (2)));

		if (otherUser.getName () != null)

			message.append (
				stringFormat (
					"Name: %s\n",
					otherUser.getName ()));

		message.append (
			stringFormat (
				"User: %s\n",
				otherUser.getCode ()));

		/*
		if (thisUser.getLocLongLat () != null
				&& otherUser.getLocLongLat () != null)
			message.append (sf ("Distance: %d miles\n", (int) LocatorLogicImpl
					.distanceMiles (thisUser.getLocLongLat (), otherUser
							.getLocLongLat ())));
		*/

		if (otherUser.getInfoText () != null) {

			message.append (
				stringFormat (
					"Info: %s\n",
					otherUser.getInfoText ().getText ()));

		}

		return message.toString ();
	}

	@Override
	public
	MediaRec chatUserBlurbMedia (
			ChatUserRec thisUser,
			ChatUserRec otherUser) {

		String message =
			chatUserBlurb (
				thisUser,
				otherUser);

		return mediaLogic.createTextMedia (
			message,
			"text/plain",
			otherUser.getCode () + ".txt");

	}

	public
	void sendUserPicsViaLink (
			ChatUserRec thisUser,
			Collection<ChatUserRec> otherUsers,
			Integer threadId,
			boolean asDating) {

		ChatRec chat =
			thisUser.getChat ();

		ChatSchemeRec chatScheme =
			thisUser.getChatScheme ();

		// create info site

		ChatInfoSiteRec chatInfoSite =
			new ChatInfoSiteRec ()
				.setChatUser (thisUser)
				.setCreateTime (Instant.now ())
				.setNumViews (0)
				.setNumExpired (0);

		// expires in 48 hours

		chatInfoSite.setExpireTime (
			Instant.now ().plus (
				Duration.standardHours (48)));

		// random 20-character token

		StringBuilder token =
			new StringBuilder (20);

		for (int i = 0; i < 10; i++)
			token.append ((char) ('a' + random.nextInt (26)));

		chatInfoSite
			.setToken (token.toString ());

		// other users

		chatInfoSite
			.getOtherChatUsers ().addAll (
				otherUsers);

		// save it

		chatInfoSiteHelper.insert (
			chatInfoSite);

		// update user charge and stats

		chatCreditLogic.userSpend (
			thisUser,
			0,
			0,
			0,
			otherUsers.size (),
			0);

		thisUser.setLastPic (new Date ());

		// flush to generate id

		database.flush ();

		// send the wap link

		/*
		wapPushUtils.wapPushSend (
			null,
			thisUser.getNumber (),
			chatScheme.getRbNumber (),
			"Click for user pics",
			sf ("http://hades.apnuk.com/api/chat/infoSite/%s/%s",
				infoSite.getId (),
				infoSite.getToken ()),
			chatScheme.getWapRouter ().getRoute (),
			serviceLogic.findOrCreateService (
				chat,
				"info_site",
				"info_site"),
			null,
			getAffiliate (thisUser),
			null,
			null,
			true,
			null,
			null,
			null);
		*/

		ChatHelpTemplateRec linkTemplate =
			chatHelpTemplateHelper.findByTypeAndCode (
				chat,
				"system",
				"info_site_link");

		String link =
			stringFormat (
				"http://txtit.com/is/%s/%s",
				chatInfoSite.getId (),
				chatInfoSite.getToken ());

		String messageText =
			linkTemplate.getText ()
				.replace ("{link}", link);

		ServiceRec infoSiteService =
			serviceHelper.findByCode (chat, "info_site");

		AffiliateRec affiliate =
			chatUserLogic.getAffiliate (thisUser);

		messageSender.get ()

			.threadId (
				threadId)

			.number (
				thisUser.getNumber ())

			.messageString (
				messageText)

			.numFrom (
				chatScheme.getRbNumber ())

			.routerResolve (
				chatScheme.getRbFreeRouter ())

			.service (
				infoSiteService)

			.affiliate (
				affiliate)

			.send ();

	}

	public
	void sendUserPicsViaMms (
			ChatUserRec thisUser,
			Collection<ChatUserRec> otherUsers,
			Integer threadId,
			boolean asDating) {

		ChatRec chat =
			thisUser.getChat ();

		ChatSchemeRec chatScheme =
			thisUser.getChatScheme ();

		List<MediaRec> medias =
			new ArrayList<MediaRec> ();

		int i = 0;

		for (ChatUserRec otherUser
				: otherUsers) {

			// add their image to the list

			if (otherUser.getMainChatUserImage () != null) {

				medias.add (
					otherUser.getMainChatUserImage ().getMedia ());

			}

			// add their blurb

			medias.add (
				chatUserBlurbMedia (
					thisUser,
					otherUser));

			i++;

		}

		// update chat user with last pic stats and charge

		chatCreditLogic.userSpend (
			thisUser,
			0,
			0,
			0,
			i,
			0);

		thisUser.setLastPic (new Date ());

		// now add a help message on the end

		ChatHelpTemplateRec chatHelpTemplate =
			chatTemplateLogic.findChatHelpTemplate (
				thisUser,
				"system",
				"photos_help_text");

		MediaRec helpTextMedia =
			mediaLogic.createTextMedia (
				chatHelpTemplate.getText (),
				"text/plain",
				"help.text");

		medias.add (helpTextMedia);

		// and send the message

		String serviceCode =
			asDating
				? "date_image"
				: "online_image";

		ServiceRec service =
			serviceHelper.findByCode (
				chat,
				serviceCode);

		AffiliateRec affiliate =
			chatUserLogic.getAffiliate (thisUser);

		messageSender.get ()

			.threadId (
				threadId)

			.number (
				thisUser.getNumber ())

			.messageString (
				"")

			.numFrom (
				chatScheme.getMmsNumber ())

			.route (
				chatScheme.getMmsRoute ())

			.service (
				service)

			.affiliate (
				affiliate)

			.subjectString (
				"User profiles")

			.medias (
				medias)

			.send ();

	}

	@Override
	public
	void sendUserPics (
			ChatUserRec thisUser,
			Collection<ChatUserRec> otherUsers,
			Integer threadId,
			boolean asDating) {

		switch (thisUser.getImageMode ()) {

		case link:

			sendUserPicsViaLink (
				thisUser,
				otherUsers,
				threadId,
				asDating);

			break;

		case mms:

			sendUserPicsViaMms (
				thisUser,
				otherUsers,
				threadId,
				asDating);

			break;

		default:

			throw new RuntimeException ("Error 10842342");

		}

		// update contact record with last pic stats

		Date now = new Date ();

		for (ChatUserRec otherUser
				: otherUsers) {

			ChatContactRec contact =
				chatContactHelper.findOrCreate (
					otherUser,
					thisUser);

			contact.setLastPicTime (now);

		}

	}

	@Override
	public
	void sendUserVideos (
			final ChatUserRec thisUser,
			Collection<ChatUserRec> otherUsers,
			Integer threadId,
			boolean asDating) {

		ChatSchemeRec chatScheme =
			thisUser.getChatScheme ();

		List<MediaRec> medias =
			new ArrayList<MediaRec> ();

		int i = 0;

		for (ChatUserRec otherUser
				: otherUsers) {

			// add their video to the list

			if (otherUser.getMainChatUserVideo () != null) {

				medias.add (
					otherUser.getMainChatUserVideo ().getMedia ());

			}

			// add their blurb

			medias.add (
				chatUserBlurbMedia (
					thisUser,
					otherUser));

			// update contact record with last videostats

			ChatContactRec contact =
				chatContactHelper.findOrCreate (
					otherUser,
					thisUser);

			contact.setLastVideoTime (
				new Date ());

			i ++;

		}

		// update chat user with last video stats and charge

		chatCreditLogic.userSpend (
			thisUser,
			0,
			0,
			0,
			0,
			i);

		thisUser.setLastPic (new Date ());

		// now add a help message on the end

		ChatHelpTemplateRec template =
			chatTemplateLogic.findChatHelpTemplate (
				thisUser,
				"system",
				"videos_help_text");

		MediaRec helpTextMedia =
			mediaLogic.createTextMedia (
				template.getText (),
				"text/plain",
				"help.text");

		medias.add (
			helpTextMedia);

		// and send the message

		String serviceCode =
			asDating
				? "date_video"
				: "online_video";

		ServiceRec service =
			serviceHelper.findByCode (
				thisUser.getChat (),
				serviceCode);

		AffiliateRec affiliate =
			chatUserLogic.getAffiliate (thisUser);

		messageSender.get ()

			.threadId (
				threadId)

			.number (
				thisUser.getNumber ())

			.messageString (
				"")

			.numFrom (
				chatScheme.getMmsNumber ())

			.route (
				chatScheme.getMmsRoute ())

			.service (
				service)

			.affiliate (
				affiliate)

			.subjectString (
				"User videos")

			.medias (
				medias)

			.send ();

	}

	@Override
	public
	int sendRequestedUserPicandOtherUserPics (
			ChatUserRec thisUser,
			ChatUserRec requestedUser,
			int numToSend,
			Integer threadId) {

		Date now =
			new Date ();

		Date cutoffTime =
			new Date (
				+ now.getTime ()
				- 24 * 60 * 60 * 1000);

		Collection<ChatUserRec> otherUsers =
			getNearbyOnlineUsersForPic (
				thisUser,
				cutoffTime,
				numToSend);

		ArrayList<ChatUserRec> list =
			new ArrayList<ChatUserRec> (otherUsers);

		list.add (0, requestedUser);

		otherUsers = list;

		sendUserPics (
			thisUser,
			otherUsers,
			threadId,
			false);

		return otherUsers.size ();

	}

	@Override
	public
	int sendUserPics (
			ChatUserRec thisUser,
			int numToSend,
			Integer threadId) {

		Date now =
			new Date ();

		Date cutoffTime =
			new Date (
				+ now.getTime ()
				- 24 * 60 * 60 * 1000);

		Collection<ChatUserRec> otherUsers =
			getNearbyOnlineUsersForPic (
				thisUser,
				cutoffTime,
				numToSend);

		if (otherUsers.size () == 0)
			return 0;

		sendUserPics (
			thisUser,
			otherUsers,
			threadId,
			false);

		return otherUsers.size ();

	}

	@Override
	public
	int sendUserVideos (
			ChatUserRec thisUser,
			int numToSend,
			Integer threadId) {

		Date now =
			new Date ();

		Date cutoffTime =
			new Date (
				+ now.getTime ()
				- 24 * 60 * 60 * 1000);

		Collection<ChatUserRec> otherUsers =
			getNearbyOnlineUsersForVideo (
				thisUser,
				cutoffTime,
				numToSend);

		if (otherUsers.size () == 0)
			return 0;

		sendUserVideos (
			thisUser,
			otherUsers,
			threadId,
			false);

		return otherUsers.size ();

	}

	/**
	 * Returns the nearest numToFind users who qualify for a pic given
	 * cutoffTime.
	 */
	@Override
	public
	Collection<ChatUserRec> getNearbyOnlineUsersForInfo (
			ChatUserRec thisUser,
			Date cutoffTime,
			int numToFind) {

		Collection<ChatUserRec> chatUsers =
			getOnlineUsersForInfo (
				thisUser,
				cutoffTime);

		return chatUserLogic.getNearestUsers (
			thisUser,
			chatUsers,
			numToFind);

	}

	/**
	 * Returns the nearest numToFind users who qualify for a pic given
	 * cutoffTime.
	 */
	@Override
	public
	Collection<ChatUserRec> getNearbyOnlineUsersForPic (
			ChatUserRec thisUser,
			Date cutoffTime,
			int numToFind) {

		Collection<ChatUserRec> chatUsers =
			getOnlineUsersForPic (
				thisUser,
				cutoffTime);

		return chatUserLogic.getNearestUsers (
			thisUser,
			chatUsers,
			numToFind);

	}

	@Override
	public
	Collection<ChatUserRec> getNearbyOnlineUsersForVideo (
			ChatUserRec thisUser,
			Date cutoffTime,
			int numToFind) {

		Collection<ChatUserRec> chatUsers =
			getOnlineUsersForVideo (
				thisUser,
				cutoffTime);

		return chatUserLogic.getNearestUsers (
			thisUser,
			chatUsers,
			numToFind);

	}

	/**
	 * Gets all online users who qualify to send us their pic. Also takes a
	 * cutoff time, anyone who we have been sent the picture of since this time
	 * won't be included.
	 *
	 * @param thisUser
	 *            the user who wants to receive pics
	 * @param cutoffTime
	 *            the cutoff time
	 * @return a collection of eligible chat users
	 */
	private
	Collection<ChatUserRec> getOnlineUsersForInfo (
			@NonNull ChatUserRec thisUser,
			@NonNull Date cutoffTime) {

		ChatRec chat =
			thisUser.getChat ();

		Collection<ChatUserRec> onlineUsers =
			chatUserHelper.findOnline (
				chat);

		List<ChatUserRec> ret =
			new ArrayList<ChatUserRec> ();

		for (ChatUserRec chatUser : onlineUsers) {

			// ignore ourselves

			if (chatUser == thisUser)
				continue;

			// ignore blocked users

			if (thisUser.getBlocked ().containsKey (chatUser.getId ()))
				continue;

			// if we aren't suitable gender/orients for each other skip it

			if (
				! chatUserLogic.compatible (
					thisUser,
					chatUser)
			) {

				continue;

			}

			// ignore users we have had an info, message or pic from recently

			ChatContactRec chatUserContact =
				thisUser.getFromContacts ().get (
					chatUser.getId ());

			if (chatUserContact != null) {

				if (chatUserContact.getLastInfoTime () != null
						&& chatUserContact.getLastInfoTime ().getTime ()
							> cutoffTime.getTime ())
					continue;

				if (chatUserContact.getLastDeliveredMessageTime () != null
						&& chatUserContact.getLastDeliveredMessageTime ().getTime ()
							> cutoffTime.getTime ())
					continue;

				if (chatUserContact.getLastPicTime () != null
						&& chatUserContact.getLastPicTime ().getTime ()
							> cutoffTime.getTime ())
					continue;

			}

			// ignore users with no info

			if (chatUser.getInfoText () == null)
				continue;

			// ignore users according to monitor cap

			if (thisUser.getMonitorCap () != null
					&& chatUser.getType () == ChatUserType.monitor
					&& (chatUser.getCode ().charAt (2) - '0')
						>= thisUser.getMonitorCap ())
				continue;

			// ignore system user

			if (chatUser == chat.getSystemChatUser ())
				continue;

			ret.add (chatUser);

		}

		return ret;

	}

	/**
	 * Gets all online users who qualify to send us their pic.
	 */
	private
	Collection<ChatUserRec> getOnlineUsersForPic (
			ChatUserRec thisUser,
			Date cutoffTime) {

		ChatRec chat =
			thisUser.getChat ();

		Collection<ChatUserRec> onlineChatUsers =
			chatUserHelper.findOnline (
				chat);

		List<ChatUserRec> selectedChatUsers =
			new ArrayList<ChatUserRec> ();

		for (ChatUserRec chatUser
				: onlineChatUsers) {

			// ignore ourselves

			if (chatUser == thisUser)
				continue;

			// ignore blocked users

			if (thisUser.getBlocked ().containsKey (
					chatUser.getId ()))
				continue;

			// if we aren't suitable gender/orients for each other skip it

			if (! chatUserLogic.compatible (
					thisUser,
					chatUser))
				continue;

			// ignore users we have had a pic from recently

			ChatContactRec chatContact =
				thisUser.getFromContacts ().get (
					chatUser.getId ());

			if (chatContact != null
					&& chatContact.getLastPicTime () != null
					&& chatContact.getLastPicTime ().getTime ()
						> cutoffTime.getTime ())
				continue;

			// ignore users with no info or main pic

			if (chatUser.getInfoText () == null)
				continue;

			if (chatUser.getMainChatUserImage () == null)
				continue;

			// ignore users according to monitor cap

			if (thisUser.getMonitorCap () != null
					&& chatUser.getType () == ChatUserType.monitor
					&& (chatUser.getCode ().charAt (2) - '0') < thisUser
							.getMonitorCap ())
				continue;

			// ignore system user

			if (chatUser == chat.getSystemChatUser ())
				continue;

			selectedChatUsers.add (chatUser);

		}

		return selectedChatUsers;

	}

	private
	Collection<ChatUserRec> getOnlineUsersForVideo (
		ChatUserRec thisUser,
		Date cutoffTime) {

		ChatRec chat =
			thisUser.getChat ();

		Collection<ChatUserRec> onlineUsers =
			chatUserHelper.findOnline (
				chat);

		List<ChatUserRec> selectedChatUsers =
			new ArrayList<ChatUserRec> ();

		for (ChatUserRec chatUser
				: onlineUsers) {

			// ignore ourselves

			if (chatUser == thisUser)
				continue;

			// ignore blocked users

			if (thisUser.getBlocked ().containsKey (
					chatUser.getId ()))
				continue;

			// if we aren't suitable gender/orients for each other skip it

			if (! chatUserLogic.compatible (
					thisUser,
					chatUser))
				continue;

			// ignore users we have had a videofrom recently

			ChatContactRec chatContact =
				thisUser.getFromContacts ().get (
					chatUser.getId ());

			if (chatContact != null
					&& chatContact.getLastVideoTime () != null
					&& chatContact.getLastVideoTime ().getTime ()
						> cutoffTime.getTime ())
				continue;

			// ignore users with no info or main video

			if (chatUser.getInfoText () == null)
				continue;

			if (chatUser.getMainChatUserVideo () == null)
				continue;

			// ignore users according to monitor cap

			if (thisUser.getMonitorCap () != null
					&& chatUser.getType () == ChatUserType.monitor
					&& (chatUser.getCode ().charAt (2) - '0')
						< thisUser.getMonitorCap ())
				continue;

			// ignore system user

			if (chatUser == chat.getSystemChatUser ())
				continue;

			selectedChatUsers.add (
				chatUser);

		}

		return selectedChatUsers;

	}

	@Override
	public
	void chatUserSetInfo (
			ChatUserRec chatUser,
			String info,
			Integer threadId) {

		Transaction transaction =
			database.currentTransaction ();

		ChatRec chat =
			chatUser.getChat ();

		TextRec newInfoText =
			textHelper.findOrCreate (info);

		// create the chat user info

		ChatUserInfoRec chatUserInfoRec =
			chatUserInfoHelper.insert (
				new ChatUserInfoRec ()

			.setChatUser (
				chatUser)

			.setCreationTime (
				instantToDate (
					transaction.now ()))

			.setOriginalText (
				newInfoText)

			.setStatus (
				ChatUserInfoStatus.moderatorPending)

			.setThreadId (
				threadId)

		);

		chatUser

			.setNewChatUserInfo (
				chatUserInfoRec);

		// create a queue item if necessary

		if (chatUser.getQueueItem () == null) {

			QueueItemRec queueItem =
				queueLogic.createQueueItem (
					queueLogic.findQueue (chat, "user"),
					chatUser,
					chatUser,
					chatUser.getPrettyName (),
					"Info to approve");

			chatUser

				.setQueueItem (
					queueItem);

		}

	}

	@Override
	public
	void sendNameHint (
			ChatUserRec chatUser) {

		ChatRec chat =
			chatUser.getChat ();

		// send the message

		chatSendLogic.sendSystemMagic (
			chatUser,
			Optional.<Integer>absent (),
			"name_hint",
			commandHelper.findByCode (chat, "magic"),
			commandHelper.findByCode (chat, "name").getId (),
			Collections.<String,String>emptyMap ());

		// and update the chat user

		chatUser

			.setLastNameHint (
				new Date ());

	}

	@Override
	public
	void sendPicHint (
			ChatUserRec chatUser) {

		ChatRec chat =
			chatUser.getChat ();

		// send the message

		chatSendLogic.sendSystemMmsFree (
			chatUser,
			Optional.<Integer>absent (),
			"photo_hint",
			commandHelper.findByCode (chat, "set_photo"));

		// and update the chat user

		chatUser

			.setLastPicHint (
				new Date ());

	}

	@Override
	public
	void sendPicHint2 (
			ChatUserRec chatUser) {

		ChatRec chat =
			chatUser.getChat ();

		// send the message

		chatSendLogic.sendSystemMmsFree (
			chatUser,
			Optional.<Integer>absent (),
			"photo_hint_2",
			commandHelper.findByCode (chat, "set_photo"));

		// and update the chat user

		chatUser

			.setLastPicHint (
				new Date ());

	}

}