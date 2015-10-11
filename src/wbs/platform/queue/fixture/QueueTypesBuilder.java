package wbs.platform.queue.fixture;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.entity.meta.ModelMetaBuilderHandler;
import wbs.framework.entity.meta.ModelMetaSpec;
import wbs.framework.entity.model.Model;
import wbs.platform.queue.metamodel.QueueTypesSpec;

@PrototypeComponent ("queueTypesBuilder")
@ModelMetaBuilderHandler
public
class QueueTypesBuilder {

	// builder

	@BuilderParent
	ModelMetaSpec parent;

	@BuilderSource
	QueueTypesSpec spec;

	@BuilderTarget
	Model model;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		builder.descend (
			parent,
			spec.queueTypes (),
			model);

	}

}