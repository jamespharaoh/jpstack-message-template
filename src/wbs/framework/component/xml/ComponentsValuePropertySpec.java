package wbs.framework.component.xml;

import static wbs.utils.etc.OptionalUtils.optionalOf;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.registry.ComponentDefinition;
import wbs.framework.data.annotations.DataAncestor;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.data.annotations.DataParent;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@PrototypeComponent ("componentsValuePropertySpec")
@Accessors (fluent = true)
@DataClass ("value-property")
public
class ComponentsValuePropertySpec
	implements ComponentsComponentPropertySpec {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// properties

	@DataAncestor
	@Getter @Setter
	ComponentsSpec beans;

	@DataParent
	@Getter @Setter
	ComponentsComponentSpec bean;

	@DataAttribute
	@Getter @Setter
	String name;

	@DataAttribute
	@Getter @Setter
	String value;

	// public implementation

	@Override
	public
	void register (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ComponentDefinition componentDefinition) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"register");

		) {

			componentDefinition.addValueProperty (
				name,
				optionalOf (
					value));

		}

	}

}
