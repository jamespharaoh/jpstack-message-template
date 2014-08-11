package wbs.platform.object.summary;

import static wbs.framework.utils.etc.Misc.capitalise;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.Getter;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.application.context.ApplicationContext;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.platform.console.annotations.ConsoleModuleBuilderHandler;
import wbs.platform.console.context.ConsoleContextBuilderContainer;
import wbs.platform.console.context.ResolvedConsoleContextExtensionPoint;
import wbs.platform.console.forms.CodeFormFieldSpec;
import wbs.platform.console.forms.DescriptionFormFieldSpec;
import wbs.platform.console.forms.FormFieldSet;
import wbs.platform.console.forms.IdFormFieldSpec;
import wbs.platform.console.forms.NameFormFieldSpec;
import wbs.platform.console.forms.ParentFormFieldSpec;
import wbs.platform.console.helper.ConsoleHelper;
import wbs.platform.console.metamodule.ConsoleMetaManager;
import wbs.platform.console.metamodule.ConsoleMetaModuleImpl;
import wbs.platform.console.module.ConsoleModuleBuilder;
import wbs.platform.console.module.ConsoleModuleImpl;
import wbs.platform.console.part.PagePart;
import wbs.platform.console.responder.ConsoleFile;
import wbs.platform.console.tab.ConsoleContextTab;
import wbs.platform.console.tab.TabContextResponder;
import wbs.platform.text.console.TextPart;

@Accessors (fluent = true)
@PrototypeComponent ("objectSummaryPageBuilder")
@ConsoleModuleBuilderHandler
public
class ObjectSummaryPageBuilder {

	// dependencies

	@Inject
	ApplicationContext applicationContext;

	@Inject
	ConsoleMetaManager consoleMetaManager;

	@Inject
	ConsoleModuleBuilder consoleModuleBuilder;

	// prototype dependencies

	@Inject
	Provider<ConsoleFile> consoleFile;

	@Inject
	Provider<ConsoleContextTab> contextTab;

	@Inject
	Provider<ObjectSummaryPart> objectSummaryPart;

	@Inject
	Provider<ParentFormFieldSpec> parentField;

	@Inject
	Provider<ObjectSummaryFieldsPart> summaryFieldsPart;

	@Inject
	Provider<TabContextResponder> tabContextResponder;

	@Inject
	Provider<TextPart> textPart;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer container;

	@BuilderSource
	ObjectSummaryPageSpec spec;

	@BuilderTarget
	ConsoleModuleImpl consoleModule;

	// state

	@Getter
	ConsoleHelper<?> consoleHelper;

	FormFieldSet formFieldSet;

	String privKey;

	List<Provider<PagePart>> pagePartFactories =
		new ArrayList<Provider<PagePart>> ();

	// build meta

	public
	void buildMeta (
			ConsoleMetaModuleImpl consoleMetaModule) {

	}

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		setDefaults ();

		buildResponder ();

		for (ResolvedConsoleContextExtensionPoint resolvedExtensionPoint
				: consoleMetaManager.resolveExtensionPoint (
					container.extensionPointName ())) {

			buildContextTabs (
				resolvedExtensionPoint);

			buildContextFile (
				resolvedExtensionPoint);

		}

