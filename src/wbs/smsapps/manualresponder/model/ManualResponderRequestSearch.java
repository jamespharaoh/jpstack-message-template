package wbs.smsapps.manualresponder.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import org.joda.time.Instant;

@Accessors (fluent = true)
@Data
@EqualsAndHashCode
@ToString
public
class ManualResponderRequestSearch {

	Integer manualResponderId;

	String numberLike;

	Instant timestampAfter;
	Instant timestampBefore;

	Order order = Order.timestampDesc;

	public static
	enum Order {
		timestampDesc
	}

}