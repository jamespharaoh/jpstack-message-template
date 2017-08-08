package wbs.framework.data.tools;

import static wbs.utils.etc.EnumUtils.enumNameHyphens;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.ReflectionUtils.fieldGet;
import static wbs.utils.string.StringUtils.nullIfEmptyString;
import static wbs.utils.string.StringUtils.stringFormat;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import lombok.NonNull;

import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChild;
import wbs.framework.data.annotations.DataClass;

public
class DataToSimple {

	public
	Object toJson (
			@NonNull Object dataValue) {

		Class<?> dataClass =
			dataValue.getClass ();

		if (simpleClasses.contains (dataValue.getClass ())) {

			return dataValue;

		} else if (dataValue instanceof Enum) {

			Enum <?> dataEnum =
				(Enum <?>) dataValue;

			return enumNameHyphens (
				dataEnum);

		} else if (dataValue instanceof List) {

			List<?> dataList =
				(List<?>) dataValue;

			ImmutableList.Builder<Object> jsonListBuilder =
				ImmutableList.<Object>builder ();

			for (
				Object dataListElement
					: dataList
			) {

				jsonListBuilder.add (
					toJson (dataListElement));

			}

			return jsonListBuilder.build ();

		} else if (dataValue instanceof Map) {

			Map<?,?> dataMap =
				(Map<?,?>) dataValue;

			ImmutableMap.Builder<String,Object> jsonMapBuilder =
				ImmutableMap.<String,Object>builder ();

			for (
				Map.Entry<?,?> dataMapEntry
					: dataMap.entrySet ()
			) {

				jsonMapBuilder.put (
					(String) dataMapEntry.getKey (),
					toJson (dataMapEntry.getValue ()));

			}

			return jsonMapBuilder.build ();

		} else {

			DataClass dataClassAnnotation =
				dataClass.getAnnotation (
					DataClass.class);

			if (dataClassAnnotation == null) {

				throw new RuntimeException (
					stringFormat (
						"Don't know how to convert %s ",
						dataClass.getSimpleName (),
						"to JSON"));

			}

			ImmutableMap.Builder <String, Object> jsonValueBuilder =
				ImmutableMap.builder ();

			for (
				Field field
					: dataClass.getDeclaredFields ()
			) {

				field.setAccessible (
					true);

				Object fieldValue =
					fieldGet (
						field,
						dataValue);

				if (fieldValue == null)
					continue;

				DataAttribute dataAttribute =
					field.getAnnotation (
						DataAttribute.class);

				if (dataAttribute != null) {

					jsonValueBuilder.put (
						ifNull (
							nullIfEmptyString (
								dataAttribute.name ()),
							field.getName ()),
						toJson (
							fieldValue));

				}

				DataChild dataChild =
					field.getAnnotation (
						DataChild.class);

				if (dataChild != null) {

					jsonValueBuilder.put (
						ifNull (
							nullIfEmptyString (
								dataChild.name ()),
							field.getName ()),
						toJson (
							fieldValue));

				}

			}

			return jsonValueBuilder.build ();

		}

	}

	// data

	Set <Class <?>> simpleClasses =
		ImmutableSet.<Class <?>> of (
			Boolean.class,
			Double.class,
			Float.class,
			Integer.class,
			Long.class,
			String.class);

}
