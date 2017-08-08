package wbs.framework.database;

import wbs.framework.logging.TaskLogger;

public
interface Transaction
	extends
		TaskLogger,
		TransactionMethods {

	default
	void commit () {

		ownedTransaction ().commit (
			this);

	}

}
