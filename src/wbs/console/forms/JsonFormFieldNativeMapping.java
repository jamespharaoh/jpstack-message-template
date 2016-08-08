package wbs.console.forms;

import lombok.NonNull;
import lombok.experimental.Accessors;

import org.json.simple.JSONValue;

import com.google.common.base.Optional;

import wbs.framework.application.annotations.PrototypeComponent;
import static wbs.framework.utils.etc.OptionalUtils.isNotPresent;

@Accessors (fluent = true)
@PrototypeComponent ("jsonFormFieldNativeMapping")
public
class JsonFormFieldNativeMapping<Container>
	implements FormFieldNativeMapping<Container,Object,String> {

	@Override
	public
	Optional<Object> nativeToGeneric (
			@NonNull Container container,
			@NonNull Optional<String> nativeValue) {

		if (
			isNotPresent (
				nativeValue)
		) {
			return Optional.absent ();
		}

		return Optional.of (
			JSONValue.parse (
				nativeValue.get ()));

	}

	@Override
	public
	Optional<String> genericToNative (
			@NonNull Container container,
			@NonNull Optional<Object> genericValue) {

		if (
			isNotPresent (
				genericValue)
		) {
			return Optional.absent ();
		}

		return Optional.of (
			JSONValue.toJSONString (
				genericValue.get ()));

	}

}