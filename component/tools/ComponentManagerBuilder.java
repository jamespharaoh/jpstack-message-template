package wbs.framework.component.tools;

import static wbs.utils.collection.CollectionUtils.collectionHasOneItem;
import static wbs.utils.collection.CollectionUtils.collectionHasTwoItems;
import static wbs.utils.collection.CollectionUtils.listLastItemRequired;
import static wbs.utils.collection.IterableUtils.iterableOnlyItemRequired;
import static wbs.utils.collection.MapUtils.mapContainsKey;
import static wbs.utils.collection.MapUtils.mapItemForKeyRequired;
import static wbs.utils.collection.MapUtils.mapWithDerivedKey;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.TypeUtils.classEqualSafe;
import static wbs.utils.etc.TypeUtils.classForName;
import static wbs.utils.string.StringUtils.hyphenToCamel;
import static wbs.utils.string.StringUtils.keyEqualsString;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.apache.commons.lang3.tuple.Pair;

import wbs.api.module.ApiModule;
import wbs.api.module.ApiModuleFactory;
import wbs.api.module.ApiModuleSpec;
import wbs.api.module.ApiModuleSpecFactory;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentManager;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.component.registry.ComponentDefinition;
import wbs.framework.component.registry.ComponentRegistryImplementation;
import wbs.framework.component.scaffold.BuildLayerPluginSpec;
import wbs.framework.component.scaffold.BuildLayerSpec;
import wbs.framework.component.scaffold.BuildSpec;
import wbs.framework.component.scaffold.PluginApiModuleSpec;
import wbs.framework.component.scaffold.PluginBootstrapComponentSpec;
import wbs.framework.component.scaffold.PluginComponentSpec;
import wbs.framework.component.scaffold.PluginLayerSpec;
import wbs.framework.component.scaffold.PluginManager;
import wbs.framework.component.scaffold.PluginSpec;
import wbs.framework.data.tools.DataFromXmlBuilder;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.LoggingLogic;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@PrototypeComponent ("componentManagerBuilder")
@Accessors (fluent = true)
public
class ComponentManagerBuilder {

	// singleton dependencies

	@SingletonDependency
	AnnotatedClassComponentTools annotatedClassComponentTools;

	@SingletonDependency
	ComponentManager bootstrapComponentManager;

	@SingletonDependency
	BuildSpec buildSpec;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	LoggingLogic loggingLogic;

	@SingletonDependency
	PluginManager pluginManager;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ComponentRegistryImplementation>
		componentRegistryImplementationProvider;

	@PrototypeDependency
	ComponentProvider <DataFromXmlBuilder> dataFromXmlBuilderProvider;

	// properties

	@Getter @Setter
	String primaryProjectPackageName;

	@Getter @Setter
	String primaryProjectName;

	@Getter @Setter
	List <String> configNames =
		new ArrayList<> ();

	@Getter @Setter
	List <String> layerNames =
		new ArrayList<> ();

	@Getter @Setter
	String outputPath;

	// state

	Map <String, BuildLayerSpec> layers;

	Map <String, Pair <Class <?>, Object>> unmanagedSingletons =
		new LinkedHashMap<> ();

	List <ComponentDefinition> componentDefinitionsToRegister =
		new ArrayList<> ();

	ComponentRegistryImplementation componentRegistry;

	// implementation

	public
	ComponentManagerBuilder addSingletonComponent (
			@NonNull String componentName,
			@NonNull Class <?> interfaceClass,
			@NonNull Object component) {

		unmanagedSingletons.put (
			componentName,
			Pair.of (
				interfaceClass,
				component));

		return this;

	}

