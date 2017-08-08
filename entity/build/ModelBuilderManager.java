package wbs.framework.entity.build;

import java.util.List;
import java.util.Map;

import lombok.NonNull;

import wbs.framework.builder.Builder;
import wbs.framework.builder.Builder.MissingBuilderBehaviour;
import wbs.framework.builder.BuilderFactory;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@SingletonComponent ("modelBuilderManager")
public
class ModelBuilderManager {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <BuilderFactory <?, TaskLogger>> builderFactoryProvider;

	@PrototypeDependency
	@ModelBuilder
	Map <Class <?>, ComponentProvider <Object>> modelBuilderProviders;

	// state

	Builder <TaskLogger> modelBuilder;

	// lifecycle

	@NormalLifecycleSetup
	public
	void setup (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setup");

		) {

			modelBuilder =
				builderFactoryProvider.provide (
					taskLogger)

				.contextClass (
					TaskLogger.class)

				.addBuilders (
					taskLogger,
					modelBuilderProviders)

				.create (
					taskLogger);

		}

	}

	// implementation

	public
	void build (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ModelFieldBuilderContext context,
			@NonNull List <?> sourceItems,
			@NonNull ModelFieldBuilderTarget target) {

		modelBuilder.descend (
			parentTaskLogger,
			context,
			sourceItems,
			target,
			MissingBuilderBehaviour.error);

	}

}
