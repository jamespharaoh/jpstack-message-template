package wbs.framework.object;

import static wbs.utils.etc.Misc.doNothing;
import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalFromNullable;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.ResultUtils.errorResult;
import static wbs.utils.etc.ResultUtils.errorResultFormat;
import static wbs.utils.etc.ResultUtils.getError;
import static wbs.utils.etc.ResultUtils.isError;
import static wbs.utils.etc.ResultUtils.resultValueRequired;
import static wbs.utils.etc.ResultUtils.successResult;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.etc.TypeUtils.isNotInstanceOf;
import static wbs.utils.string.StringUtils.stringFormat;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.annotations.WeakSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.utils.etc.PropertyUtils;

import fj.data.Either;

@Accessors (fluent = true)
@PrototypeComponent ("objectHelperPropertyImplementation")
public
class ObjectHelperPropertyImplementation <
	RecordType extends Record <RecordType>
>
	implements
		ObjectHelperComponent <RecordType>,
		ObjectHelperPropertyMethods <RecordType> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@WeakSingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	ObjectTypeRegistry objectTypeRegistry;

	// properties

	@Getter @Setter
	ObjectModel <RecordType> objectModel;

	@Getter @Setter
	ObjectHelper <RecordType> objectHelper;

	@Getter @Setter
	ObjectDatabaseHelper <RecordType> objectDatabaseHelper;

	// public implementation

	@Override
	public
	String getName (
			@NonNull RecordType object) {

		return objectModel.getName (
			object);

	}

	@Override
	public
	String getTypeCode (
			@NonNull RecordType object) {

		return objectModel.getTypeCode (
			object);

	}

	@Override
	public
	String getCode (
			@NonNull RecordType object) {

		return objectModel.getCode (
			object);

	}

	@Override
	public
	String getDescription (
			@NonNull RecordType object) {

		return objectModel.getDescription (
			object);

	}

	@Override
	public
	Record <?> getParentType (
			@NonNull RecordType object) {

		return objectModel.getParentType (
			object);

	}

	@Override
	public
	Long getParentTypeId (
			@NonNull RecordType object) {

		if (
			! objectModel.objectClass ().isInstance (
				object)
		) {

			throw new IllegalArgumentException ();

		} else if (objectModel.isRoot ()) {

			throw new UnsupportedOperationException ();

		} else if (objectModel.parentTypeId () != null) {

			return objectModel.parentTypeId ();

		} else {

			Record <?> parentType =
				objectModel.getParentType (
					object);

			return parentType.getId ();

		}

	}

	@Override
	public
	Long getParentId (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getParentId");

		) {

			if (
				isNotInstanceOf (
					objectModel.objectClass (),
					object)
			) {

				throw new IllegalArgumentException ();

			} else if (objectModel.isRoot ()) {

				throw new UnsupportedOperationException ();

			} else if (objectModel.isRooted ()) {

				return 0l;

			} else if (objectModel.canGetParent ()) {

				Record <?> parent =
					getParentRequired (
						transaction,
						object);

				return parent.getId ();

			} else {

				return (Long)
					PropertyUtils.propertyGetAuto (
						object,
						objectModel.parentIdField ().name ());

			}

		}

	}

	@Override
	public
	void setParent (
			@NonNull RecordType object,
			@NonNull Record <?> parent) {

		PropertyUtils.propertySetAuto (
			object,
			objectModel.parentField ().name (),
			parent);

		// TODO grand parent etc

	}

	@Override
	public
	GlobalId getParentGlobalId (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getParentGlobalId");

		) {

			if (objectModel.isRoot ()) {

				return null;

			} else {

				return new GlobalId (
					getParentTypeId (
						object),
					getParentId (
						transaction,
						object));

			}

		}

	}

	@Override
	public
	Either <Optional <Record <?>>, String> getParentOrError (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getParentOrError");

		) {

			if (objectModel.isRoot ()) {

				return successResult (
					optionalAbsent ());

			} else if (objectModel.isRooted ()) {

				ObjectHelper <?> rootHelper =
					objectManager.objectHelperForClassRequired (
						objectTypeRegistry.rootRecordClass ());

				return successResult (
					optionalOf (
						rootHelper.findRequired (
							transaction,
							0l)));

			} else if (objectModel.canGetParent ()) {

				return successResult (
					optionalFromNullable (
						objectModel.getParentOrNull (
							object)));

			} else {

				Record <?> parentObjectType =
					(Record <?>)
					objectModel.getParentType (
						object);

				Long parentObjectId =
					objectModel.getParentId (
						object);

				if (
					isNull (
						parentObjectId)
				) {

					return errorResultFormat (
						"Failed to get parent id of %s with id %s",
						objectModel.objectName (),
						integerToDecimalString (
							object.getId ()));

				}

				Optional <ObjectHelper <?>> parentHelperOptional =
					objectManager.objectHelperForTypeId (
						parentObjectType.getId ());

				if (
					optionalIsNotPresent (
						parentHelperOptional)
				) {

					return errorResult (
						stringFormat (
							"No object helper provider for %s, ",
							integerToDecimalString (
								parentObjectType.getId ()),
							"parent of %s (%s)",
							objectModel.objectName (),
							integerToDecimalString (
								object.getId ())));

				}

				ObjectHelper <?> parentHelper =
					optionalGetRequired (
						parentHelperOptional);

				Optional <Record <?>> parentOptional =
					genericCastUnchecked (
						parentHelper.find (
							transaction,
							parentObjectId));

				if (
					optionalIsNotPresent (
						parentOptional)
				) {

					return errorResultFormat (
						"Can't find %s with id %s",
						parentHelper.objectName (),
						integerToDecimalString (
						parentObjectId));

				}

				return successResult (
					parentOptional);

			}

		}

	}

	@Override
	public
	GlobalId getGlobalId (
			@NonNull RecordType object) {

		return new GlobalId (
			objectModel.objectTypeId (),
			object.getId ());

	}

	@Override
	public
	Either <Boolean, String> getDeletedOrError (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object,
			@NonNull Boolean checkParents) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getDeletedOrError");

		) {

			Record <?> currentObject =
				object;

			ObjectHelper <?> currentHelper =
				objectHelper;

			for (;;) {

				// root is never deleted

				if (currentHelper.isRoot ()) {

					return successResult (
						false);

				}

				// check our deleted flag

				try {

					Boolean deletedProperty =
						genericCastUnchecked (
							PropertyUtils.propertyGetAuto (
								currentObject,
								"deleted"));

					if (deletedProperty) {

						return successResult (
							true);

					}

				} catch (Exception exception) {

					doNothing ();

				}

				// try parent

				if (! checkParents) {

					return successResult (
						false);

				}

				Either <Optional <Record <?>>, String> nextObjectOrError =
					currentHelper.getParentOrError (
						transaction,
						genericCastUnchecked (
							currentObject));

				if (
					isError (
						nextObjectOrError)
				) {

					return errorResult (
						getError (
							nextObjectOrError));

				}

				Optional <Record <?>> nextObjectOptional =
					resultValueRequired (
						nextObjectOrError);

				if (
					optionalIsNotPresent (
						nextObjectOptional)
				) {

					return errorResultFormat (
						"Unable to find parent for %s with id %s",
						currentHelper.objectName (),
						integerToDecimalString (
							object.getId ()));

				}

				currentObject =
					optionalGetRequired (
						nextObjectOptional);

				currentHelper =
					objectManager.objectHelperForObjectRequired (
						currentObject);

			}

		}

	}

	@Override
	public
	Object getDynamic (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object,
			@NonNull String name) {

		return objectModel.hooks ().getDynamic (
			parentTransaction,
			object,
			name);

	}

	@Override
	public
	Optional <String> setDynamic (
			@NonNull Transaction parentTransaction,
			@NonNull RecordType object,
			@NonNull String name,
			@NonNull Optional <?> valueOptional) {

		return objectModel.hooks ().setDynamic (
			parentTransaction,
			object,
			name,
			valueOptional);

	}

}
