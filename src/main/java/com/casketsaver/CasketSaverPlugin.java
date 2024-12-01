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
import java.awt.Color;
import java.time.temporal.ChronoUnit;
import java.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.Timer;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Objects;

import static net.runelite.api.ItemID.*;

@Slf4j
@PluginDescriptor(
	name = "CasketSaver"
)
public class CasketSaverPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	ItemManager itemManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	private InfoBox infoBox = null;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private CasketSaverOverlay infoOverlay;

	@Inject
	private CasketSaverConfig config;
	private MasterLocation masterLocation = MasterLocation.UNKNOWN;
	private boolean masterDeposited = false;
	private boolean masterDropped = false;
	private boolean hasMasterClueCooldown = false;
	private int casketCooldown;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(infoOverlay);
		clientThread.invoke(this::loadFromConfig);
	}

	@Override
	protected void shutDown() throws Exception
	{
		removeInfoBox();

		overlayManager.remove(infoOverlay);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
			clientThread.invoke(this::loadFromConfig);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INVENTORY.getId())
		{
			checkContainer(event.getItemContainer(), MasterLocation.INVENTORY);
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		// When bank interface is closed, check if existing master was deposited
		if (event.getGroupId() == InterfaceID.BANK && masterLocation.equals(MasterLocation.INVENTORY))
		{
			ItemContainer inventoryContainer = client.getItemContainer(InventoryID.INVENTORY);

			if (inventoryContainer != null)
			{
				checkContainer(inventoryContainer, MasterLocation.INVENTORY);
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// Check for banked clues when none have been located
		if (event.getGroupId() == InterfaceID.BANK && masterLocation.equals(MasterLocation.UNKNOWN))
		{
			ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);

			if (bankContainer != null)
			{
				checkContainer(bankContainer, MasterLocation.BANK);
			}
		}
		else if (event.getGroupId() == InterfaceID.CLUESCROLL_REWARD)
		{
			checkReward();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		// Track master interactions to detect banking edge cases
		if (event.getItemId() == ItemID.CLUE_SCROLL_MASTER)
		{
			masterDeposited = event.getMenuOption().contains("Deposit");
			masterDropped = event.getMenuOption().contains("Drop");
		}

		// Consume Casket Open events
		if (event.isItemOp() && isCasketToSave(event.getItemId()) && event.getMenuOption().equals("Open"))
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
		handleInfoBox();

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

	private void checkContainer(ItemContainer container, MasterLocation location)
	{
		if (Arrays.stream(container.getItems())
			.anyMatch(item -> item.getId() == CLUE_SCROLL_MASTER))
		{
			setMasterLocation(location);
		}
		// If master was previously located in container and not found, update location
		else if (masterLocation.equals(location))
		{
			// Check if it was banked or dropped
			if (location.equals(MasterLocation.INVENTORY) && (masterDeposited || !masterDropped))
			{
				setMasterLocation(MasterLocation.BANK);
			}
			else
			{
				setMasterLocation(MasterLocation.UNKNOWN);
			}
		}
	}

	private void checkReward()
	{
		final Widget clueScrollReward = client.getWidget(ComponentID.CLUESCROLL_REWARD_ITEM_CONTAINER);

		for (Widget widget : Objects.requireNonNull(clueScrollReward.getChildren()))
		{
			if (widget.getItemId() == CLUE_SCROLL_MASTER)
			{
				setMasterLocation(MasterLocation.REWARD);
				if (config.masterCooldown())
				{
					startMasterCooldown();
				}
			}
		}
	}

	public String getCause()
	{
		if (masterLocation.equals(MasterLocation.UNKNOWN))
		{
			return null;
		}

		StringBuilder savingCause = new StringBuilder()
			.append(ColorUtil.wrapWithColorTag("Casket Saver: ", Color.YELLOW))
			.append(ColorUtil.wrapWithColorTag("active", Color.GREEN))
			.append(ColorUtil.wrapWithColorTag("</br>Cause: ", Color.YELLOW))
			.append("Master clue ");
		if (hasMasterClueCooldown)
		{
			savingCause.append(ColorUtil.wrapWithColorTag("cooldown", Color.RED));
		}
		else if (masterLocation.equals(MasterLocation.BANK))
		{
			savingCause.append("in ").append(ColorUtil.wrapWithColorTag("bank", Color.RED));
		}
		else if (masterLocation.equals(MasterLocation.INVENTORY))
		{
			savingCause.append("in ").append(ColorUtil.wrapWithColorTag("inventory", Color.RED));
		}
		else if (masterLocation.equals(MasterLocation.REWARD))
		{
			savingCause.append("in ").append(ColorUtil.wrapWithColorTag("clue reward", Color.RED));
		}
		return savingCause.toString();
	}

	private void handleInfoBox()
	{
		var isShowing = infoBox != null;
		var shouldShow = config.showInfobox() && !masterLocation.equals(MasterLocation.UNKNOWN);

		if (isShowing && !shouldShow)
		{
			removeInfoBox();
		}
		else if (shouldShow)
		{
			if (!isShowing)
			{
				infoBox = new InfoBox(itemManager.getImage(CASKET), this)
				{
					@Override
					public String getText()
					{
						return "";
					}

					@Override
					public Color getTextColor()
					{
						return null;
					}
				};
			}

			infoBox.setTooltip(getCause());

			if (!isShowing)
			{
				infoBoxManager.addInfoBox(infoBox);
			}
		}
	}

	private boolean hasMasterClue()
	{
		return !masterLocation.equals(MasterLocation.UNKNOWN) || hasMasterClueCooldown;
	}

	public boolean isCasketToSave(Integer itemId)
	{
		return (itemId == REWARD_CASKET_EASY && config.easyMode())
			|| (itemId == REWARD_CASKET_MEDIUM && config.mediumMode())
			|| (itemId == REWARD_CASKET_HARD && config.hardMode())
			|| (itemId == REWARD_CASKET_ELITE && config.eliteMode());
	}

	private void loadFromConfig()
	{
		MasterLocation loadedMasterLocation = configManager.getRSProfileConfiguration("casketsaver", "masterLocation", MasterLocation.class);
		if (loadedMasterLocation != null)
		{
			masterLocation = loadedMasterLocation;
		}
	}

	private void removeInfoBox()
	{
		if (infoBox != null)
		{
			infoBoxManager.removeInfoBox(infoBox);
			infoBox = null;
		}
	}

	private void saveCasket(MenuOptionClicked event)
	{
		if (isCasketToSave(event.getItemId()))
		{
			event.consume();
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Casket Saver prevents you from opening this casket.", "");
		}
	}

	private void setMasterLocation(MasterLocation location)
	{
		masterDeposited = false;
		masterLocation = location;
		configManager.setRSProfileConfiguration("casketsaver", "masterLocation", masterLocation);
	}

	private void startMasterCooldown()
	{
		hasMasterClueCooldown = true;

		// Start timer infobox
		Timer timer = new Timer(30, ChronoUnit.SECONDS, itemManager.getImage(CASKET), this);
		timer.setTooltip(getCause());
		infoBoxManager.addInfoBox(timer);

		// Start 30-second timer
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
