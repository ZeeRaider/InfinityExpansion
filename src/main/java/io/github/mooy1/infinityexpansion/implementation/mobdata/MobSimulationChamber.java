package io.github.mooy1.infinityexpansion.implementation.mobdata;

import io.github.mooy1.infinityexpansion.InfinityExpansion;
import io.github.mooy1.infinityexpansion.implementation.materials.MachineItem;
import io.github.mooy1.infinityexpansion.setup.categories.Categories;
import io.github.mooy1.infinityexpansion.utils.Util;
import io.github.mooy1.infinitylib.PluginUtils;
import io.github.mooy1.infinitylib.items.StackUtils;
import io.github.mooy1.infinitylib.math.RandomUtils;
import io.github.mooy1.infinitylib.objects.AbstractContainer;
import io.github.mooy1.infinitylib.presets.LorePreset;
import io.github.mooy1.infinitylib.presets.MenuPreset;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.Map;

public final class MobSimulationChamber extends AbstractContainer implements EnergyNetComponent {
    
    public static final SlimefunItemStack ITEM = new SlimefunItemStack(
            "MOB_SIMULATION_CHAMBER",
            Material.GILDED_BLACKSTONE,
            "&8Mob Simulation Chamber",
            "&7Use mob data cards to activate",
            "",
            LorePreset.energyBuffer(MobSimulationChamber.BUFFER),
            LorePreset.energyPerSecond(MobSimulationChamber.ENERGY)
    );
    
    private static final int CARD_SLOT = MenuPreset.slot1 + 27;
    private static final int INTERVAL = 16;
    private static final int STATUS_SLOT = MenuPreset.slot1;
    private static final int[] OUTPUT_SLOTS = Util.largeOutput;
    private static final int XP_Slot = 46;
    public static final int BUFFER = 16000;
    public static final int ENERGY = 240;
    public static int CHANCE = 2;

    private static final ItemStack NO_CARD = new CustomItem(Material.BARRIER, "&cInput a Mob Data Card!");
    
    public MobSimulationChamber() {
        super(Categories.MOB_SIMULATION, ITEM, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                MachineItem.MAGSTEEL_PLATE, MachineItem.MACHINE_PLATE, MachineItem.MAGSTEEL_PLATE,
                MachineItem.MACHINE_CIRCUIT, SlimefunItems.PROGRAMMABLE_ANDROID_BUTCHER, MachineItem.MACHINE_CIRCUIT,
                MachineItem.MAGSTEEL_PLATE, MachineItem.MACHINE_PLATE, MachineItem.MAGSTEEL_PLATE,
        });

        registerBlockHandler(getId(), (p, b, stack, reason) -> {
            BlockMenu inv = BlockStorage.getInventory(b);
            Location l = b.getLocation();
            
            if (inv != null) {
                inv.dropItems(l, OUTPUT_SLOTS);
                inv.dropItems(l, CARD_SLOT);
            }
            
            p.giveExp(getXP(l));
            setXp(l, 0);

            return true;
        });
    }

    @Override
    public void tick(@Nonnull Block b, @Nonnull BlockMenu inv) {

        MobDataCard.Type card = MobDataCard.CARDS.get(StackUtils.getIDofNullable(inv.getItemInSlot(CARD_SLOT)));

        if (card == null) {
            if (inv.hasViewer()) {
                inv.replaceExistingItem(STATUS_SLOT, NO_CARD);
            }
            return;
        }
        
        int energy = card.energy + ENERGY;
        
        if (getCharge(b.getLocation()) < energy) {
            if (inv.hasViewer()) {
                inv.replaceExistingItem(STATUS_SLOT, MenuPreset.notEnoughEnergy);
            }
            return;
        }

        if (inv.hasViewer()) {
            inv.replaceExistingItem(STATUS_SLOT, makeSimulating(energy));
            inv.replaceExistingItem(XP_Slot, makeXpItem(getXP(b.getLocation())));
        }

        if (!InfinityExpansion.progressEvery(INTERVAL)) return;
        
        if (RandomUtils.chanceIn(CHANCE)) {
            int xp = getXP(b.getLocation()) + card.xp;
            setXp(b.getLocation(), xp);
        }
        
        for (Map.Entry<Integer, ItemStack> entry : card.drops.entrySet()) {
            if (RandomUtils.chanceIn(entry.getKey())) {
                ItemStack output = entry.getValue();
                if (inv.fits(output, OUTPUT_SLOTS)) {
                    inv.pushItem(output.clone(), OUTPUT_SLOTS);
                } else if (inv.hasViewer()) {
                    inv.replaceExistingItem(STATUS_SLOT, MenuPreset.notEnoughRoom);
                }
            }
        }
        
        removeCharge(b.getLocation(), energy);
    }
    
    @Nonnull
    private ItemStack makeSimulating(int energy) {
        return new CustomItem(Material.LIME_STAINED_GLASS_PANE, "&aSimulating... (" + Math.round(energy * PluginUtils.TICK_RATIO) + " J/s)");
    }

    @Override
    public void setupInv(@Nonnull BlockMenuPreset blockMenuPreset) {
        for (int i : Util.largeOutputBorder) {
            blockMenuPreset.addItem(i, MenuPreset.borderItemOutput, ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i : MenuPreset.slotChunk1) {
            blockMenuPreset.addItem(i, MenuPreset.borderItemStatus, ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i : MenuPreset.slotChunk1) {
            blockMenuPreset.addItem(i + 27, MenuPreset.borderItemInput, ChestMenuUtils.getEmptyClickHandler());
        }
        blockMenuPreset.addItem(STATUS_SLOT, MenuPreset.loadingItemRed, ChestMenuUtils.getEmptyClickHandler());
        blockMenuPreset.addItem(XP_Slot, MenuPreset.loadingItemRed, ChestMenuUtils.getEmptyClickHandler());
    }

    @Override
    public int[] getTransportSlots(@Nonnull ItemTransportFlow flow) {
        if (flow == ItemTransportFlow.WITHDRAW) {
            return OUTPUT_SLOTS;
        }
        return new int[0];
    }
    
    @Nonnull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return BUFFER;
    }

    @Override
    public void onNewInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
        Location l = b.getLocation();
        if (BlockStorage.getLocationInfo(l, "xp") == null) {
            setXp(l, 0);
        }
        menu.replaceExistingItem(XP_Slot, makeXpItem(0));
        menu.addMenuClickHandler(XP_Slot, (p, slot, item, action) -> {
            int xp = getXP(l);
            if (xp > 0) {
                p.giveExp(xp);
                p.playSound(l, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                setXp(l, 0);
                menu.replaceExistingItem(XP_Slot, makeXpItem(0));
            }
            return false;
        });
    }
    
    private ItemStack makeXpItem(int stored) {
        return new CustomItem(Material.LIME_STAINED_GLASS_PANE, "&aStored xp: " + stored, "", "&a> Click to claim");
    }

    private void setXp(Location l, int xp) {
        BlockStorage.addBlockInfo(l, "xp", String.valueOf(xp));
    }
    
    private int getXP(Location l) {
        try {
            return Integer.parseInt(BlockStorage.getLocationInfo(l, "xp"));
        } catch (NumberFormatException e) {
            setXp(l, 0);
            return 0;
        }
    }
    
}
