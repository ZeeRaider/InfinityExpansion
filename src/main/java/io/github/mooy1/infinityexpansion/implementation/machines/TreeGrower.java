package io.github.mooy1.infinityexpansion.implementation.machines;

import io.github.mooy1.infinityexpansion.InfinityExpansion;
import io.github.mooy1.infinityexpansion.implementation.blocks.InfinityWorkbench;
import io.github.mooy1.infinityexpansion.implementation.materials.InfinityItem;
import io.github.mooy1.infinityexpansion.implementation.materials.MachineItem;
import io.github.mooy1.infinityexpansion.implementation.materials.SmelteryItem;
import io.github.mooy1.infinityexpansion.setup.categories.Categories;
import io.github.mooy1.infinityexpansion.utils.Util;
import io.github.mooy1.infinitylib.math.RandomUtils;
import io.github.mooy1.infinitylib.objects.AbstractMachine;
import io.github.mooy1.infinitylib.presets.LorePreset;
import io.github.mooy1.infinitylib.presets.MenuPreset;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;
import lombok.NonNull;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Grows trees in a virtual interface
 *
 * @author Mooy1
 */
public final class TreeGrower extends AbstractMachine implements RecipeDisplayItem {

    public static void setup(InfinityExpansion plugin) {
        new TreeGrower(Categories.BASIC_MACHINES, BASIC, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                new ItemStack(Material.GLASS), new ItemStack(Material.GLASS), new ItemStack(Material.GLASS),
                SmelteryItem.MAGSTEEL, new ItemStack(Material.PODZOL), SmelteryItem.MAGSTEEL,
                MachineItem.MACHINE_CIRCUIT, VirtualFarm.BASIC, MachineItem.MACHINE_CIRCUIT
        }, 36, 1).register(plugin);
        new TreeGrower(Categories.ADVANCED_MACHINES, ADVANCED, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.HARDENED_GLASS, SlimefunItems.HARDENED_GLASS, SlimefunItems.HARDENED_GLASS,
                SmelteryItem.MAGNONIUM, BASIC,SmelteryItem.MAGNONIUM,
                MachineItem.MACHINE_CIRCUIT, MachineItem.MACHINE_CORE, MachineItem.MACHINE_CIRCUIT
        }, 180, 5).register(plugin);
        new TreeGrower(Categories.INFINITY_CHEAT, INFINITY, InfinityWorkbench.TYPE, new ItemStack[] {
                new ItemStack(Material.GLASS), new ItemStack(Material.GLASS), new ItemStack(Material.GLASS), new ItemStack(Material.GLASS), new ItemStack(Material.GLASS), new ItemStack(Material.GLASS),
                new ItemStack(Material.GLASS), SlimefunItems.TREE_GROWTH_ACCELERATOR, null, null, SlimefunItems.TREE_GROWTH_ACCELERATOR, new ItemStack(Material.GLASS),
                new ItemStack(Material.GLASS), ADVANCED, null, null, ADVANCED, new ItemStack(Material.GLASS),
                new ItemStack(Material.GLASS), SlimefunItems.TREE_GROWTH_ACCELERATOR, null, null, SlimefunItems.TREE_GROWTH_ACCELERATOR, new ItemStack(Material.GLASS),
                MachineItem.MACHINE_PLATE, new ItemStack(Material.PODZOL), new ItemStack(Material.PODZOL), new ItemStack(Material.PODZOL), new ItemStack(Material.PODZOL), MachineItem.MACHINE_PLATE,
                MachineItem.MACHINE_PLATE, InfinityItem.CIRCUIT, InfinityItem.CORE, InfinityItem.CORE, InfinityItem.CIRCUIT, MachineItem.MACHINE_PLATE
        }, 1800, 25).register(plugin);
    }

    public static final SlimefunItemStack BASIC = new SlimefunItemStack(
            "BASIC_TREE_GROWER",
            Material.STRIPPED_OAK_WOOD,
            "&9Basic &2Tree Grower",
            "&7Automatically grows, harvests, and replants trees",
            "",
            LorePreset.speed(1),
            LorePreset.energyPerSecond(36)
    );
    public static final SlimefunItemStack ADVANCED = new SlimefunItemStack(
            "ADVANCED_TREE_GROWER",
            Material.STRIPPED_ACACIA_WOOD,
            "&cAdvanced &2Tree Grower",
            "&7Automatically grows, harvests, and replants trees",
            "",
            LorePreset.speed(5),
            LorePreset.energyPerSecond(180)
    );
    public static final SlimefunItemStack INFINITY = new SlimefunItemStack(
            "INFINITY_TREE_GROWER",
            Material.STRIPPED_WARPED_HYPHAE,
            "&bInfinity &2Tree Grower",
            "&7Automatically grows, harvests, and replants trees",
            "",
            LorePreset.speed(25),
            LorePreset.energyPerSecond(1800)
    );

    public static final int TIME = 600;

    private static final int[] OUTPUT_SLOTS = Util.largeOutput;
    private static final int[] INPUT_SLOTS = {
            MenuPreset.slot1 + 27
    };
    private static final int STATUS_SLOT = MenuPreset.slot1;

    private final int speed;

    private TreeGrower(Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int energy, int speed) {
        super(category, item, recipeType, recipe, STATUS_SLOT, energy);
        this.speed = speed;

        registerBlockHandler(getId(), (p, b, stack, reason) -> {
            BlockMenu inv = BlockStorage.getInventory(b);

            if (inv != null) {
                Location l = b.getLocation();
                inv.dropItems(l, OUTPUT_SLOTS);
                inv.dropItems(l, INPUT_SLOTS);

                String progressType = getType(b);
                if (progressType != null) {
                    b.getWorld().dropItemNaturally(l, new ItemStack(Objects.requireNonNull(Material.getMaterial(progressType + "_SAPLING"))));
                }
            }

            setProgress(b, 0);
            setType(b, null);

            return true;
        });
    }

    public void setupInv(@Nonnull BlockMenuPreset blockMenuPreset) {
        for (int i : MenuPreset.slotChunk1) {
            blockMenuPreset.addItem(i, MenuPreset.borderItemStatus, ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i : MenuPreset.slotChunk1) {
            blockMenuPreset.addItem(i + 27, MenuPreset.borderItemInput, ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i : Util.largeOutputBorder) {
            blockMenuPreset.addItem(i, MenuPreset.borderItemOutput, ChestMenuUtils.getEmptyClickHandler());
        }
        blockMenuPreset.addItem(STATUS_SLOT, MenuPreset.loadingItemRed, ChestMenuUtils.getEmptyClickHandler());
    }

    @Override
    public void onNewInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
        if (getProgress(b) == null) {
            setProgress(b, 0);
        }
    }

    @Override
    public int[] getTransportSlots(@Nonnull DirtyChestMenu menu, @Nonnull ItemTransportFlow flow, @Nonnull ItemStack item) {
        if (flow == ItemTransportFlow.WITHDRAW) {
            return OUTPUT_SLOTS;
        }

        if (flow == ItemTransportFlow.INSERT && SlimefunTag.SAPLINGS.isTagged(item.getType())) {
            return INPUT_SLOTS;
        }

        return new int[0];
    }

    @Override
    public boolean process(@Nonnull Block b, @Nonnull BlockMenu inv) {
        int progress = Integer.parseInt(getProgress(b));

        if (progress == 0) { //try to start
            ItemStack input = inv.getItemInSlot(INPUT_SLOTS[0]);

            if (input == null) {

                if (inv.hasViewer()) {
                    inv.replaceExistingItem(STATUS_SLOT, new CustomItem(Material.BLUE_STAINED_GLASS_PANE, "&9Input a sapling"));
                }

            } else {

                String inputType = getInputType(input);

                if (inputType == null) {

                    if (inv.hasViewer()) {
                        inv.replaceExistingItem(STATUS_SLOT, new CustomItem(Material.BARRIER, "&cInput a sapling!"));
                    }

                    for (int slot : OUTPUT_SLOTS) {
                        if (inv.getItemInSlot(slot) == null) {
                            inv.replaceExistingItem(slot, input);
                            inv.consumeItem(INPUT_SLOTS[0], input.getAmount());
                            break;
                        }
                    }

                } else { //start

                    setProgress(b, this.speed);
                    setType(b, inputType);
                    inv.consumeItem(INPUT_SLOTS[0], 1);

                    if (inv.hasViewer()) {
                        inv.replaceExistingItem(STATUS_SLOT, new CustomItem(Material.LIME_STAINED_GLASS_PANE,
                                "&aPlanting... (" + this.speed + "/" + TIME + ")"));
                    }

                    return true;
                }
            }
            return false;
        }

        if (progress < TIME) { //progress

            setProgress(b, progress + this.speed);

            if (inv.hasViewer()) {
                inv.replaceExistingItem(STATUS_SLOT, new CustomItem(Material.LIME_STAINED_GLASS_PANE, "&aGrowing... (" + (progress + this.speed) + "/" + TIME + ")"));
            }
            return true;
        }

        //done
        String type = getType(b);

        ItemStack output1 = new ItemStack(Objects.requireNonNull(Material.getMaterial(type + "_LOG")), RandomUtils.randomFromRange(6, 12));
        ItemStack output2 = new ItemStack(Objects.requireNonNull(Material.getMaterial(type + "_LEAVES")), RandomUtils.randomFromRange(8, 16));
        ItemStack output3 = new ItemStack(Objects.requireNonNull(Material.getMaterial(type + "_SAPLING")), RandomUtils.randomFromRange(1, 2));

        if (!inv.fits(output1, OUTPUT_SLOTS)) {

            if (inv.hasViewer()) {
                inv.replaceExistingItem(STATUS_SLOT, MenuPreset.notEnoughRoom);
            }
            return false;

        } else {
            inv.pushItem(output1, OUTPUT_SLOTS);
            if (inv.fits(output2, OUTPUT_SLOTS)) inv.pushItem(output2, OUTPUT_SLOTS);
            if (inv.fits(output3, INPUT_SLOTS)) inv.pushItem(output3, INPUT_SLOTS);

            if (type.equals("OAK")) {
                ItemStack apple = new ItemStack(Material.APPLE);
                if (inv.fits(apple, OUTPUT_SLOTS)) inv.pushItem(apple, OUTPUT_SLOTS);
            }

            if (inv.hasViewer()) {
                inv.replaceExistingItem(STATUS_SLOT, new CustomItem(Material.LIME_STAINED_GLASS_PANE, "&aHarvesting..."));
            }

            setProgress(b, 0);
            setType(b, null);

            return true;

        }
    }

    /**
     * This method gets the type of input
     *
     * @param input input item
     *
     * @return type of input
     */
    @Nullable
    private String getInputType(@NonNull ItemStack input) {
        for (String recipe : INPUTS) {
            if (input.getType() == Material.getMaterial(recipe + "_SAPLING")) return recipe;
        }
        return null;
    }

    private void setType(Block b, String type) {
        BlockStorage.addBlockInfo(b, "type", type);
    }

    private String getType(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation(), "type");
    }

    private void setProgress(Block b, int progress) {
        BlockStorage.addBlockInfo(b, "progress", String.valueOf(progress));
    }

    private String getProgress(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation(), "progress");
    }

    @Override
    public int getCapacity() {
        return this.energy * 2;
    }

    @Nonnull
    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> items = new ArrayList<>();

        for (String input : INPUTS) {
            items.add(new ItemStack(Objects.requireNonNull(Material.getMaterial(input + "_SAPLING"))));
            items.add(new ItemStack(Objects.requireNonNull(Material.getMaterial(input + "_LOG"))));
        }
        return items;
    }

    private static final String[] INPUTS = {
            "OAK",
            "DARK_OAK",
            "ACACIA",
            "SPRUCE",
            "BIRCH",
            "JUNGLE"
    };

}
