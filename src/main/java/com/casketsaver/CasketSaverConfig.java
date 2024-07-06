package com.casketsaver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface CasketSaverConfig extends Config
{
	@ConfigItem(
		keyName = "easy",
		name = "Easy Clues",
		description = "Easy Caskets will be use only when master clue present"
	)
	default boolean easyMode() {
		return true;
	}

	@ConfigItem(
			keyName = "medium",
			name = "Medium Clues",
			description = "Medium Caskets will be use only when master clue present"
	)
	default boolean mediumMode() {
		return true;
	}

	@ConfigItem(
			keyName = "hard",
			name = "Hard Clues",
			description = "Hard Caskets will be use only when master clue present"
	)
	default boolean hardMode() {
		return true;
	}

	@ConfigItem(
			keyName = "elite",
			name = "Elite Clues",
			description = "Elite Caskets will be use only when master clue present"
	)
	default boolean eliteMode() {
		return true;
	}

	@ConfigItem(
			keyName = "casketCooldown",
			name = "Add open cooldown",
			description = "Add a cooldown to opening Caskets so you can spam click."
	)
	default boolean casketCooldown() {
		return false;
	}
}
