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

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import dk.sqmmer.generator.Main;
import dk.sqmmer.generator.features.Pickup;
import dk.sqmmer.generator.generators.objects.GeneratorType;
import dk.sqmmer.generator.languages.Lang;
import dk.sqmmer.generator.utils.NumUtils;
import dk.sqmmer.generator.utils.PlaceholderString;
import dk.sqmmer.generator.utils.PlayerUtils;
import dk.sqmmer.generator.utils.StringUtil;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Shop {


    public static void open(ShopHandler handler, Player player) {
        if (handler == null || handler.getShopConfig() == null || handler.getShopConfig().getConfigurationSection("gui-layout") == null) return;
        openStageSelector(handler, player);
    }

    private static void openStageSelector(ShopHandler handler, Player player) {
        Gui gui = Gui.gui()
                .title(Component.text(StringUtil.colorize(Lang.SHOP_GUI_TITLE + " - Vælg stadie")))
                .rows(3)
                .disableAllInteractions()
                .create();

        int slot = 10;
        for (Integer stage : getStages(handler)) {
            if (slot == 17) slot = 19;
            if (slot > 25) break;

            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setDisplayName(StringUtil.colorize("&b&lStadie " + stage));
            List<String> lore = new ArrayList<>();
            lore.add(StringUtil.colorize("&7Klik for at se generatorer"));
            lore.add(StringUtil.colorize("&7fra stadie &f" + stage + "&7."));
            itemMeta.setLore(lore);
            item.setItemMeta(itemMeta);

            gui.setItem(slot, ItemBuilder.from(item).asGuiItem(event -> openGenerators(handler, player, stage)));
            slot++;
        }

        gui.open(player);
    }

    private static void openGenerators(ShopHandler handler, Player player, int stage) {
        PaginatedGui gui = Gui.paginated()
                .title(Component.text(StringUtil.colorize(Lang.SHOP_GUI_TITLE + " - Stadie " + stage)))
                .rows(6)
                .disableAllInteractions()
                .create();

        List<ShopItem> items = getShopItems(handler, stage);
        for (ShopItem shopItem : items) {
            GeneratorType generatorType = shopItem.getGeneratorType();
            double price = shopItem.getPrice();

            // Add Text To Item
            ItemStack item = generatorType.getGeneratorItem().clone();
            ItemMeta itemMeta = item.getItemMeta();
            List<String> lore = new ArrayList<>();
            double sellValue = generatorType.getGeneratorDrops().isEmpty() ? 0 : generatorType.getGeneratorDrops().get(0).getSellPrice();
            for (String s:Lang.SHOP_ITEM_LORE) {
                PlaceholderString loreMessage = new PlaceholderString(StringUtil.colorize(s), "%PRICE%", "%TYPE%", "%VALUE%")
                        .placeholderValues(NumUtils.formatNumber(price), generatorType.getName(), sellValue);
                lore.add(loreMessage.parse());
            }
            itemMeta.setLore(lore);
            item.setItemMeta(itemMeta);

            // Insert Items
            gui.addItem(ItemBuilder.from(item).asGuiItem(event -> {
                if (player.getInventory().firstEmpty() == -1) {
                    PlayerUtils.sendMessage(player, Lang.PREFIX+ Lang.SHOP_FULL_INVENTORY);
                    return;
                }
                Economy econ = Main.getInstance().getEconomy();
                if (econ == null) {
                    PlaceholderString errorMessage = new PlaceholderString(Lang.PREFIX + Lang.ERROR, "%ERROR%")
                            .placeholderValues(Lang.NO_ECONOMY);
                    PlayerUtils.sendMessage(player, errorMessage);
                    return;
                }
                double playerBalance = econ.getBalance(player);
                if (playerBalance-price < 0) {
                    PlaceholderString shopFailMessage = new PlaceholderString(Lang.PREFIX + Lang.SHOP_BUY_FAIL, "%NEEDED%")
                            .placeholderValues(NumUtils.formatNumber((price- playerBalance)));
                    PlayerUtils.sendMessage(player, shopFailMessage);
                    return;
                }
                econ.withdrawPlayer(player, price);
                PlaceholderString shopSuccessMessage = new PlaceholderString(Lang.PREFIX + Lang.SHOP_BUY_SUCCESS, "%TYPE%", "%PRICE%")
                        .placeholderValues(generatorType.getName(), NumUtils.formatNumber(price));
                PlayerUtils.sendMessage(player, shopSuccessMessage);
                Pickup.giveItem(player, generatorType.getGeneratorItem());

            }));
        }

        gui.setItem(45, ItemBuilder.from(navigationItem(Material.ARROW, "&eForrige side")).asGuiItem(event -> gui.previous()));
        gui.setItem(49, ItemBuilder.from(navigationItem(Material.BARRIER, "&cTilbage til stadier")).asGuiItem(event -> openStageSelector(handler, player)));
        gui.setItem(53, ItemBuilder.from(navigationItem(Material.ARROW, "&eNæste side")).asGuiItem(event -> gui.next()));
        gui.open(player);
    }

    private static Set<Integer> getStages(ShopHandler handler) {
        Set<Integer> stages = new TreeSet<>();
        if (handler.getShopConfig().getConfigurationSection("gui-layout") == null) return stages;

        for (String key : handler.getShopConfig().getConfigurationSection("gui-layout").getKeys(false)) {
            ConfigurationSection section = handler.getShopConfig().getConfigurationSection("gui-layout." + key);
            if (section == null || !section.contains("name")) continue;

            GeneratorType generatorType = Main.getInstance().getGeneratorHandler().getGeneratorType(section.getString("name"));
            if (generatorType == null) continue;

            stages.add(generatorType.getStage());
        }
        return stages;
    }

    private static List<ShopItem> getShopItems(ShopHandler handler, int stage) {
        List<ShopItem> items = new ArrayList<>();
        ConfigurationSection guiLayout = handler.getShopConfig().getConfigurationSection("gui-layout");
        if (guiLayout == null) return items;

        for (String key : guiLayout.getKeys(false)) {
            ConfigurationSection section = handler.getShopConfig().getConfigurationSection("gui-layout."+key);
            if (section == null) continue;

            String name = section.contains("name") ? section.getString("name") : "";
            double price = section.contains("price") ? section.getDouble("price") : -1;
            if (price == -1 || name.equals("")) continue;

            GeneratorType generatorType = Main.getInstance().getGeneratorHandler().getGeneratorType(name);
            if (generatorType == null || generatorType.getStage() != stage) continue;

            items.add(new ShopItem(getSlot(key), price, generatorType));
        }
        items.sort(Comparator.comparingInt(ShopItem::getSlot).thenComparing(item -> item.getGeneratorType().getName(), String.CASE_INSENSITIVE_ORDER));
        return items;
    }

    private static int getSlot(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static ItemStack navigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(StringUtil.colorize(name));
        item.setItemMeta(itemMeta);
        return item;
    }

    private static class ShopItem {
        private final int slot;
        private final double price;
        private final GeneratorType generatorType;

        private ShopItem(int slot, double price, GeneratorType generatorType) {
            this.slot = slot;
            this.price = price;
            this.generatorType = generatorType;
        }

        public int getSlot() {
            return slot;
        }

        public double getPrice() {
            return price;
        }

        public GeneratorType getGeneratorType() {
            return generatorType;
        }
    }
}
