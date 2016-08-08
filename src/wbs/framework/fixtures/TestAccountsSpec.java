package wbs.framework.fixtures;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("test-accounts")
public
class TestAccountsSpec {

	@DataChildren (
		direct = true)
	List<TestAccountSpec> accounts =
		new ArrayList<TestAccountSpec> ();

}