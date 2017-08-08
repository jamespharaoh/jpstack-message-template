package wbs.framework.entity.generate;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.config.WbsSpecialConfig;
import wbs.framework.component.tools.ComponentFactory;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@SingletonComponent ("wbsSpecialConfig")
public
class ModelGenerateSpecialConfig
	implements ComponentFactory <WbsSpecialConfig> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// components

	@Override
	public
	WbsSpecialConfig makeComponent (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"makeComponent");

		) {

			return new WbsSpecialConfig ()

				.assumeNegativeCache (
					true);

		}

	}

}
