package wbs.console.helper;

import wbs.console.forms.EntityFinder;
import wbs.console.lookup.ObjectLookup;
import wbs.framework.entity.model.ModelMethods;
import wbs.framework.object.ObjectHelperMethods;
import wbs.framework.record.Record;

public
interface ConsoleHelper<ObjectType extends Record<ObjectType>>
	extends
		ConsoleHelperMethods<ObjectType>,
		EntityFinder<ObjectType>,
		ObjectHelperMethods<ObjectType>,
		ObjectLookup<ObjectType>,
		ModelMethods {

}