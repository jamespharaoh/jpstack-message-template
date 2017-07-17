package wbs.integrations.oxygenate.api;

import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.string.StringUtils.stringFormat;

import lombok.NonNull;

import wbs.api.mvc.ApiLoggingAction;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.integrations.oxygenate.model.OxygenateInboundLogObjectHelper;
import wbs.integrations.oxygenate.model.OxygenateInboundLogType;
import wbs.integrations.oxygenate.model.OxygenateReportCodeObjectHelper;
import wbs.integrations.oxygenate.model.OxygenateReportCodeRec;
import wbs.integrations.oxygenate.model.OxygenateRouteOutObjectHelper;
import wbs.integrations.oxygenate.model.OxygenateRouteOutRec;

import wbs.sms.message.report.logic.SmsDeliveryReportLogic;
import wbs.sms.route.core.model.RouteObjectHelper;
import wbs.sms.route.core.model.RouteRec;

import wbs.utils.string.FormatWriter;

import wbs.web.context.RequestContext;
import wbs.web.responder.TextResponder;
import wbs.web.responder.WebResponder;

@PrototypeComponent ("oxygenateRouteReportApiLoggingAction")
public
class OxygenateRouteReportApiLoggingAction
	implements ApiLoggingAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	OxygenateInboundLogObjectHelper oxygenateInboundLogHelper;

	@SingletonDependency
	OxygenateReportCodeObjectHelper oxygenateReportCodeHelper;

	@SingletonDependency
	OxygenateRouteOutObjectHelper oxygenateRouteOutCodeHelper;

	@SingletonDependency
	SmsDeliveryReportLogic reportLogic;

	@SingletonDependency
	RequestContext requestContext;

	@SingletonDependency
	RouteObjectHelper smsRouteHelper;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <TextResponder> textResponderProvider;

	// state

	Long smsRouteId;

	String reference;
	String status;

	Boolean success = false;

	// implementation

	@Override
	public
	void processRequest (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull FormatWriter debugWriter) {

		smsRouteId =
			parseIntegerRequired (
				requestContext.requestStringRequired (
					"smsRouteId"));

		reference =
			requestContext.parameterRequired (
				"Reference");

		status =
			requestContext.parameterRequired (
				"Status");

	}

	@Override
	public
	void updateDatabase (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"updateDatabase");

		) {

			OxygenateRouteOutRec routeOut =
				oxygenateRouteOutCodeHelper.findRequired (
					transaction,
					smsRouteId);

			if (! routeOut.getRoute ().getDeliveryReports ()) {

				throw new RuntimeException (
					stringFormat (
						"Delivery reports are not enabled for route %s.%s",
						routeOut.getRoute ().getSlice ().getCode (),
						routeOut.getRoute ().getCode ()));

			}

			OxygenateReportCodeRec reportCode =
				oxygenateReportCodeHelper.findByCodeRequired (
					transaction,
					routeOut.getOxygenateConfig (),
					status);

			RouteRec route =
				smsRouteHelper.findRequired (
					transaction,
					smsRouteId);

			reportLogic.deliveryReport (
				transaction,
				route,
				reference,
				reportCode.getMessageStatus (),
				optionalOf (
					status),
				optionalOf (
					reportCode.getDescription ()),
				optionalAbsent (),
				optionalAbsent ());

			transaction.commit ();

			success = true;

		}

	}

	@Override
	public
	WebResponder createResponse (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull FormatWriter debugWriter) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"createResponse");

		) {

			// encode response

			String responseString = "SUCCESS\n";

			// write to debug log

			debugWriter.writeString (
				"== RESPONSE BODY ==\n\n");

			debugWriter.writeString (
				responseString);

			debugWriter.writeString (
				"\n\n");

			// create responder

			return textResponderProvider.provide (
				taskLogger,
				textResponder ->
					textResponder

				.contentType (
					"text/plain")

				.text (
					responseString)

			);

		}

	}

	@Override
	public
	void storeLog (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull String debugLog) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"storeLog");

		) {

			oxygenateInboundLogHelper.insert (
				transaction,
				oxygenateInboundLogHelper.createInstance ()

				.setRoute (
					smsRouteHelper.findRequired (
						transaction,
						smsRouteId))

				.setType (
					OxygenateInboundLogType.smsDelivery)

				.setTimestamp (
					transaction.now ())

				.setDetails (
					debugLog)

				.setSuccess (
					success)

			);

			transaction.commit ();

		}

	}

}
