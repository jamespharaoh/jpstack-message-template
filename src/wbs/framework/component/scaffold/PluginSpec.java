package wbs.framework.component.scaffold;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.apache.commons.lang3.builder.CompareToBuilder;

import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChild;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataChildrenIndex;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.data.annotations.DataIgnore;
import wbs.framework.data.annotations.DataParent;

@Accessors (fluent = true)
@DataClass ("plugin")
public
class PluginSpec
	implements Comparable <PluginSpec> {

	// attributes

	@DataParent
	@Getter @Setter
	BuildSpec build;

	@DataAttribute (
		required = true)
	@Getter @Setter
	String name;

	@DataAttribute (
		name = "package",
		required = true)
	@Getter @Setter
	String packageName;

	@DataChildren
	@Getter @Setter
	List<PluginDependencySpec> pluginDependencies =
		new ArrayList<PluginDependencySpec> ();

	@DataChild
	@Getter @Setter
	PluginModelsSpec models =
		new PluginModelsSpec ();

	@DataChildren
	@Getter @Setter
	List <PluginFixtureSpec> fixtures =
		new ArrayList<> ();

	@DataIgnore
	Object sqlScripts;

	@DataChildren (
		direct = true,
		childElement = "layer")
	@Getter @Setter
	List <PluginLayerSpec> layers =
		new ArrayList<> ();

	@DataChildrenIndex
	@Getter @Setter
	Map <String, PluginLayerSpec> layersByName =
		new HashMap<> ();

	@DataChildren
	@Getter @Setter
	List <PluginApiModuleSpec> apiModules =
		new ArrayList<> ();

	@DataChildren
	@Getter @Setter
	List <PluginConsoleModuleSpec> consoleModules =
		new ArrayList<> ();

	// implementation

	@Override
	public
	int compareTo (
			PluginSpec other) {

		return new CompareToBuilder ()

			.append (
				name (),
				other.name ())

			.toComparison ();

	}

}
