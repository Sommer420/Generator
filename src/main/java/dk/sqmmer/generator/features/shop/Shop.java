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
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Shop {

    private static final int MIN_STAGE = 1;
    private static final int MAX_STAGE = 15;
    private static final int[] STAGE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28};
    private static final int SPECIAL_SLOT = 31;

    public static void open(ShopHandler handler, Player player) {
        if (handler == null || handler.getShopConfig() == null) return;
        openCategorySelector(handler, player);
    }

    static void openCategorySelector(ShopHandler handler, Player player) {
        Gui gui = new Gui(4, StringUtil.colorize(Lang.SHOP_GUI_TITLE + " - Kategorier"));
        gui.disableAllInteractions();
        cancelAllInteractions(gui);

        for (int stage = MIN_STAGE; stage <= MAX_STAGE; stage++) {
            int slot = STAGE_SLOTS[stage - MIN_STAGE];
            int stageNumber = stage;
            gui.setItem(slot, ItemBuilder.from(categoryItem(Material.BOOK, "&b&lStadie " + stageNumber,
                    "&7Klik for at se generatorer", "&7fra stadie &f" + stageNumber + "&7."))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        openGenerators(handler, player, stageNumber);
                    }));
        }

        gui.setItem(SPECIAL_SLOT, ItemBuilder.from(categoryItem(Material.NETHER_STAR, "&d&lSpecial",
                "&7Generatorer som købes", "&7via &f/buy&7."))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    openSpecialGenerators(handler, player);
                }));

        gui.open(player);
    }

    static void openGenerators(ShopHandler handler, Player player, int stage) {
        PaginatedGui gui = createGeneratorGui(Lang.SHOP_GUI_TITLE + " - Stadie " + stage);

        for (ShopItem shopItem : getShopItems(handler, stage, false)) {
            gui.addItem(ItemBuilder.from(createGeneratorShopItem(shopItem)).asGuiItem(event -> {
                event.setCancelled(true);
                buyGenerator(player, shopItem);
            }));
        }

        addNavigation(handler, player, gui);
        gui.open(player);
    }

    static void openSpecialGenerators(ShopHandler handler, Player player) {
        PaginatedGui gui = createGeneratorGui(Lang.SHOP_GUI_TITLE + " - Special");

        for (ShopItem shopItem : getShopItems(handler, 0, true)) {
            gui.addItem(ItemBuilder.from(createSpecialShopItem(shopItem)).asGuiItem(event -> {
                event.setCancelled(true);
                player.closeInventory();
                player.performCommand("buy");
            }));
        }

        addNavigation(handler, player, gui);
        gui.open(player);
    }


    static int getStageFromSlot(int slot) {
        for (int i = 0; i < STAGE_SLOTS.length; i++) {
            if (STAGE_SLOTS[i] == slot) return MIN_STAGE + i;
        }
        return -1;
    }

    private static PaginatedGui createGeneratorGui(String title) {
        PaginatedGui gui = new PaginatedGui(6, StringUtil.colorize(title));
        gui.disableAllInteractions();
        cancelAllInteractions(gui);
        return gui;
    }

    private static void addNavigation(ShopHandler handler, Player player, PaginatedGui gui) {
        gui.setItem(45, ItemBuilder.from(navigationItem(Material.ARROW, "&eForrige side")).asGuiItem(event -> {
            event.setCancelled(true);
            gui.previous();
        }));
        gui.setItem(49, ItemBuilder.from(navigationItem(Material.BARRIER, "&cTilbage til kategorier")).asGuiItem(event -> {
            event.setCancelled(true);
            openCategorySelector(handler, player);
        }));
        gui.setItem(53, ItemBuilder.from(navigationItem(Material.ARROW, "&eNæste side")).asGuiItem(event -> {
            event.setCancelled(true);
            gui.next();
        }));
    }

    private static void cancelAllInteractions(Gui gui) {
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.setDefaultTopClickAction(event -> event.setCancelled(true));
        gui.setPlayerInventoryAction(event -> event.setCancelled(true));
        gui.setOutsideClickAction(event -> event.setCancelled(true));
        gui.setDragAction(event -> event.setCancelled(true));
    }

    private static void cancelAllInteractions(PaginatedGui gui) {
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.setDefaultTopClickAction(event -> event.setCancelled(true));
        gui.setPlayerInventoryAction(event -> event.setCancelled(true));
        gui.setOutsideClickAction(event -> event.setCancelled(true));
        gui.setDragAction(event -> event.setCancelled(true));
    }

    private static void buyGenerator(Player player, ShopItem shopItem) {
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
        if (playerBalance-shopItem.getPrice() < 0) {
            PlaceholderString shopFailMessage = new PlaceholderString(Lang.PREFIX + Lang.SHOP_BUY_FAIL, "%NEEDED%")
                    .placeholderValues(NumUtils.formatNumber((shopItem.getPrice()- playerBalance)));
            PlayerUtils.sendMessage(player, shopFailMessage);
            return;
        }
        econ.withdrawPlayer(player, shopItem.getPrice());
        PlaceholderString shopSuccessMessage = new PlaceholderString(Lang.PREFIX + Lang.SHOP_BUY_SUCCESS, "%TYPE%", "%PRICE%")
                .placeholderValues(shopItem.getGeneratorType().getName(), NumUtils.formatNumber(shopItem.getPrice()));
        PlayerUtils.sendMessage(player, shopSuccessMessage);
        Pickup.giveItem(player, shopItem.getGeneratorType().getGeneratorItem());
    }

    private static ItemStack createGeneratorShopItem(ShopItem shopItem) {
        GeneratorType generatorType = shopItem.getGeneratorType();
        ItemStack item = generatorType.getGeneratorItem().clone();
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return item;

        List<String> lore = new ArrayList<>();
        double sellValue = generatorType.getGeneratorDrops().isEmpty() ? 0 : generatorType.getGeneratorDrops().get(0).getSellPrice();
        for (String s:Lang.SHOP_ITEM_LORE) {
            PlaceholderString loreMessage = new PlaceholderString(StringUtil.colorize(s), "%PRICE%", "%TYPE%", "%VALUE%")
                    .placeholderValues(NumUtils.formatNumber(shopItem.getPrice()), generatorType.getName(), sellValue);
            lore.add(loreMessage.parse());
        }
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return item;
    }

    private static ItemStack createSpecialShopItem(ShopItem shopItem) {
        ItemStack item = shopItem.getGeneratorType().getGeneratorItem().clone();
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return item;

        List<String> lore = new ArrayList<>();
        lore.add(StringUtil.colorize("&7Denne generator købes via &f/buy&7."));
        lore.add("");
        lore.add(StringUtil.colorize("&d&nKlik for at åbne /buy!"));
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return item;
    }

    private static List<ShopItem> getShopItems(ShopHandler handler, int stage, boolean special) {
        List<ShopItem> items = new ArrayList<>();
        ConfigurationSection guiLayout = handler.getShopConfig().getConfigurationSection("gui-layout");
        if (guiLayout == null) return items;

        for (String key : guiLayout.getKeys(false)) {
            ConfigurationSection section = handler.getShopConfig().getConfigurationSection("gui-layout."+key);
            if (section == null) continue;

            String name = section.contains("name") ? section.getString("name") : "";
            double price = section.contains("price") ? section.getDouble("price") : -1;
            if (name.equals("")) continue;

            GeneratorType generatorType = Main.getInstance().getGeneratorHandler().getGeneratorType(name);
            if (generatorType == null) continue;
            if (special != isSpecialShopItem(section, generatorType)) continue;
            if (!special && generatorType.getStage() != stage) continue;
            if (!special && price == -1) continue;

            items.add(new ShopItem(getSlot(key), price, generatorType));
        }
        items.sort(Comparator.comparingDouble(ShopItem::getPrice).thenComparingInt(ShopItem::getSlot).thenComparing(item -> item.getGeneratorType().getName(), String.CASE_INSENSITIVE_ORDER));
        return items;
    }

    private static boolean isSpecialShopItem(ConfigurationSection section, GeneratorType generatorType) {
        if (section.getBoolean("special", false)) return true;
        String category = section.contains("category") ? section.getString("category") : "";
        if (category != null && category.equalsIgnoreCase("special")) return true;
        return generatorType.getStage() < MIN_STAGE || generatorType.getStage() > MAX_STAGE;
    }

    private static int getSlot(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static ItemStack categoryItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(StringUtil.colorize(name));
        List<String> lore = new ArrayList<>();
        for (String loreLine : loreLines) {
            lore.add(StringUtil.colorize(loreLine));
        }
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return item;
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
