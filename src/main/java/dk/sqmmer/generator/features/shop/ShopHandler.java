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

import dk.sqmmer.generator.Main;
import dk.sqmmer.generator.generators.objects.GeneratorType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ShopHandler {

    private static ShopHandler instance;

    private static boolean enabled = false;

    private FileConfiguration shopConfig = null;

    public ShopHandler() {
        instance = this;
        new ShopListener();
        load();
    }

    public static void openShop(Player player) {
        if (!enabled) return;
        if (player == null) return;
        Shop.open(instance, player);
    }

    public void load() {
        enabled = Main.getInstance().getConfig().contains("shop-enabled") && Main.getInstance().getConfig().getBoolean("shop-enabled");
        if (!enabled) return;
        //Load/Create config file
        File dataFile = new File(Main.getInstance().getDataFolder(), "shop.yml");
        if (!dataFile.exists()) try {
            dataFile.createNewFile();
        } catch (IOException ex) {
        }

        // Loads the file as a bukkit config
        this.shopConfig = YamlConfiguration.loadConfiguration(dataFile);
        syncGeneratorsToShopConfig();
        try {shopConfig.save(dataFile); } catch (IOException ignored) {}
    }

    private void syncGeneratorsToShopConfig() {
        if (!shopConfig.contains("gui-layout")) shopConfig.createSection("gui-layout");

        ConfigurationSection guiLayout = shopConfig.getConfigurationSection("gui-layout");
        Set<String> configuredGenerators = new HashSet<>();
        int highestSlot = -1;

        for (String key : guiLayout.getKeys(false)) {
            highestSlot = Math.max(highestSlot, getSlot(key));
            String name = guiLayout.getString(key + ".name");
            if (name == null || name.isEmpty()) continue;
            configuredGenerators.add(name.toLowerCase());
        }

        int nextSlot = highestSlot + 1;
        for (GeneratorType generatorType:Main.getInstance().getGeneratorHandler().getGeneratorTypes()) {
            if (configuredGenerators.contains(generatorType.getName().toLowerCase())) continue;

            shopConfig.set("gui-layout."+nextSlot+".name", generatorType.getName());
            shopConfig.set("gui-layout."+nextSlot+".price", getDefaultPrice(generatorType, nextSlot));
            nextSlot++;
        }
    }

    private int getSlot(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private double getDefaultPrice(GeneratorType generatorType, int slot) {
        if (generatorType.getUpgradePrice() > 0) return generatorType.getUpgradePrice();
        return 500D * (slot + 1);
    }


    public static ShopHandler getInstance() {return instance;}
    public FileConfiguration getShopConfig() {return shopConfig;}
    public static boolean isEnabled() {return enabled;}

    public static void setEnabled(boolean enabled) {ShopHandler.enabled = enabled;}
}
