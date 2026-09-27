package com.fakeskills;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("fakeskills")
public interface FakeSkillsConfig extends Config
{
	@ConfigSection(
			name = "Fake Skills",
			description = "Check the skills you want to enable to begin earning Fake XP.",
			position = 0
	)
	String fakeSkillsSection = "fakeSkills";

	@ConfigItem(
			keyName = "bankStandingEnabled",
			name = "Bank Standing",
			description = "Enable Bank Standing XP.",
			position = 1,
			section = fakeSkillsSection
	)
	default boolean bankStandingEnabled()
	{
		return false;
	}

	@ConfigItem(
			keyName = "travelingEnabled",
			name = "Traveling",
			description = "Enable Traveling XP.",
			position = 2,
			section = fakeSkillsSection
	)
	default boolean travelingEnabled()
	{
		return false;
	}

	@ConfigItem(
			keyName = "yappingEnabled",
			name = "Yapping",
			description = "Enable Yapping XP.",
			position = 3,
			section = fakeSkillsSection
	)
	default boolean yappingEnabled()
	{
		return false;
	}

	@ConfigItem(
			keyName = "doublingDownEnabled",
			name = "Doubling Down",
			description = "Enable Doubling Down XP.",
			position = 4,
			section = fakeSkillsSection
	)
	default boolean doublingDownEnabled()
	{
		return false;
	}

	@ConfigItem(
			keyName = "prayingEnabled",
			name = "Praying",
			description = "Enable Praying XP.",
			position = 5,
			section = fakeSkillsSection
	)
	default boolean prayingEnabled()
	{
		return false;
	}

	@ConfigItem(
			keyName = "expWasteEnabled",
			name = "Exp Waste",
			description = "Enable Exp Waste XP.",
			position = 6,
			section = fakeSkillsSection
	)
	default boolean expWasteEnabled()
	{
		return false;
	}

	@ConfigSection(
			name = "Game Mode",
			description = "Optional Fake Skills game mode settings.",
			position = 1
	)
	String gameModeSection = "gameMode";

	@ConfigItem(
			keyName = "ironmanMode",
			name = "Ironman",
			description = "Removes the Traveling follower bonus and doubles Yapping XP.",
			position = 1,
			section = gameModeSection
	)
	default boolean ironmanMode()
	{
		return false;
	}
}