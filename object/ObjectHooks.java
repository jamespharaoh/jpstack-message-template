package wbs.framework.object;

import static wbs.utils.collection.CollectionUtils.emptyList;
import static wbs.utils.etc.Misc.doNothing;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.List;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;

import wbs.utils.data.Pair;

public
interface ObjectHooks <RecordType extends Record <RecordType>> {

	default
	void createSingletons (
			@NonNull Transaction parentTransaction,
			@NonNull ObjectHelper <RecordType> objectHelper,
			@NonNull ObjectHelper <?> parentHelper,
			@NonNull Record <?> parentObject) {

		doNothing ();

	}

	default
	void beforeInsert (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object) {

		doNothing ();

	}

	default
	void afterInsert (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object) {

		doNothing ();

	}

	default
	void beforeUpdate (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object) {

		doNothing ();

	}

	default
	List <Pair <Record <?>, String>> verifyData (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object,
			@NonNull Boolean recurse) {

		return emptyList ();

	}

	default
	Object getDynamic (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object,
			@NonNull String name) {

		throw new UnsupportedOperationException (
			stringFormat (
				"%s.getDynamic (...)",
				getClass ().getSimpleName ()));

	}

	default
	Optional <String> setDynamic (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object,
			@NonNull String name,
			@NonNull Optional <?> value) {

		throw new UnsupportedOperationException (
			stringFormat (
				"%s.setDynamic (...)",
				getClass ().getSimpleName ()));

	}

	public
	class DefaultImplementation <
		RecordType extends Record <RecordType>
	>
		implements ObjectHooks <RecordType> {

	}

}
