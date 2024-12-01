/*
 * Copyright (c) 2024, Notespeon <https://github.com/Notespeon>
 * Copyright (c) 2024, TheLope <https://github.com/TheLope>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.casketsaver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("CasketSaver")
public interface CasketSaverConfig extends Config
{
	@ConfigItem(
		keyName = "casketCooldown",
		name = "Spamming cooldown",
		description = "Add a cooldown to the open option to help the plugin not miss master clues when spamming",
		position = 0
	)
	default boolean casketCooldown()
	{
		return false;
	}

	@ConfigItem(
		keyName = "easy",
		name = "Easy caskets",
		description = "Easy caskets cannot be opened while player is ineligible to receive a master clue",
		position = 1
	)
	default boolean easyMode()
	{
		return true;
	}

	@ConfigItem(
		keyName = "medium",
		name = "Medium caskets",
		description = "Medium caskets cannot be opened while player is ineligible to receive a master clue",
		position = 2
	)
	default boolean mediumMode()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hard",
		name = "Hard caskets",
		description = "Hard caskets cannot be opened while player is ineligible to receive a master clue",
		position = 3
	)
	default boolean hardMode()
	{
		return true;
	}

	@ConfigItem(
		keyName = "elite",
		name = "Elite caskets",
		description = "Elite caskets cannot be opened while player is ineligible to receive a master clue",
		position = 4
	)
	default boolean eliteMode()
	{
		return true;
	}

	@ConfigItem(
		keyName = "masterCooldown",
		name = "Master clue cooldown",
		description = "Caskets selected above cannot be opened until master clue cooldown is over",
		position = 5
	)
	default boolean masterCooldown()
	{
		return true;
	}

	@ConfigSection(name = "Overlays", description = "Options that effect overlays", position = 6)
	String overlaysSection = "Overlays";

	@ConfigItem(
		keyName = "showChatMessage",
		name = "Show chat message",
		description = "Show chat message indicating when caskets are being saved",
		section = overlaysSection,
		position = 0
	)
	default boolean showChatMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInfobox",
		name = "Show infobox",
		description = "Show infobox indicating when caskets are being saved",
		section = overlaysSection,
		position = 1
	)
	default boolean showInfobox()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showTooltip",
		name = "Show tooltip",
		description = "Show tooltip on casket \"Open\" hover when caskets are being saved",
		section = overlaysSection,
		position = 2
	)
	default boolean showTooltip()
	{
		return true;
	}
}
