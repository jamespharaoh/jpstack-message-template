package wbs.framework.object;

import wbs.framework.entity.record.Record;

public
interface ObjectHelper <RecordType extends Record <RecordType>>
	extends
		ObjectHelperMethods <RecordType>,
		ObjectModelMethods <RecordType> {

}
