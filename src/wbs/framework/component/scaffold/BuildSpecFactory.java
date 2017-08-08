package wbs.framework.component.scaffold;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.component.tools.ComponentFactory;
import wbs.framework.data.tools.DataFromXml;
import wbs.framework.data.tools.DataFromXmlBuilder;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@SingletonComponent ("buildSpec")
public
class BuildSpecFactory
	implements ComponentFactory <BuildSpec> {

	// singleton components

	@ClassSingletonDependency
	LogContext logContext;

	// prototype components

	@PrototypeDependency
	ComponentProvider <DataFromXmlBuilder> dataFromXmlBuilderProvider;

	// public implementation

	@Override
	public
	BuildSpec makeComponent (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"makeComponent");

		) {

			DataFromXml buildDataFromXml =
				dataFromXmlBuilderProvider.provide (
					taskLogger)

				.registerBuilderClasses (
					taskLogger,
					BuildSpec.class,
					BuildLayerPluginSpec.class,
					BuildLayerSpec.class,
					BuildPluginSpec.class)

				.build (
					taskLogger);

			BuildSpec build =
				(BuildSpec)
				buildDataFromXml.readClasspathRequired (
					taskLogger,
					"/wbs-build.xml");

			return build;

		}

	}

}
