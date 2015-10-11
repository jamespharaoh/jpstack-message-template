package wbs.clients.apn.chat.core.model;

import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import org.apache.commons.lang3.builder.CompareToBuilder;

import wbs.framework.entity.annotations.CommonEntity;
import wbs.framework.entity.annotations.GeneratedIdField;
import wbs.framework.entity.annotations.ParentField;
import wbs.framework.entity.annotations.SimpleField;
import wbs.framework.record.CommonRecord;
import wbs.framework.record.Record;

@Accessors (chain = true)
@Data
@EqualsAndHashCode (of = "id")
@ToString (of = "id")
@CommonEntity
public
class ChatStatsRec
	implements CommonRecord<ChatStatsRec> {

	@GeneratedIdField
	Integer id;

	@ParentField
	ChatRec chat;

	@SimpleField
	Date timestamp;

	@SimpleField
	Integer numUsers;

	@SimpleField
	Integer numMonitors;

	// compare to

	@Override
	public
	int compareTo (
			Record<ChatStatsRec> otherRecord) {

		ChatStatsRec other =
			(ChatStatsRec) otherRecord;

		return new CompareToBuilder ()

			.append (
				other.getTimestamp (),
				getTimestamp ())

			.toComparison ();

	}

}
