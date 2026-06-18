package com.doomunique;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(DoomUniqueConfig.GROUP)
public interface DoomUniqueConfig extends Config
{
	String GROUP = "doomunique";
	int DOM_DESCEND_HOLE_UNIQUE = 50940;

	@ConfigSection(
		name = "Colors",
		description = "Hole recolor settings",
		position = 0
	)
	String colorsSection = "colors";

	@ConfigItem(
		keyName = "uniqueHoleColor",
		name = "Unique hole color",
		description = "Color to apply to the special-loot hole",
		position = 0,
		section = colorsSection
	)
	default Color uniqueHoleColor()
	{
		return Color.decode("#DA56F5");
	}

	@ConfigItem(
		keyName = "recolorUniqueHole",
		name = "Recolor unique hole",
		description = "Recolor Doom's special-loot hole object",
		position = 1,
		section = colorsSection
	)
	default boolean recolorUniqueHole()
	{
		return true;
	}
}
