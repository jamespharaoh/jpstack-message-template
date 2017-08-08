package wbs.framework.entity.meta.model;

import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.camelToHyphen;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.component.scaffold.PluginManager;
import wbs.framework.component.scaffold.PluginSpec;
import wbs.framework.data.tools.DataFromXml;
import wbs.framework.data.tools.DataFromXmlBuilder;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.utils.io.RuntimeIoException;

@Accessors (fluent = true)
@SingletonComponent ("modelMetaLoader")
public
class ModelMetaLoader {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	PluginManager pluginManager;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <DataFromXmlBuilder> dataFromXmlBuilderProvider;

	@PrototypeDependency
	@ModelMetaData
	Map <Class <?>, ComponentProvider <Object>> modelMetaDataProviders;

	// properties

	@Getter
	Map <String, RecordSpec> modelMetas;

	@Getter
	Map <String, RecordSpec> componentMetas;

	// state

	DataFromXml dataFromXml;

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

			createDataFromXml (
				taskLogger);

			loadSpecs (
				taskLogger);

		}

	}

	// implementation

	private
	void createDataFromXml (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"createDataFromXml");

		) {

			dataFromXml =
				dataFromXmlBuilderProvider.provide (
					taskLogger)

				.registerBuilders (
					taskLogger,
					modelMetaDataProviders)

				.build (
					taskLogger)

			;

		}

	}

	private
	void loadSpecs (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"loadSpecs");

		) {

			ImmutableMap.Builder <String, RecordSpec> modelBuilder =
				ImmutableMap.builder ();

			ImmutableMap.Builder <String, RecordSpec> componentBuilder =
				ImmutableMap.builder ();

			pluginManager.plugins ().forEach (
				pluginSpec -> {

				if (
					isNull (
						pluginSpec.models ())
				) {
					return;
				}

				pluginSpec.models ().models ().forEach (
					pluginModelSpec ->
						loadModelMeta (
							taskLogger,
							modelBuilder,
							pluginSpec,
							pluginModelSpec.name ()));

				pluginSpec.models ().componentTypes ().forEach (
					pluginComponentTypeSpec ->
						loadModelMeta (
							taskLogger,
							componentBuilder,
							pluginSpec,
							pluginComponentTypeSpec.name ()));

			});

			if (taskLogger.errors ()) {

				throw new RuntimeException (
					stringFormat (
						"Aborting due to %s errors",
						integerToDecimalString (
							taskLogger.errorCount ())));

			}

			modelMetas =
				modelBuilder.build ();

			componentMetas =
				componentBuilder.build ();

		}

	}

	private
	void loadModelMeta (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ImmutableMap.Builder <String, RecordSpec> builder,
			@NonNull PluginSpec plugin,
			@NonNull String modelName) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"loadModelMeta");

		) {

			String resourceName =
				stringFormat (
					"/%s/model/%s-record.xml",
					plugin.packageName ().replace ('.', '/'),
					camelToHyphen (
						modelName));

			try (

				InputStream inputStream =
					getClass ().getResourceAsStream (
						resourceName);

			) {

				if (inputStream == null) {

					taskLogger.errorFormat (
						"Record spec not found for %s.%s: %s",
						plugin.name (),
						modelName,
						resourceName);

					return;

				}

				RecordSpec spec;

				try {

					spec =
						(RecordSpec)
						dataFromXml.readInputStreamRequired (
							taskLogger,
							inputStream,
							resourceName,
							ImmutableList.<Object> of (
								plugin));

				} catch (Exception exception) {

					taskLogger.errorFormatException (
						exception,
						"Error reading model meta for %s.%s: %s",
						plugin.name (),
						modelName,
						resourceName);

					return;

				}

				if (
					stringNotEqualSafe (
						spec.name (),
						modelName)
				) {

					taskLogger.errorFormat (
						"Model meta name %s should be %s in %s",
						spec.name (),
						modelName,
						resourceName);

					return;

				}

				builder.put (
					modelName,
					spec);

			} catch (IOException ioException) {

				throw new RuntimeIoException (
					ioException);

			}

		}

	}

}
