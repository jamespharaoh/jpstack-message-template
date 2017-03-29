package wbs.sms.message.inbox.logic;

import org.joda.time.Instant;

import wbs.framework.logging.TaskLogger;

import wbs.sms.message.inbox.model.InboxMultipartBufferRec;
import wbs.sms.network.model.NetworkRec;
import wbs.sms.route.core.model.RouteRec;

public
interface SmsInboxMultipartLogic {

	InboxMultipartBufferRec insertInboxMultipart (
			TaskLogger parentTaskLogger,
			RouteRec route,
			long multipartId,
			long multipartSegMax,
			long multipartSeg,
			String msgTo,
			String msgFrom,
			Instant msgNetworkTime,
			NetworkRec msgNetwork,
			String msgOtherId,
			String msgText);

	boolean insertInboxMultipartMessage (
			TaskLogger parentTaskLogger,
			InboxMultipartBufferRec inboxMultipartBuffer);

}