	public
	ComponentManager build (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"build");

		) {

			loadPlugins (
				taskLogger);

			registerComponents (
				taskLogger);

			return componentRegistry.build ();

		}

	}

	private
	void loadPlugins (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"loadPlugins");

		) {

			layers =
				mapWithDerivedKey (
					buildSpec.layers (),
					BuildLayerSpec::name);

			taskLogger.makeException ();

		}

	}

	private
	void registerComponents (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"registerComponents");


		) {

			createComponentRegistry (
				taskLogger);

			registerBootstrapComponents (
				taskLogger);

			registerLayerComponents (
				taskLogger);

			registerConfigComponents (
				taskLogger);

			registerUnmanagedSingletons (
				taskLogger);

			if (taskLogger.errors ()) {

				throw new RuntimeException (
					stringFormat (
						"Aborting due to %s errors",
						integerToDecimalString (
							taskLogger.errorCount ())));

			}

			for (
				ComponentDefinition componentDefinition
					: componentDefinitionsToRegister
			) {

				componentRegistry.registerDefinition (
					taskLogger,
					componentDefinition);

			}

		}

	}

	private
	void registerBootstrapComponents (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"registerLayerComponents");

		) {

			bootstrapComponentManager.allSingletonComponents (
				taskLogger)

				.forEach (
					(componentName, componentClassAndValue) -> {

					Class <?> interfaceClass =
						componentClassAndValue.left ();

					if (
						classEqualSafe (
							interfaceClass,
							ComponentManager.class)
					) {
						return;
					}

					Object component =
						componentClassAndValue.right ();

					componentRegistry.registerUnmanagedSingleton (
						taskLogger,
						componentName,
						interfaceClass,
						component);

				});

		}

	}

	private
	void registerLayerComponents (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"registerLayerComponents");

		) {

			for (
				String layerName
					: layerNames
			) {

				BuildLayerSpec buildLayer =
					mapItemForKeyRequired (
						layers,
						layerName);

				taskLogger.noticeFormat (
					"Loading components for layer %s",
					layerName);

				for (
					PluginSpec plugin
						: pluginManager.plugins ()
				) {

					PluginLayerSpec pluginLayer =
						plugin.layersByName ().get (
							layerName);

					if (
						isNotNull (
							pluginLayer)
					) {

						registerPluginLayerComponents (
							taskLogger,
							plugin,
							pluginLayer);

					}

				}

				registerLayerAutomaticComponents (
					taskLogger,
					buildLayer,
					pluginManager.plugins ());

			}

		}

	}

	void registerPluginLayerComponents (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull PluginSpec pluginSpec,
			@NonNull PluginLayerSpec layerSpec) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"registerPluginLayerComponents",
					keyEqualsString (
						"pluginName",
						pluginSpec.name ()),
					keyEqualsString (
						"layerName",
						layerSpec.name ()));
		) {

			for (
				PluginBootstrapComponentSpec component
					: layerSpec.bootstrapComponents ()
			) {

				registerLayerComponent (
					taskLogger,
					pluginSpec,
					layerSpec,
					component.className ());

			}

			for (
				PluginComponentSpec component
					: layerSpec.components ()
			) {

				registerLayerComponent (
					taskLogger,
					pluginSpec,
					layerSpec,
					component.className ());

			}

		}

	}

	void registerLayerAutomaticComponents (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull BuildLayerSpec buildLayer,
			@NonNull List <PluginSpec> plugins) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"registerLayerAutomaticComponents");

		) {

			for (
				BuildLayerPluginSpec buildLayerPlugin
					: buildLayer.plugins ()
			) {

				ComponentPlugin componentPlugin =
					bootstrapComponentManager.getComponentRequired (
						taskLogger,
						buildLayerPlugin.name (),
						ComponentPlugin.class);

				for (
					PluginSpec plugin
						: plugins
				) {

					try {

						componentPlugin.registerComponents (
							taskLogger,
							componentRegistry,
							plugin);

					} catch (Exception exception) {

						taskLogger.errorFormatException (
							exception,
							"Error running component plugin %s ",
							buildLayerPlugin.name (),
							"on plugin module %s",
							plugin.name ());

					}

				}

			}

		}

	}

	long registerApiModule (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull PluginApiModuleSpec pluginApiModuleSpec) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"registerApiModule");

		) {

			String xmlResourceName =
				stringFormat (
					"/%s/api/%s-api.xml",
					pluginApiModuleSpec
						.plugin ()
						.packageName ()
						.replace (".", "/"),
					pluginApiModuleSpec
						.name ());

			String apiModuleSpecComponentName =
				stringFormat (
					"%sApiModuleSpec",
					hyphenToCamel (
						pluginApiModuleSpec.name ()));

			String apiModuleComponentName =
				stringFormat (
					"%sApiModule",
					hyphenToCamel (
						pluginApiModuleSpec.name ()));

			componentRegistry.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.name (
					apiModuleSpecComponentName)

				.componentClass (
					ApiModuleSpec.class)

				.scope (
					"singleton")

				.factoryClass (
					ApiModuleSpecFactory.class)

				.addValueProperty (
					"xmlResourceName",
					optionalOf (
						xmlResourceName))

			);

			componentRegistry.registerDefinition (
				taskLogger,
				new ComponentDefinition ()

				.name (
					apiModuleComponentName)

				.componentClass (
					ApiModule.class)

				.scope (
					"singleton")

				.factoryClass (
					ApiModuleFactory.class)

				.addReferenceProperty (
					"apiModuleSpec",
					"singleton",
					apiModuleSpecComponentName)

			);

			return 0;

		}

	}

	void registerLayerComponent (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull PluginSpec pluginSpec,
			@NonNull PluginLayerSpec layerSpec,
			@NonNull String className) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"registerLayerComponent");

		) {

			taskLogger.debugFormat (
				"Loading %s from %s",
				className,
				pluginSpec.name ());

			String componentClassName =
				stringFormat (
					"%s.%s",
					pluginSpec.packageName (),
					className);

			Optional <Class <?>> componentClassOptional =
				classForName (
					componentClassName);

			if (
				optionalIsNotPresent (
					componentClassOptional)
			) {

				taskLogger.errorFormat (
					"No such class %s in plugin %s layer %s",
					componentClassName,
					pluginSpec.name (),
					layerSpec.name ());

				return;

			}

			Class <?> componentClass =
				optionalGetRequired (
					componentClassOptional);

			List <ComponentDefinition> componentDefinitions =
				annotatedClassComponentTools.definitionsForClass (
					taskLogger,
					componentClass);

			if (taskLogger.errors ()) {
				return;
			}

			if (
				collectionHasOneItem (
					componentDefinitions)
			) {

				ComponentDefinition componentDefinition =
					iterableOnlyItemRequired (
						componentDefinitions);

				if (

					stringEqualSafe (
						componentDefinition.scope (),
						"singleton")

					&& mapContainsKey (
						bootstrapComponentManager.allSingletonComponents (
							taskLogger),
						componentDefinition.name ())

				) {

					taskLogger.debugFormat (
						"Skip bootstrap singleton component %s",
						componentDefinition.name ());

					return;

				}

			}

			componentDefinitions.forEach (
				componentDefinition ->
					componentRegistry.registerDefinition (
						taskLogger,
						componentDefinition));

			ComponentDefinition componentDefinition =
				listLastItemRequired (
					componentDefinitions);

			String componentName =
				componentDefinition.name ();

			if (
				collectionHasTwoItems (
					componentDefinitions)
			) {

				componentRegistry.addRequestComponentName (
					componentName);

			}

		}

	}

	void registerConfigComponents (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"registerConfigComponents");

		) {

			for (
				String configName
					: configNames
			) {

				taskLogger.noticeFormat (
					"Loading configuration %s",
					configName);

				String configPath =
					stringFormat (
						"conf/%s-config-components.xml",
						configName);

				componentRegistry.registerXmlFilename (
					taskLogger,
					configPath);

			}

		}

	}

	void registerUnmanagedSingletons (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"registerSingletonComponents");

		) {

			unmanagedSingletons.forEach (
				(componentName, componentClassAndValue) -> {

				Class <?> interfaceClass =
					componentClassAndValue.getLeft ();

				Object component =
					componentClassAndValue.getRight ();

				componentRegistry.registerUnmanagedSingleton (
					taskLogger,
					componentName,
					interfaceClass,
					component);

			});

		}

	}

	void createComponentRegistry (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"createComponentRegistry");

		) {

			componentRegistry =
				componentRegistryImplementationProvider.provide (
					taskLogger)

				.outputPath (
					outputPath)

			;

		}

	}

	public
	ComponentManagerBuilder registerComponentDefinition (
			@NonNull ComponentDefinition componentDefinition) {

		componentDefinitionsToRegister.add (
			componentDefinition);

		return this;

	}

}
