package wbs.platform.queue.fixture;

import static wbs.framework.utils.etc.CodeUtils.simplifyToCodeRequired;
import static wbs.framework.utils.etc.Misc.camelToUnderscore;
import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.sql.SQLException;

import javax.inject.Inject;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.entity.helper.EntityHelper;
import wbs.framework.entity.meta.ModelMetaBuilderHandler;
import wbs.framework.entity.meta.ModelMetaSpec;
import wbs.framework.entity.model.Model;
import wbs.framework.record.GlobalId;
import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.queue.metamodel.QueueTypeSpec;
import wbs.platform.queue.model.QueueTypeObjectHelper;

@Log4j
@PrototypeComponent ("queueTypeBuilder")
@ModelMetaBuilderHandler
public
class QueueTypeBuilder {

	// dependencies

	@Inject
	Database database;

	@Inject
	EntityHelper entityHelper;

	@Inject
	ObjectTypeObjectHelper objectTypeHelper;

	@Inject
	QueueTypeObjectHelper queueTypeHelper;

	// builder

	@BuilderParent
	ModelMetaSpec parent;

	@BuilderSource
	QueueTypeSpec spec;

	@BuilderTarget
	Model model;

	// build

	@BuildMethod
	public
	void build (
			@NonNull Builder builder) {

		try {

			log.info (
				stringFormat (
					"Create queue type %s.%s",
					camelToUnderscore (
						ifNull (
							spec.subject (),
							parent.name ())),
					simplifyToCodeRequired (
						spec.name ())));

			createQueueType ();

		} catch (Exception exception) {

			throw new RuntimeException (
				stringFormat (
					"Error creating queue type %s.%s",
					camelToUnderscore (
						ifNull (
							spec.subject (),
							parent.name ())),
					simplifyToCodeRequired (
						spec.name ())),
				exception);

		}

	}

	private
	void createQueueType ()
		throws SQLException {

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				this);

		// lookup parent type

		String parentTypeCode =
			camelToUnderscore (
				ifNull (
					spec.parent (),
					parent.name ()));

		ObjectTypeRec parentType =
			objectTypeHelper.findByCode (
				GlobalId.root,
				parentTypeCode);

		// lookup subject type

		String subjectTypeCode =
			camelToUnderscore (
				spec.subject ());

		ObjectTypeRec subjectType =
			objectTypeHelper.findByCode (
				GlobalId.root,
				subjectTypeCode);

		// lookup ref type

		String refTypeCode =
			camelToUnderscore (
				spec.ref ());

		ObjectTypeRec refType =
			objectTypeHelper.findByCode (
				GlobalId.root,
				refTypeCode);

		// create queue type

		queueTypeHelper.insert (
			queueTypeHelper.createInstance ()

			.setParentType (
				parentType)

			.setCode (
				simplifyToCodeRequired (
					spec.name ()))

			.setDescription (
				spec.description ())

			.setSubjectType (
				subjectType)

			.setRefType (
				refType)

		);

		// commit transaction

		transaction.commit ();

	}

}
