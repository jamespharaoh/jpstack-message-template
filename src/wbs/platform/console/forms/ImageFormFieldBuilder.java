package wbs.platform.console.forms;

import static wbs.framework.utils.etc.Misc.camelToSpaces;
import static wbs.framework.utils.etc.Misc.capitalise;
import static wbs.framework.utils.etc.Misc.ifNull;

import javax.inject.Inject;
import javax.inject.Provider;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.platform.console.annotations.ConsoleModuleBuilderHandler;
import wbs.platform.media.model.MediaRec;

@SuppressWarnings ({ "rawtypes", "unchecked" })
@PrototypeComponent ("imageFormFieldBuilder")
@ConsoleModuleBuilderHandler
public
class ImageFormFieldBuilder {

	// prototype dependencies

	@Inject
	Provider<IdentityFormFieldInterfaceMapping>
	identityFormFieldInterfaceMappingProvider;

	@Inject
	Provider<IdentityFormFieldNativeMapping>
	identityFormFieldNativeMappingProvider;

	@Inject
	Provider<NullFormFieldConstraintValidator>
	nullFormFieldValueConstraintValidatorProvider;

	@Inject
	Provider<NullFormFieldValueValidator>
	nullFormFieldValueValidatorProvider;

	@Inject
	Provider<ReadOnlyFormField>
	readOnlyFormFieldProvider;

	@Inject
	Provider<SimpleFormFieldAccessor>
	simpleFormFieldAccessorProvider;

	@Inject
	Provider<SimpleFormFieldUpdateHook>
	simpleFormFieldUpdateHookProvider;

	@Inject
	Provider<ImageFormFieldRenderer>
	imageFormFieldRendererProvider;

	@Inject
	Provider<UpdatableFormField>
	updatableFormFieldProvider;

	// builder

	@BuilderParent
	FormFieldBuilderContext context;

	@BuilderSource
	ImageFormFieldSpec spec;

	@BuilderTarget
	FormFieldSet formFieldSet;

	// state

	String name;
	String label;
	Boolean nullable;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		setDefaults ();
		buildField ();

	}

	void setDefaults () {

		name =
			spec.name ();

		label =
			ifNull (
				spec.label (),
				capitalise (
					camelToSpaces (
						name)));

		nullable =
			ifNull (
				spec.nullable (),
				false);

	}

	void buildField () {

		// accessor

		FormFieldAccessor accessor =
			simpleFormFieldAccessorProvider.get ()

			.name (
				name)

			.nativeClass (
				MediaRec.class);

		// native mapping

		FormFieldNativeMapping nativeMapping =
			identityFormFieldNativeMappingProvider.get ();

		// value validator

		FormFieldValueValidator valueValidator =
			nullFormFieldValueValidatorProvider.get ();

		// constraint validator

		FormFieldConstraintValidator constraintValidator =
			nullFormFieldValueConstraintValidatorProvider.get ();

		// interface mapping

		FormFieldInterfaceMapping interfaceMapping =
			identityFormFieldInterfaceMappingProvider.get ();

		// renderer

		FormFieldRenderer renderer =
			imageFormFieldRendererProvider.get ()

			.name (
				name)

			.label (
				label)

			.nullable (
				nullable);

		// update hook

		FormFieldUpdateHook updateHook =
			simpleFormFieldUpdateHookProvider.get ()

			.name (
				name);

		// form field

		formFieldSet.formFields ().add (
			updatableFormFieldProvider.get ()

			.name (
				name)

			.label (
				label)

			.accessor (
				accessor)

			.nativeMapping (
				nativeMapping)

			.valueValidator (
				valueValidator)

			.constraintValidator (
				constraintValidator)

			.interfaceMapping (
				interfaceMapping)

			.renderer (
				renderer)

			.updateHook (
				updateHook)

		);

	}

}