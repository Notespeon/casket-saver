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

import com.google.inject.Provides;
import java.time.temporal.ChronoUnit;
import java.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.Timer;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static net.runelite.api.ItemID.*;

@Slf4j
@PluginDescriptor(
	name = "CasketSaver"
)
public class CasketSaverPlugin extends Plugin
{
	public ArrayList<Integer> casketIds = new ArrayList<Integer>()
	{{
		add(REWARD_CASKET_EASY);
		add(REWARD_CASKET_MEDIUM);
		add(REWARD_CASKET_HARD);
		add(REWARD_CASKET_ELITE);
	}};

	@Inject
	private Client client;

	@Inject
	ItemManager itemManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private CasketSaverConfig config;
	private ItemContainer bankContainer;
	private boolean hasMasterClueInventory = false;
	private boolean hasMasterClueBanked = false;
	private boolean hasMasterClueReward = false;
	private boolean hasMasterClueCooldown = false;
	private int casketCooldown;
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INVENTORY.getId() )
		{
			bankContainer = client.getItemContainer(InventoryID.BANK);

			if (bankContainer != null)
			{
				hasMasterClueBanked = false;
				Arrays.stream(bankContainer.getItems())
						.forEach(item ->
						{
							if (!hasMasterClueBanked && item.getId() == CLUE_SCROLL_MASTER)
							{
								hasMasterClueBanked = true;
							}
						});
			}

			hasMasterClueInventory = false;
			Arrays.stream(event.getItemContainer().getItems())
					.forEach(item ->
					{
						if (!hasMasterClueInventory && item.getId() == CLUE_SCROLL_MASTER)
						{
							hasMasterClueInventory = true;
							hasMasterClueBanked = false;
							hasMasterClueReward = false;
						}
					});

		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.CLUESCROLL_REWARD)
		{
			final Widget clueScrollReward = client.getWidget(ComponentID.CLUESCROLL_REWARD_ITEM_CONTAINER);

			for (Widget widget : Objects.requireNonNull(clueScrollReward.getChildren()))
			{
				if (widget.getItemId() == CLUE_SCROLL_MASTER)
				{
					hasMasterClueReward = true;
					if (config.masterCooldown())
					{
						startMasterCooldown();
					}
				}
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.isItemOp() && casketIds.contains(event.getItemId()) && event.getMenuOption().equals("Open"))
		{
			if (hasMasterClue())
			{
				saveCasket(event);
			}

			if (config.casketCooldown())
			{
				if (casketCooldown == 0)
				{
					casketCooldown = 1;
				}
				else
				{
					event.consume();
				}
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (config.casketCooldown())
		{
			casketCooldown = 0;
		}
	}

	@Provides
	CasketSaverConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CasketSaverConfig.class);
	}

	private void saveCasket(MenuOptionClicked event)
	{
		if ((event.getItemId() == REWARD_CASKET_EASY && config.easyMode())
				|| (event.getItemId() == REWARD_CASKET_MEDIUM && config.mediumMode())
				|| (event.getItemId() == REWARD_CASKET_HARD && config.hardMode())
				|| (event.getItemId() == REWARD_CASKET_ELITE && config.eliteMode()))
		{
			event.consume();
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Casket Saver prevents you from opening this casket.", "");
		}
	}

	private boolean hasMasterClue()
	{
		return hasMasterClueBanked || hasMasterClueInventory || hasMasterClueReward || hasMasterClueCooldown;
	}

	private void startMasterCooldown()
	{
		Timer timer = new Timer(30, ChronoUnit.SECONDS, itemManager.getImage(ItemID.CLUE_SCROLL_MASTER), this);
		infoBoxManager.addInfoBox(timer);

		hasMasterClueCooldown = true;
		java.util.Timer t = new java.util.Timer();
		TimerTask task = new TimerTask()
		{
			@Override
			public void run()
			{
				hasMasterClueCooldown = false;
			}
		};

		t.schedule(task, 30000);
	}
}
