package wbs.framework.entity.build;

import static wbs.utils.etc.NullUtils.ifNull;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;

import wbs.framework.builder.Builder;
import wbs.framework.builder.BuilderComponent;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.entity.meta.ids.AssignedIdFieldSpec;
import wbs.framework.entity.model.ModelField;
import wbs.framework.entity.model.ModelFieldType;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@PrototypeComponent ("assignedIdModelFieldBuilder")
@ModelBuilder
public
class AssignedIdModelFieldBuilder
	implements BuilderComponent {

	// builder

	@BuilderParent
	ModelFieldBuilderContext context;

	@ClassSingletonDependency
	LogContext logContext;

	@BuilderSource
	AssignedIdFieldSpec spec;

	@BuilderTarget
	ModelFieldBuilderTarget target;

	// build

	@Override
	@BuildMethod
	public
	void build (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Builder <TaskLogger> builder) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"build");

		) {

			// create model field

			ModelField modelField =
				new ModelField ()

				.model (
					target.model ())

				.parentField (
					context.parentModelField ())

				.name (
					"id")

				.label (
					"id")

				.type (
					ModelFieldType.assignedId)

				.parent (
					false)

				.identity (
					false)

				.valueType (
					Long.class)

				.nullable (
					false)

				.columnNames (
					ImmutableList.<String>of (
						ifNull (
							spec.columnName (),
							"id")));

			// store field

			target.fields ().add (
				modelField);

			target.fieldsByName ().put (
				modelField.name (),
				modelField);

			if (target.model ().idField () != null)
				throw new RuntimeException ();

			target.model ().idField (
				modelField);

		}

	}

}
