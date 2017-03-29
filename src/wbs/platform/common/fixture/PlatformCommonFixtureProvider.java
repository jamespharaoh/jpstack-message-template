package wbs.platform.common.fixture;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.fixtures.FixtureProvider;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.menu.model.MenuGroupObjectHelper;
import wbs.platform.menu.model.MenuGroupRec;
import wbs.platform.menu.model.MenuItemObjectHelper;
import wbs.platform.scaffold.model.SliceObjectHelper;

@PrototypeComponent ("platformCommonFixtureProvider")
public
class PlatformCommonFixtureProvider
	implements FixtureProvider {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MenuGroupObjectHelper menuGroupHelper;

	@SingletonDependency
	MenuItemObjectHelper menuItemHelper;

	@SingletonDependency
	SliceObjectHelper sliceHelper;

	// implementation

	@Override
	public
	void createFixtures (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"createFixtures");

		menuGroupHelper.insert (
			taskLogger,
			menuGroupHelper.createInstance ()

			.setSlice (
				sliceHelper.findByCodeRequired (
					GlobalId.root,
					"test"))

			.setCode (
				"internal")

			.setName (
				"Internal")

			.setDescription (
				"")

			.setLabel (
				"Internals")

			.setOrder (
				70l)

		);

		MenuGroupRec systemMenuGroup =
			menuGroupHelper.insert (
				taskLogger,
				menuGroupHelper.createInstance ()

			.setSlice (
				sliceHelper.findByCodeRequired (
					GlobalId.root,
					"test"))

			.setCode (
				"system")

			.setName (
				"System")

			.setDescription (
				"")

			.setLabel (
				"System")

			.setOrder (
				50l)

		);

		menuItemHelper.insert (
			taskLogger,
			menuItemHelper.createInstance ()

			.setMenuGroup (
				systemMenuGroup)

			.setCode (
				"slice")

			.setName (
				"Slice")

			.setDescription (
				"")

			.setLabel (
				"Slice")

			.setTargetPath (
				"/slices")

			.setTargetFrame (
				"main")

		);

		menuItemHelper.insert (
			taskLogger,
			menuItemHelper.createInstance ()

			.setMenuGroup (
				systemMenuGroup)

			.setCode (
				"menu")

			.setName (
				"Menu")

			.setDescription (
				"")

			.setLabel (
				"Menu")

			.setTargetPath (
				"/menuGroups")

			.setTargetFrame (
				"main")

		);

	}

}