		builder.descend (
			spec,
			spec.builders (),
			this);

	}

	void buildContextTabs (
			ResolvedConsoleContextExtensionPoint resolvedExtensionPoint) {

		consoleModule.addContextTab (

			"end",

			contextTab.get ()

				.name (
					stringFormat (
						"%s.summary",
						container.pathPrefix ()))

				.defaultLabel (
					"Summary")

				.localFile (
					stringFormat (
						"%s.summary",
						container.pathPrefix ()))

				.privKeys (
					privKey),

			resolvedExtensionPoint.contextTypeNames ());

	}

	void buildContextFile (
			ResolvedConsoleContextExtensionPoint resolvedExtensionPoint) {

		consoleModule.addContextFile (

			stringFormat (
				"%s.summary",
				container.pathPrefix ()),

			consoleFile.get ()

				.getResponderName (
					stringFormat (
						"%sSummaryResponder",
						container.newBeanNamePrefix ()))

				.privKeys (
					privKey != null
						? Collections.singletonList (privKey)
						: Collections.<String>emptyList ()),

			resolvedExtensionPoint.contextTypeNames ());

	}

	void buildResponder () {

		Provider<PagePart> partFactory =
			new Provider<PagePart> () {

			@Override
			public
			PagePart get () {

				return objectSummaryPart.get ()
					.partFactories (pagePartFactories);

			}

		};

		consoleModule.addResponder (

			stringFormat (
				"%sSummaryResponder",
				container.newBeanNamePrefix ()),

			tabContextResponder.get ()

				.tab (
					stringFormat (
						"%s.summary",
						container.pathPrefix ()))

				.title (
					capitalise (
						stringFormat (
							"%s summary",
							consoleHelper.friendlyName ())))

				.pagePartFactory (
					partFactory));

	}

	public
	ObjectSummaryPageBuilder addFieldsPart (
			final FormFieldSet formFieldSet) {

		if (formFieldSet == null)
			return this;

		Provider<PagePart> partFactory =
			new Provider<PagePart> () {

			@Override
			public
			PagePart get () {

				return summaryFieldsPart.get ()
					.consoleHelper (consoleHelper)
					.formFieldSet (formFieldSet);

			}

		};

		pagePartFactories.add (
			partFactory);

		return this;

	}

	public
	ObjectSummaryPageBuilder addHeading (
			String heading) {

		final
		String html =
			stringFormat (
				"<h2>%h</h2>\n",
				heading);

		Provider<PagePart> pagePartFactory =
			new Provider<PagePart> () {

			@Override
			public
			PagePart get () {

				return textPart.get ()
					.text (html);

			}

		};

		pagePartFactories.add (
			pagePartFactory);


		return this;

	}

	public
	ObjectSummaryPageBuilder addPart (
			final String beanName) {

		Provider<PagePart> partFactory =
			new Provider<PagePart> () {

			@Override
			public
			PagePart get () {

				Object object =
					applicationContext.getBean (
						beanName,
						Object.class);

				if (object instanceof PagePart)
					return (PagePart) object;

				if (object instanceof Provider) {

					Provider<?> provider =
						(Provider<?>) object;

					return (PagePart)
						provider.get ();

				}

				throw new ClassCastException (
					object.getClass ().getName ());

			}

		};

		pagePartFactories.add (
			partFactory);

		return this;

	}

	void setDefaults () {

		consoleHelper =
			container.consoleHelper ();

		formFieldSet =
			spec.fieldsName () != null
				? consoleModuleBuilder.buildFormFieldSet (
					spec.consoleSpec (),
					spec.fieldsName ())
				: defaultFields ();

		privKey =
			spec.privKey ();

		if (spec.builders ().isEmpty ()) {

			addFieldsPart (
				formFieldSet);

		}

	}

	FormFieldSet defaultFields () {

		List<Object> formFieldSpecs =
			new ArrayList<Object> ();

		formFieldSpecs.add (
			new IdFormFieldSpec ());

		if (consoleHelper.parentTypeIsFixed ()) {

			// TODO this should not be disabled!

			/*
			ConsoleObjectHelper parentHelper =
				objectManager.getConsoleObjectHelper (
					maintSchedConsoleHelper.parentClass ());

			if (! parentHelper.isRoot ())
				fields.add (parentField.get ());
			*/

		} else {

			formFieldSpecs.add (
				new ParentFormFieldSpec ());

		}

		if (consoleHelper.codeExists ()) {

			formFieldSpecs.add (
				new CodeFormFieldSpec ());

		}

		if (consoleHelper.nameExists ()
				&& ! consoleHelper.nameIsCode ()) {

			formFieldSpecs.add (
				new NameFormFieldSpec ());

		}

		if (consoleHelper.descriptionExists ()) {

			formFieldSpecs.add (
				new DescriptionFormFieldSpec ());

		}

		String fieldSetName =
			stringFormat (
				"%s.summary",
				consoleHelper.objectName ());

		return consoleModuleBuilder.buildFormFieldSet (
			consoleHelper,
			fieldSetName,
			formFieldSpecs);

	}

}