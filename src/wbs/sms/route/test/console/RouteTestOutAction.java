package wbs.sms.route.test.console;

import static wbs.framework.utils.etc.StringUtils.stringFormat;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.Cleanup;

import com.google.common.collect.ImmutableMap;

import wbs.console.action.ConsoleAction;
import wbs.console.helper.ConsoleObjectManager;
import wbs.console.param.ParamChecker;
import wbs.console.param.ParamCheckerSet;
import wbs.console.param.RegexpParamChecker;
import wbs.console.request.ConsoleRequestContext;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.record.GlobalId;
import wbs.framework.web.Responder;
import wbs.platform.service.console.ServiceConsoleHelper;
import wbs.platform.service.model.ServiceRec;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.outbox.logic.MessageSender;
import wbs.sms.number.core.model.NumberObjectHelper;
import wbs.sms.number.core.model.NumberRec;
import wbs.sms.route.core.console.RouteConsoleHelper;
import wbs.sms.route.core.model.RouteRec;

@PrototypeComponent ("routeTestOutAction")
public
class RouteTestOutAction
	extends ConsoleAction {

	// dependencies

	@Inject
	ConsoleObjectManager objectManager;

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	Database database;

	@Inject
	NumberObjectHelper numberHelper;

	@Inject
	RouteConsoleHelper routeHelper;

	@Inject
	ServiceConsoleHelper serviceHelper;

	// prototype dependencies

	@Inject
	Provider<MessageSender> messageSender;

	// dependencies

	@Override
	public
	Responder backupResponder () {

		return responder (
			"routeTestOutResponder");

	}

	// implementation

	@Override
	public
	Responder goReal () {

		MessageRec message = null;

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"RouteTestOutAction.goReal ()",
				this);

		int routeId =
			requestContext.stuffInt (
				"routeId");

		RouteRec route =
			routeHelper.findRequired (
				routeId);

		// check params

		Map<String,Object> params =
			paramsChecker.apply (
				requestContext);

		if (params == null) {
			throw new RuntimeException ();
		}

		// get params

		NumberRec number =
			numberHelper.findOrCreate (
				(String) params.get ("num_to"));

		ServiceRec testService =
			serviceHelper.findByCodeRequired (
				GlobalId.root,
				"test");

		message =
			messageSender.get ()

			.number (
				number)

			.messageString (
				requestContext.parameterRequired (
					"message"))

			.numFrom (
				requestContext.parameterRequired (
					"num_from"))

			.route (
				route)

			.service (
				testService)

			.send ();

		transaction.commit ();

		if (message != null) {

			requestContext.addNotice (
				stringFormat (
					"Message %s inserted",
					message.getId ()));

		}

		return null;

	}

	static
	ParamCheckerSet paramsChecker =
		new ParamCheckerSet (
			new ImmutableMap.Builder<String,ParamChecker<?>> ()

				.put (
					"num_to",
					new RegexpParamChecker (
						"Please enter a valid destination number",
						"\\d+"))

				.build ());

}
