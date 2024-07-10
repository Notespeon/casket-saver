package com.casketsaver;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

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
	public ArrayList<Integer> casketIds = new ArrayList<Integer>() {{
		add(REWARD_CASKET_EASY);
		add(REWARD_CASKET_MEDIUM);
		add(REWARD_CASKET_HARD);
		add(REWARD_CASKET_ELITE);
	}};

	@Inject
	private Client client;

	@Inject
	private CasketSaverConfig config;
	private ItemContainer bankContainer;
	private boolean hasMasterClueInventory = false;
	private boolean hasMasterClueBanked = false;
	private boolean hasMasterClueReward = false;
	private int casketCooldown;

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (event.getContainerId() == InventoryID.INVENTORY.getId() ) {
			bankContainer = client.getItemContainer(InventoryID.BANK);

			if (bankContainer != null) {
				hasMasterClueBanked = false;
				Arrays.stream(bankContainer.getItems())
						.forEach(item -> {
							if (!hasMasterClueBanked && item.getId() == CLUE_SCROLL_MASTER) {
								hasMasterClueBanked = true;
							}
						});
			}

			hasMasterClueInventory = false;
			Arrays.stream(event.getItemContainer().getItems())
					.forEach(item -> {
						if (!hasMasterClueInventory && item.getId() == CLUE_SCROLL_MASTER) {
							hasMasterClueInventory = true;
							hasMasterClueBanked = false;
							hasMasterClueReward = false;
						}
					});

		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event) {
		if (event.getGroupId() == InterfaceID.CLUESCROLL_REWARD) {
			final Widget clueScrollReward = client.getWidget(ComponentID.CLUESCROLL_REWARD_ITEM_CONTAINER);

            for (Widget widget : Objects.requireNonNull(clueScrollReward.getChildren())) {
				if (widget.getItemId() == CLUE_SCROLL_MASTER) {
					hasMasterClueReward = true;
				}
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (!config.casketCooldown()) {
			return;
		}

		if (event.getMenuAction().getId() == 57 && casketIds.contains(event.getItemId())) {
			if (casketCooldown == 0) {
				casketCooldown = 1;
			} else {
				event.consume();
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!config.casketCooldown()) {
			return;
		}

		casketCooldown = 0;
	}



	@Provides
	CasketSaverConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CasketSaverConfig.class);
	}

	@Subscribe(priority = -1)
	// This will run after the normal menu entry swapper, so it won't interfere with this plugin.
	public void onPostMenuSort(PostMenuSort e) {
		// The menu is not rebuilt when it is open, so don't swap or else it will
		// repeatedly swap entries
		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen() || client.isKeyPressed(KeyCode.KC_SHIFT)) {
			return;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();

		for (int i = 0; i < menuEntries.length; i++) {
			checkCaskets(menuEntries, i);
		}
	}

	private void checkCaskets(MenuEntry[] menuEntries, int index) {
		MenuEntry menuEntry = menuEntries[index];
		boolean hasMasterClue = hasMasterClueBanked || hasMasterClueInventory || hasMasterClueReward;
		if ((menuEntry.getItemId() == REWARD_CASKET_EASY)
				&& config.easyMode() && hasMasterClue) {
			doSwaps(menuEntries);
		} else if ((menuEntry.getItemId() == REWARD_CASKET_MEDIUM)
				&& config.mediumMode() && hasMasterClue) {
			doSwaps(menuEntries);
		} else if ((menuEntry.getItemId() == REWARD_CASKET_HARD)
				&& config.hardMode() && hasMasterClue) {
			doSwaps(menuEntries);
		} else if ((menuEntry.getItemId() == REWARD_CASKET_ELITE)
				&& config.eliteMode() && hasMasterClue) {
			doSwaps(menuEntries);
		}
	}

	private void doSwaps(MenuEntry[] menuEntries) {
		int useIndex = -1;
		int topIndex = menuEntries.length - 1;

		for (int i = 0; i < topIndex; i++) {
			if (Text.removeTags(menuEntries[i].getOption()).equals("Use")) {
				useIndex = i;
				break;
			}
		}

		if (useIndex == -1) {
			return;
		}

		MenuEntry entry1 = menuEntries[useIndex];
		MenuEntry entry2 = menuEntries[topIndex];

		menuEntries[useIndex] = entry2;
		menuEntries[topIndex] = entry1;

		client.setMenuEntries(menuEntries);
	}
}
