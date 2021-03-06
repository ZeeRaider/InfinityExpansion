package io.github.mooy1.infinityexpansion;

import io.github.mooy1.infinityexpansion.implementation.blocks.StorageUnit;
import io.github.mooy1.infinityexpansion.implementation.machines.GearTransformer;
import io.github.mooy1.infinityexpansion.implementation.mobdata.MobSimulationChamber;
import io.github.mooy1.infinityexpansion.setup.Setup;
import io.github.mooy1.infinityexpansion.setup.commands.GiveRecipe;
import io.github.mooy1.infinityexpansion.setup.commands.ResetConfig;
import io.github.mooy1.infinitylib.PluginUtils;
import io.github.mooy1.infinitylib.command.CommandLib;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.logging.Level;


public class InfinityExpansion extends JavaPlugin implements SlimefunAddon {

    @Getter
    private static InfinityExpansion instance;
    @Getter
    private static int currentTick = 1;
    @Getter
    private static double vanillaScale = 1;
    private final FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        instance = this;

        PluginUtils.setup(ChatColor.AQUA + "Infinity" + ChatColor.GRAY + "Expansion", this, "Mooy1/InfinityExpansion/master", getFile());
        new CommandLib(this, "infinityexpansion", "infinityexpansion.admin", "/ie, /ix, /infinity");
        CommandLib.addCommands(new GiveRecipe(), new ResetConfig());
        setupConfigOptions();

        @SuppressWarnings("unused") final Metrics metrics = new Metrics(this, 8991);

        Setup.setup(this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (currentTick < 120) {
                currentTick++;
            } else {
                currentTick = 1;
            }
        }, 100L, PluginUtils.TICKER_DELAY);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/Mooy1/InfinityExpansion/issues";
    }

    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    private void setupConfigOptions() {
        StorageUnit.DISPLAY_SIGNS = getOrDefault("storage-unit-options.display-signs", true);
        MobSimulationChamber.CHANCE = getOrDefault("balance-options.mob-simulation-xp-chance", 1, 10, 2);
        GearTransformer.sf = getOrDefault("balance-options.allow-sf-item-transform", false);
        vanillaScale = getOrDefault("balance-options.vanilla-economy-scale", .1, 10, 1);
        saveConfig();
    }

    private int getOrDefault(String path, int min, int max, int def) {
        if (hasPath(path, this.config)) {
            int value = this.config.getInt(path);
            if (value >= min && value <= max) {
                return value;
            } else {
                configWarnValue(path);
                this.config.set(path, def);
                return def;
            }
        }
        return def;
    }

    private boolean getOrDefault(String path, boolean def) {
        if (hasPath(path, this.config)) {
            String value = this.config.getString(path);
            if (Objects.equals(value, "true")) {
                return true;
            } else if (Objects.equals(value, "false")) {
                return false;
            } else {
                configWarnValue(path);
                this.config.set(path, def);
                return def;
            }
        }
        return def;
    }

    private double getOrDefault(String path, double min, double max, double def) {
        if (hasPath(path, this.config)) {
            double value = this.config.getDouble(path);
            if (value >= min && value <= max) {
                return value;
            } else {
                configWarnValue(path);
                this.config.set(path, def);
                return def;
            }
        }
        return def;
    }

    private boolean hasPath(String path, FileConfiguration config) {
        if (config.contains(path)) {
            return true;
        } else {
            configWarnPath(path);
            return false;
        }
    }

    private void configWarnValue(String path) {
        PluginUtils.log(Level.WARNING, "Config value at " + path + " was out of bounds, resetting it to default");
    }

    private void configWarnPath(String path) {
        PluginUtils.log(Level.SEVERE, "Config was missing path " + path + ", please add this path or reset your config!");
    }

    /**
     * @param rate ticks per progress
     *
     * @return whether the block should progress
     */
    public static boolean progressEvery(int rate) {
        return progressOn(rate, 0);
    }

    /**
     * @param rate ticks per progress
     * @param pos  offset from from other progress at same rate
     *
     * @return whether the block should progress
     */
    public static boolean progressOn(int rate, int pos) {
        return currentTick % rate == pos;
    }

}