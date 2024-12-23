/*
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

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

public class CasketSaverOverlay extends OverlayPanel
{
	private final Client client;
	private final CasketSaverPlugin plugin;
	private final CasketSaverConfig config;
	private final TooltipManager tooltipManager;

	@Inject
	public CasketSaverOverlay(Client client, CasketSaverPlugin plugin, CasketSaverConfig config, TooltipManager tooltipManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.tooltipManager = tooltipManager;

		setPosition(OverlayPosition.TOOLTIP);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGHEST);
		setDragTargetable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		renderMouseover();
		return null;
	}

	private void renderMouseover()
	{
		if (!config.showTooltip()) return;

		Menu menu = client.getMenu();
		MenuEntry[] menuEntries = menu.getMenuEntries();
		if (menuEntries.length == 0)
		{
			return;
		}

		MenuEntry entry = menuEntries[menuEntries.length - 1];
		String menuOption = entry.getOption();
		int itemId = entry.getItemId();

		if (plugin.isCasketToSave(itemId) && menuOption.equals("Open"))
		{
			String tooltipText = plugin.getCause();

			if (tooltipText != null){
				tooltipManager.add(new Tooltip(tooltipText));
			}
		}
	}
}
