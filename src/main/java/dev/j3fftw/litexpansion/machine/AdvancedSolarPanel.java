package dev.j3fftw.litexpansion.machine;

import dev.j3fftw.extrautils.interfaces.InventoryBlock;
import dev.j3fftw.extrautils.utils.Utils;
import dev.j3fftw.litexpansion.Items;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class AdvancedSolarPanel extends SlimefunItem implements InventoryBlock, EnergyNetProvider {

    private static final int PROGRESS_SLOT = 4;
    private static final CustomItem generatingItem = new CustomItem(Material.ORANGE_STAINED_GLASS_PANE,
        "&cNot Generating..."
    );
    public static int advancedDayRate = 80;
    public static int advancedNightRate = 10;
    public static int advancedOutput = 320;
    public static int advancedStorage = 320_000;
    public static int hybridDayRate = 640;
    public static int hybridNightRate = 80;
    public static int hybridOutput = 1200;
    public static int hybridStorage = 1_000_000;
    public static int ultimateDayRate = 5120;
    public static int ultimateNightRate = 640;
    public static int ultimateOutput = 5120;
    public static int ultimateStorage = 10_000_000;
    private final Type type;

    public AdvancedSolarPanel(Type type) {
        super(Items.LITEXPANSION, type.getItem(), RecipeType.ENHANCED_CRAFTING_TABLE, type.getRecipe());
        this.type = type;

        createPreset(this, type.getItem().getImmutableMeta().getDisplayName().orElse("&7Solar Panel"),
            blockMenuPreset -> {
                for (int i = 0; i < 9; i++) {
                    blockMenuPreset.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
                }
                blockMenuPreset.addItem(PROGRESS_SLOT, generatingItem);
            });
    }

    @Override
    public int getGeneratedOutput(@Nonnull Location l, @Nonnull Config data) {
        @Nullable final BlockMenu inv = BlockStorage.getInventory(l);
        if (inv == null) {
            return 0;
        }

        final int stored = getCharge(l);
        final boolean canGenerate = stored < getCapacity();
        Validate.notNull(l.getWorld());
        final int rate = canGenerate ? getGeneratingAmount(inv.getBlock(), l.getWorld()) : 0;

        String generationType = "&4Unknown";

        if (l.getWorld().getEnvironment() == World.Environment.NETHER) {
            generationType = "&cNether &e(Day)";
        } else if (l.getWorld().getEnvironment() == World.Environment.THE_END) {
            generationType = "&5End &8(Night)";
        } else if (rate == this.type.getDayGenerationRate()) {
            generationType = "&aOverworld &e(Day)";
        } else if (rate == this.type.getNightGenerationRate()) {
            generationType = "&aOverworld &8(Night)";
        }

        if (inv.toInventory() != null && !inv.toInventory().getViewers().isEmpty()) {
            inv.replaceExistingItem(PROGRESS_SLOT,
                canGenerate ? new CustomItem(Material.GREEN_STAINED_GLASS_PANE, "&aGenerating",
                    "", "&bRate: " + generationType,
                    "&7Generating at &6" + Utils.powerFormatAndFadeDecimals(Utils.perTickToPerSecond(rate)) + " J/s " +
                        "&8(" + rate + " J/t)",
                    "", "&7Stored: &6" + Utils.powerFormatAndFadeDecimals((double) stored + rate) + " J"
                )
                    : new CustomItem(Material.ORANGE_STAINED_GLASS_PANE, "&cNot Generating",
                    "", "&7Generator has reached maximum capacity.",
                    "", "&7Stored: &6" + Utils.powerFormatAndFadeDecimals(stored) + " J")
            );
        }

        return rate;
    }

    @Override
    public boolean willExplode(@Nonnull Location l, @Nonnull Config data) {
        return false;
    }

    private int getGeneratingAmount(@Nonnull Block b, @Nonnull World world) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            return this.type.getDayGenerationRate();
        }
        if (world.getEnvironment() == World.Environment.THE_END) {
            return this.type.getNightGenerationRate();
        }

        // Note: You need to get the block above for the light check, the block itself is always 0
        if (world.isThundering() || world.hasStorm() || world.getTime() >= 13000
            || b.getLocation().add(0, 1, 0).getBlock().getLightFromSky() != 15
        ) {
            return this.type.getNightGenerationRate();
        } else {
            return this.type.getDayGenerationRate();
        }
    }

    @Nonnull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.GENERATOR;
    }

    @Override
    public int getCapacity() {
        return this.type.getStorage();
    }

    @Override
    public int[] getInputSlots() {
        return new int[0];
    }

    @Override
    public int[] getOutputSlots() {
        return new int[0];
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public enum Type {

        ADVANCED(Items.ADVANCED_SOLAR_PANEL, advancedDayRate, advancedNightRate, advancedOutput,
            advancedStorage, new ItemStack[] {
            Items.REINFORCED_GLASS, Items.REINFORCED_GLASS, Items.REINFORCED_GLASS,
            Items.ADVANCED_ALLOY, SlimefunItems.SOLAR_GENERATOR_4, Items.ADVANCED_ALLOY,
            SlimefunItems.ADVANCED_CIRCUIT_BOARD, Items.ADVANCED_MACHINE_BLOCK, SlimefunItems.ADVANCED_CIRCUIT_BOARD
        }),

        HYBRID(Items.HYBRID_SOLAR_PANEL, hybridDayRate, hybridNightRate, hybridOutput, hybridStorage,
            new ItemStack[] {
                Items.CARBON_PLATE, new ItemStack(Material.LAPIS_BLOCK), Items.CARBON_PLATE,
                Items.IRIDIUM_PLATE, Items.ADVANCED_MACHINE_BLOCK, Items.IRIDIUM_PLATE,
                SlimefunItems.ADVANCED_CIRCUIT_BOARD, Items.IRIDIUM_PLATE, SlimefunItems.ADVANCED_CIRCUIT_BOARD
            }),

        ULTIMATE(Items.ULTIMATE_SOLAR_PANEL, ultimateDayRate, ultimateNightRate, ultimateOutput,
            ultimateStorage, new ItemStack[] {
            Items.HYBRID_SOLAR_PANEL, Items.HYBRID_SOLAR_PANEL, Items.HYBRID_SOLAR_PANEL,
            Items.HYBRID_SOLAR_PANEL, SlimefunItems.ADVANCED_CIRCUIT_BOARD, Items.HYBRID_SOLAR_PANEL,
            Items.HYBRID_SOLAR_PANEL, Items.HYBRID_SOLAR_PANEL, Items.HYBRID_SOLAR_PANEL,
        });

        @Nonnull
        private final SlimefunItemStack item;
        private final int dayGenerationRate;
        private final int nightGenerationRate;
        private final int output;
        private final int storage;

        @Nonnull
        private final ItemStack[] recipe;
    }
}
