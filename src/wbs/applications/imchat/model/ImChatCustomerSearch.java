package wbs.applications.imchat.model;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@Accessors (chain = true)
@Data
@EqualsAndHashCode
@ToString
public
class ImChatCustomerSearch
	implements Serializable {

	Integer imChatId;

	String code;

	Order order =
		Order.timestampDesc;

	public static
	enum Order {
		timestampDesc
	}

}
