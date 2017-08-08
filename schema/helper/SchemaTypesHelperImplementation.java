package wbs.framework.schema.helper;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.TypeUtils.classForName;
import static wbs.utils.etc.TypeUtils.classNameSimple;
import static wbs.utils.string.StringUtils.camelToUnderscore;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;
import org.joda.time.Instant;
import org.joda.time.LocalDate;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.scaffold.PluginCustomTypeSpec;
import wbs.framework.component.scaffold.PluginEnumTypeSpec;
import wbs.framework.component.scaffold.PluginManager;
import wbs.framework.component.scaffold.PluginSpec;
import wbs.framework.hibernate.EnumUserType;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@Accessors (fluent = true)
@SingletonComponent ("schemaTypesHelper")
public
class SchemaTypesHelperImplementation
	implements SchemaTypesHelper {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	PluginManager pluginManager;

	@SingletonDependency
	SchemaNamesHelper schemaNamesHelper;

	// properties

	@Getter
	Map <Class <?>, List <String>> fieldTypeNames;

	@Getter
	Map <String, List <String>> enumTypes;

	// implementation

	@NormalLifecycleSetup
	public
	void init (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"init");

		) {

			initTypeNames (
				taskLogger);

		}

	}

	void initTypeNames (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"initTypeNames");

		) {

			ImmutableMap.Builder <Class <?>, List <String>> fieldTypeNamesBuilder =
				ImmutableMap.builder ();

			ImmutableMap.Builder <String, List <String>> enumTypesBuilder =
				ImmutableMap.builder ();

			for (
				Map.Entry <Class <?>, String> entry
					: builtinFieldTypeNames.entrySet ()
			) {

				fieldTypeNamesBuilder.put (
					entry.getKey (),
					ImmutableList.of (
						entry.getValue ()));

			}

			for (
				PluginSpec plugin
					: pluginManager.plugins ()
			) {

				if (plugin.models () == null)
					continue;

				for (
					PluginEnumTypeSpec enumType
						: plugin.models ().enumTypes ()
				) {

					initEnumType (
						taskLogger,
						fieldTypeNamesBuilder,
						enumTypesBuilder,
						enumType);

				}

				for (
					PluginCustomTypeSpec customType
						: plugin.models ().customTypes ()
				) {

					initCustomType (
						taskLogger,
						fieldTypeNamesBuilder,
						enumTypesBuilder,
						customType);

				}

			}

			taskLogger.makeException ();

			fieldTypeNames =
				fieldTypeNamesBuilder.build ();

			enumTypes =
				enumTypesBuilder.build ();

		}

	}

	@SuppressWarnings ({ "unchecked", "rawtypes" })
	void initEnumType (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ImmutableMap.Builder <Class <?>, List <String>>
				fieldTypeNamesBuilder,
			@NonNull ImmutableMap.Builder <String, List <String>>
				enumTypesBuilder,
			PluginEnumTypeSpec enumType) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"initEnumType");

		) {

			String enumClassName =
				stringFormat (
					"%s.model.%s",
					enumType.plugin ().packageName (),
					capitalise (enumType.name ()));

			Class enumClass;

			try {

				enumClass =
					Class.forName (
						enumClassName);

			} catch (ClassNotFoundException exception) {

				taskLogger.errorFormat (
					"No such class %s",
					enumClassName);

				return;

			}

			EnumUserType enumUserType =
				new EnumUserType ()

				.sqlType (
					1111)

				.enumClass (
					enumClass)

				.auto (
					String.class);

			String typeName =
				camelToUnderscore (
					enumClass.getSimpleName ());

			fieldTypeNamesBuilder.put (
				enumClass,
				ImmutableList.<String>of (
					typeName));

			if (enumUserType.sqlType () == 1111) {

				ImmutableList.Builder<String> enumValuesBuilder =
					ImmutableList.<String>builder ();

				for (
					Object enumValue
						: enumUserType.databaseValues ()
				) {

					enumValuesBuilder.add (
						(String) enumValue);

				}

				enumTypesBuilder.put (
					typeName,
					enumValuesBuilder.build ());

			}

		}

	}

	void initCustomType (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ImmutableMap.Builder <Class <?>, List <String>>
				fieldTypeNamesBuilder,
			ImmutableMap.Builder <String, List <String>> enumTypesBuilder,
			PluginCustomTypeSpec customType) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"initCustomType");

		) {

			String objectClassName =
				stringFormat (
					"%s.model.%s",
					customType.plugin ().packageName (),
					capitalise (
						customType.name ()));

			Optional <Class <?>> objectClassOptional =
				classForName (
					objectClassName);

			if (
				optionalIsNotPresent (
					objectClassOptional)
			) {

				taskLogger.errorFormat (
					"No such class %s",
					objectClassName);

			}

			String helperClassName =
				stringFormat (
					"%s.hibernate.%sType",
					customType.plugin ().packageName (),
					capitalise (
						customType.name ()));

			Optional<Class<?>> helperClassOptional =
				classForName (
					helperClassName);

			if (
				optionalIsNotPresent (
					helperClassOptional)
			) {

				taskLogger.errorFormat (
					"No such class %s",
					helperClassName);

			}

			if (

				optionalIsNotPresent (
					objectClassOptional)

				|| optionalIsNotPresent (
					helperClassOptional)

			) {
				return;
			}

			Class<?> objectClass =
				objectClassOptional.get ();

			Class<?> helperClass =
				helperClassOptional.get ();

			Object helper;

			try {

				helper =
					helperClass.newInstance ();

			} catch (Exception exception) {

				taskLogger.errorFormatException (
					exception,
					"Error instantiating %s",
					helperClass.getName ());

				return;

			}

			if (helper instanceof EnumUserType) {

				EnumUserType<?,?> enumHelper =
					(EnumUserType<?,?>)
					helper;

				String typeName =
					enumHelper.sqlType () == 1111

					? camelToUnderscore (
						enumHelper.enumClass ().getSimpleName ())

					: builtinSqlTypeNames.get (
						enumHelper.sqlType ());

				if (typeName == null) {

					taskLogger.errorFormat (
						"Don't know how to handle sql type %s for %s",
						integerToDecimalString (
							enumHelper.sqlType ()),
						helper.getClass ().getName ());

					return;

				}

				fieldTypeNamesBuilder.put (
					objectClass,
					ImmutableList.<String>of (typeName));

				if (enumHelper.sqlType () == 1111) {

					ImmutableList.Builder<String> enumValuesBuilder =
						ImmutableList.<String>builder ();

					for (
						Object enumValue
							: enumHelper.databaseValues ()
					) {

						enumValuesBuilder.add (
							(String) enumValue);

					}

					enumTypesBuilder.put (
						typeName,
						enumValuesBuilder.build ());

				}

				return;

			}

			if (helper instanceof CompositeUserType) {

				CompositeUserType compositeUserType =
					(CompositeUserType)
					helper;

				ImmutableList.Builder<String> typeNamesBuilder =
					ImmutableList.<String>builder ();

				for (
					Type propertyType
						: compositeUserType.getPropertyTypes ()
				) {

					String typeName =
						builtinFieldTypeNames.get (
							propertyType.getReturnedClass ());

					if (typeName == null) {

						taskLogger.errorFormat (
							"Don't know how to handle sql type %s for %s",
							classNameSimple (
								propertyType.getReturnedClass ()),
							helper.getClass ().getName ());

						return;

					}

					typeNamesBuilder.add (
						typeName);

				}

				fieldTypeNamesBuilder.put (
					compositeUserType.returnedClass (),
					typeNamesBuilder.build ());

				return;

			}

			taskLogger.errorFormat (
				"Don't know how to handle %s",
				classNameSimple (
					helper.getClass ()));

			return;

		}

	}

	Map <Class <?>, String> builtinFieldTypeNames =
		ImmutableMap.<Class <?>, String> builder ()

		.put (
			Boolean.class,
			"boolean")

		.put (
			Double.class,
			"double precision")

		.put (
			Integer.class,
			"integer")

		.put (
			Long.class,
			"bigint")

		.put (
			String.class,
			"text")

		.put (
			byte[].class,
			"bytea")

		.put (
			LocalDate.class,
			"date")

		.put (
			Instant.class,
			"text")

		.put (
			Date.class,
			"timestamp with time zone")

		.put (
			Character.class,
			"char (1)")

		.build ();

	Map <Integer, String> builtinSqlTypeNames =
		ImmutableMap.<Integer, String> builder ()

		.put (
			Types.VARCHAR,
			"text")

		.put (
			Types.CHAR,
			"char (1)")

		.put (
			Types.INTEGER,
			"int")

		.put (
			Types.BIGINT,
			"bigint")

		.build ();

}
