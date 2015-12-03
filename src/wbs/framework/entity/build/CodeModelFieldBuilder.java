package wbs.framework.entity.build;

import static wbs.framework.utils.etc.Misc.camelToSpaces;
import static wbs.framework.utils.etc.Misc.camelToUnderscore;
import static wbs.framework.utils.etc.Misc.ifNull;

import com.google.common.collect.ImmutableList;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.entity.meta.CodeFieldSpec;
import wbs.framework.entity.model.ModelField;
import wbs.framework.entity.model.ModelFieldType;

@PrototypeComponent ("codeModelFieldBuilder")
@ModelBuilder
public
class CodeModelFieldBuilder {

	// builder

	@BuilderParent
	ModelFieldBuilderContext context;

	@BuilderSource
	CodeFieldSpec spec;

	@BuilderTarget
	ModelFieldBuilderTarget target;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		String fieldName =
			ifNull (
				spec.name (),
				"code");

		// create model field

		ModelField modelField =
			new ModelField ()

			.model (
				target.model ())

			.parentField (
				context.parentModelField ())

			.name (
				fieldName)

			.label (
				camelToSpaces (
					fieldName))

			.type (
				ModelFieldType.code)

			.parent (
				false)

			.identity (
				true)

			.valueType (
				String.class)

			.nullable (
				false)

			.columnNames (
				ImmutableList.<String>of (
					ifNull (
						spec.columnName (),
						camelToUnderscore (
							fieldName))));
		// store field

		target.fields ().add (
			modelField);

		target.fieldsByName ().put (
			modelField.name (),
			modelField);

		if (target.model ().codeField () != null)
			throw new RuntimeException ();

		target.model ().codeField (
			modelField);

	}

}