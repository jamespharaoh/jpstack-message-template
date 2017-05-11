package wbs.platform.object.summary;

import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenDetails;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.forms.context.FormContext;
import wbs.console.forms.context.FormContextBuilder;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.part.AbstractPagePart;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.scaffold.model.RootObjectHelper;

@Accessors (fluent = true)
@PrototypeComponent ("objectSummaryFieldsPart")
public
class ObjectSummaryFieldsPart <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
>
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	RootObjectHelper rootHelper;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	FormContextBuilder <ObjectType> formContextBuilder;

	// state

	ObjectType object;
	ParentType parent;

	FormContext <ObjectType> formContext;

	// implementation

	@Override
	public
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			object =
				consoleHelper.lookupObject (
					transaction,
					requestContext.consoleContextStuffRequired ());

			/*
			if (formFieldsProvider != null) {

				prepareParent (
					transaction);

				prepareFieldSet (
					transaction);

			}
			*/

			formContext =
				formContextBuilder.build (
					transaction,
					emptyMap (),
					object);

		}

	}

	void prepareParent (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepareParent");

		) {

			ConsoleHelper <ParentType> parentHelper =
				objectManager.findConsoleHelperRequired (
					consoleHelper.parentClass ());

			if (parentHelper.isRoot ()) {

				parent =
					parentHelper.findRequired (
						transaction,
						0l);

				return;

			}

			Optional <Long> parentIdOptional =
				requestContext.stuffInteger (
					parentHelper.idKey ());

			if (
				optionalIsPresent (
					parentIdOptional)
			) {

				// use specific parent

				parent =
					parentHelper.findRequired (
						transaction,
						optionalGetRequired (
							parentIdOptional));

				return;

			}

		}

	}

	/*
	void prepareFieldSet (
			@NonNull TaskLogger parentTaskLogger) {

		formFieldSet =
			formFieldsProvider.getFieldsForObject (
				parentTaskLogger,
				object);

	}
	*/

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContent");

		) {

			htmlTableOpenDetails ();

			formContext.outputTableRows (
				transaction);

			htmlTableClose ();

		}

	}

}
