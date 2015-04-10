package wbs.platform.object.criteria;

import static wbs.framework.utils.etc.Misc.in;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.record.Record;
import wbs.framework.utils.etc.BeanLogic;
import wbs.platform.console.helper.ConsoleHelper;
import wbs.platform.console.module.ConsoleModuleData;

@Accessors (fluent = true)
@DataClass ("where-in")
@PrototypeComponent ("whereInCriteriaSpec")
@ConsoleModuleData
public
class WhereInCriteriaSpec
	implements CriteriaSpec {

	@DataAttribute (
		value = "field",
		required = true)
	@Getter @Setter
	String fieldName;

	@DataChildren (
		direct = true,
		childElement = "item",
		valueAttribute = "value")
	@Getter @Setter
	List<String> values;

	@Override
	public
	boolean evaluate (
			ConsoleHelper<?> objectHelper,
			Record<?> object) {

		Object fieldValue =
			BeanLogic.getProperty (
				object,
				fieldName);

		if (fieldValue == null) {
			return false;
		}

		return in (
			fieldValue.toString (),
			values);

	}

}
