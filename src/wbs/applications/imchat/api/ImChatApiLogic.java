package wbs.applications.imchat.api;

import wbs.applications.imchat.model.ImChatConversationRec;
import wbs.applications.imchat.model.ImChatCustomerRec;
import wbs.applications.imchat.model.ImChatMessageRec;
import wbs.applications.imchat.model.ImChatPricePointRec;
import wbs.applications.imchat.model.ImChatProfileRec;

public
interface ImChatApiLogic {

	ImChatPricePointData pricePointData (
			ImChatPricePointRec pricePoint);

	ImChatProfileData profileData (
			ImChatProfileRec profile);

	ImChatCustomerData customerData (
			ImChatCustomerRec customer);

	ImChatConversationData conversationData (
			ImChatConversationRec conversation);

	ImChatMessageData messageData (
			ImChatMessageRec message);

}