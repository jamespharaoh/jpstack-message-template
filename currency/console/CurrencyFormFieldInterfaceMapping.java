package wbs.platform.currency.console;

import static wbs.framework.utils.etc.Misc.errorResult;
import static wbs.framework.utils.etc.Misc.isNotPresent;
import static wbs.framework.utils.etc.Misc.isNull;
import static wbs.framework.utils.etc.Misc.stringFormat;
import static wbs.framework.utils.etc.Misc.successResult;

import javax.inject.Inject;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.google.common.base.Optional;

import fj.data.Either;

import wbs.console.forms.FormFieldInterfaceMapping;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.object.ObjectManager;
import wbs.platform.currency.logic.CurrencyLogic;
import wbs.platform.currency.model.CurrencyRec;

@Accessors (fluent = true)
@PrototypeComponent ("currencyFormFieldInterfaceMapping")
public
class CurrencyFormFieldInterfaceMapping<Container>
	implements FormFieldInterfaceMapping<Container,Long,String> {

	// dependencies

	@Inject
	CurrencyLogic currencyLogic;

	@Inject
	ObjectManager objectManager;

	// properties

	@Getter @Setter
	String currencyPath;

	@Getter @Setter
	Boolean blankIfZero = false;

	// implementation

	@Override
	public
	Either<Optional<Long>,String> interfaceToGeneric (
			@NonNull Container container,
			@NonNull Optional<String> interfaceValue) {

		if (! interfaceValue.isPresent ()) {

			return successResult (
				Optional.<Long>absent ());

		}

		if (interfaceValue.get ().isEmpty ()) {

			return successResult (
				Optional.<Long>absent ());

		}

		CurrencyRec currency =
			(CurrencyRec)
			objectManager.dereference (
				container,
				currencyPath);

		if (
			isNull (
				currency)
		) {

			try {

				return successResult (
					Optional.of (
						Long.parseLong (
							interfaceValue.get ())));

			} catch (NumberFormatException exception) {

				return errorResult (
					"This currency value must be a whole number");

			}

		}

		Optional<Long> parseResult =
			currencyLogic.parseText (
				currency,
				interfaceValue.get ());

		if (
			isNotPresent (
				parseResult)
		) {

			return errorResult (
				stringFormat (
					"A currency value must be numeric and include the ",
					"appropriate decimal places"));

		}

		return successResult (
			Optional.of (
				currencyLogic.parseTextRequired (
					currency,
					interfaceValue.get ())));

	}

	@Override
	public
	Either<Optional<String>,String> genericToInterface (
			@NonNull Container container,
			@NonNull Optional<Long> genericValue) {

		if (
			isNotPresent (
				genericValue)
		) {

			return successResult (
				Optional.<String>absent ());

		}

		CurrencyRec currency =
			(CurrencyRec)
			objectManager.dereference (
				container,
				currencyPath);

		if (genericValue.get () == 0 && blankIfZero) {

			return successResult (
				Optional.of (
					""));

		}

		if (currency != null) {

			return successResult (
				Optional.of (
					currencyLogic.formatText (
						currency,
						genericValue.get ())));

		} else {

			return successResult (
				Optional.of (
					Long.toString (
						genericValue.get ())));

		}


	}

}
