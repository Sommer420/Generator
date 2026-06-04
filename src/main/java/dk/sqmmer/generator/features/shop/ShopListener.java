/*
 * Copyright (c) 2023 bondegaard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dk.sqmmer.generator.features.shop;

import dev.triumphteam.gui.guis.PaginatedGui;
import dk.sqmmer.generator.Main;
import dk.sqmmer.generator.languages.Lang;
import dk.sqmmer.generator.utils.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class ShopListener implements Listener {

    public ShopListener() {
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!isShopTitle(event.getView().getTitle())) return;

        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        Player player = (Player) event.getWhoClicked();
        ShopHandler handler = ShopHandler.getInstance();
        if (handler == null || handler.getShopConfig() == null) return;

        String title = event.getView().getTitle();
        if (title.equals(StringUtil.colorize(Lang.SHOP_GUI_TITLE + " - Kategorier"))) {
            int stage = Shop.getStageFromSlot(event.getRawSlot());
            if (stage != -1) {
                Shop.openGenerators(handler, player, stage);
                return;
            }
            if (event.getRawSlot() == 31) {
                Shop.openSpecialGenerators(handler, player);
            }
            return;
        }

        if (event.getRawSlot() == 49) {
            Shop.openCategorySelector(handler, player);
            return;
        }

        if (event.getInventory().getHolder() instanceof PaginatedGui) {
            PaginatedGui gui = (PaginatedGui) event.getInventory().getHolder();
            if (event.getRawSlot() == 45) gui.previous();
            if (event.getRawSlot() == 53) gui.next();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isShopTitle(event.getView().getTitle())) return;
        event.setCancelled(true);
    }

    private boolean isShopTitle(String title) {
        return title != null && title.startsWith(StringUtil.colorize(Lang.SHOP_GUI_TITLE));
    }
}
