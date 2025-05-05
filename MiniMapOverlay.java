package com.oakmods.oakfrontier.client.screens;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.resources.ResourceLocation;
import com.oakmods.oakfrontier.command.ToggleMinimapCommand;
import com.oakmods.oakfrontier.procedures.CompassReturnsProcedure;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;


import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber({Dist.CLIENT})
public class MiniMapOverlay 
{

    // Minimap size constants
    private static final int MINIMAP_SIZE = 100; // Minimap size in pixels
    // WARNING DO NOT SET ANY HIGHER THEN 16 or it will cause LAG
    private static final int MINIMAP_RADIUS = 16; // How many blocks to show around the player
    private static final int SURFACE_SCAN_DEPTH = 120; // How deep we scan down to find the surface block

    private static final ResourceLocation MINIMAP_BACKGROUND = ResourceLocation.parse("minecraft:textures/map/map_background.png");
    private static final ResourceLocation PLAYER_ICON = ResourceLocation.parse("minecraft:textures/map/decorations/player_off_map.png");
    private static final ResourceLocation NORTH_ICON = ResourceLocation.parse("minecraft:textures/map/decorations/north.png");

    private static int frameCounter = 0;
	private static final int UPDATE_INTERVAL = 1;

    // A map to store block colors dynamically
    private static Map<Block, Integer> blockColors = new HashMap<>();

    static { // MANUALLY SET EACH BLOCK IN THE FUCKING GAME!!! OR ELSE MAP LOOKS SHIT, DOESNT WORK WITH OTHER MODS CAUSE LORD FORBIT I KNOW HOW TO CODE
    	// 1 Grass
        blockColors.put(Blocks.GRASS_BLOCK, 0xFF00FF00);
        blockColors.put(Blocks.SLIME_BLOCK, 0xFF00FF00);

        // 2 Sand
        blockColors.put(Blocks.SAND, 0xFFFFFFA3);
        blockColors.put(Blocks.SUSPICIOUS_SAND, 0xFFFFFFA3);
        blockColors.put(Blocks.SANDSTONE, 0xFFFFFFA3);
        blockColors.put(Blocks.SANDSTONE_SLAB, 0xFFFFFFA3);
        blockColors.put(Blocks.SANDSTONE_STAIRS, 0xFFFFFFA3);
        blockColors.put(Blocks.SANDSTONE_WALL, 0xFFFFFFA3);
        // Birch
        blockColors.put(Blocks.BIRCH_BUTTON, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_DOOR, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_FENCE, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_FENCE_GATE, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_HANGING_SIGN, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_LOG, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_PLANKS, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_PRESSURE_PLATE, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_SIGN, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_STAIRS, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_SLAB, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_TRAPDOOR, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_WALL_HANGING_SIGN, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_WALL_SIGN, 0xFFFFFFA3);
        blockColors.put(Blocks.BIRCH_WOOD, 0xFFFFFFA3);
        blockColors.put(Blocks.STRIPPED_BIRCH_LOG, 0xFFFFFFA3);
        blockColors.put(Blocks.STRIPPED_BIRCH_WOOD, 0xFFFFFFA3);
        // End of birch
        blockColors.put(Blocks.GLOWSTONE, 0xFFFFFFA3);
        blockColors.put(Blocks.END_STONE, 0xFFFFFFA3);
        blockColors.put(Blocks.END_STONE_BRICKS, 0xFFFFFFA3);
        blockColors.put(Blocks.END_STONE_BRICK_SLAB, 0xFFFFFFA3);
        blockColors.put(Blocks.END_STONE_BRICK_STAIRS, 0xFFFFFFA3);
        blockColors.put(Blocks.END_STONE_BRICK_WALL, 0xFFFFFFA3);
        blockColors.put(Blocks.BONE_BLOCK, 0xFFFFFFA3);
        blockColors.put(Blocks.TURTLE_EGG, 0xFFFFFFA3);
        blockColors.put(Blocks.SCAFFOLDING, 0xFFFFFFA3);
        blockColors.put(Blocks.OCHRE_FROGLIGHT, 0xFFFFFFA3);
        blockColors.put(Blocks.FROGSPAWN, 0xFFFFFFA3);

        // 3 wool
        blockColors.put(Blocks.COBWEB, 0xFFFFFFFF);
        blockColors.put(Blocks.MUSHROOM_STEM, 0xFFFFFFFF);
        blockColors.put(Blocks.WHITE_CANDLE, 0xFFFFFFFF);

        // 4 fire
        blockColors.put(Blocks.LAVA, 0xFFFF0000);
        blockColors.put(Blocks.TNT, 0xFFFF0000);
        blockColors.put(Blocks.FIRE, 0xFFFF0000);
        blockColors.put(Blocks.REDSTONE_BLOCK, 0xFFFF0000);

        // 5 ice
        blockColors.put(Blocks.ICE, 0xFFA0A0FF);
        blockColors.put(Blocks.PACKED_ICE, 0xFFA0A0FF);
        blockColors.put(Blocks.FROSTED_ICE, 0xFFA0A0FF);
        blockColors.put(Blocks.BLUE_ICE, 0xFFA0A0FF);
        
        // 6 metal
        blockColors.put(Blocks.IRON_BLOCK, 0xFFA7A7A7);
        blockColors.put(Blocks.IRON_DOOR, 0xFFA7A7A7);
        blockColors.put(Blocks.BREWING_STAND, 0xFFA7A7A7);
        blockColors.put(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, 0xFFA7A7A7);
        blockColors.put(Blocks.IRON_TRAPDOOR, 0xFFA7A7A7);
        blockColors.put(Blocks.LANTERN, 0xFFA7A7A7);
        blockColors.put(Blocks.ANVIL, 0xFFA7A7A7);
        blockColors.put(Blocks.CHIPPED_ANVIL, 0xFFA7A7A7);
        blockColors.put(Blocks.DAMAGED_ANVIL, 0xFFA7A7A7);
        blockColors.put(Blocks.GRINDSTONE, 0xFFA7A7A7);
        blockColors.put(Blocks.LODESTONE, 0xFFA7A7A7);
        blockColors.put(Blocks.HEAVY_CORE, 0xFFA7A7A7);

        // 7 plant
        blockColors.put(Blocks.OAK_SAPLING, 0xFF007C00);
        blockColors.put(Blocks.SPRUCE_SAPLING, 0xFF007C00);
        blockColors.put(Blocks.BIRCH_SAPLING, 0xFF007C00);
        blockColors.put(Blocks.JUNGLE_SAPLING, 0xFF007C00);
        blockColors.put(Blocks.ACACIA_SAPLING, 0xFF007C00);
        blockColors.put(Blocks.DARK_OAK_SAPLING, 0xFF007C00);
        blockColors.put(Blocks.CHERRY_SAPLING, 0xFF007C00);
        blockColors.put(Blocks.MANGROVE_PROPAGULE, 0xFF007C00);
        blockColors.put(Blocks.FLOWERING_AZALEA, 0xFF007C00);
        blockColors.put(Blocks.ALLIUM, 0xFF007C00);
        blockColors.put(Blocks.AZALEA, 0xFF007C00);
        blockColors.put(Blocks.AZURE_BLUET, 0xFF007C00);
        blockColors.put(Blocks.BLUE_ORCHID, 0xFF007C00);
        blockColors.put(Blocks.CORNFLOWER, 0xFF007C00);
        blockColors.put(Blocks.DANDELION, 0xFF007C00);
        blockColors.put(Blocks.LILY_OF_THE_VALLEY, 0xFF007C00);
        blockColors.put(Blocks.OXEYE_DAISY, 0xFF007C00);
        blockColors.put(Blocks.POPPY, 0xFF007C00);
        blockColors.put(Blocks.TORCHFLOWER, 0xFF007C00);
        blockColors.put(Blocks.TORCHFLOWER_CROP, 0xFF007C00);
        blockColors.put(Blocks.ORANGE_TULIP, 0xFF007C00);
        blockColors.put(Blocks.PINK_TULIP, 0xFF007C00);
        blockColors.put(Blocks.PINK_PETALS, 0xFF007C00);
        blockColors.put(Blocks.RED_TULIP, 0xFF007C00);
        blockColors.put(Blocks.RED_MUSHROOM, 0xFF007C00);
        blockColors.put(Blocks.WHITE_TULIP, 0xFF007C00);
        blockColors.put(Blocks.BROWN_MUSHROOM, 0xFF007C00);
        blockColors.put(Blocks.WITHER_ROSE, 0xFF007C00);
        blockColors.put(Blocks.LILAC, 0xFF007C00);
        blockColors.put(Blocks.PEONY, 0xFF007C00);
        blockColors.put(Blocks.PITCHER_CROP, 0xFF007C00);
        blockColors.put(Blocks.PITCHER_PLANT, 0xFF007C00);
        blockColors.put(Blocks.ROSE_BUSH, 0xFF007C00);
        blockColors.put(Blocks.SUNFLOWER, 0xFF007C00);
        blockColors.put(Blocks.CHERRY_LEAVES, 0xFF007C00);
        blockColors.put(Blocks.SPORE_BLOSSOM, 0xFF007C00);
        blockColors.put(Blocks.WHEAT, 0xFF007C00);
        blockColors.put(Blocks.SUGAR_CANE, 0xFF007C00);
        blockColors.put(Blocks.PUMPKIN_STEM, 0xFF007C00);
        blockColors.put(Blocks.MELON_STEM, 0xFF007C00);
        blockColors.put(Blocks.LILY_PAD, 0xFF007C00);
        blockColors.put(Blocks.COCOA, 0xFF007C00);
        blockColors.put(Blocks.POTATOES, 0xFF007C00);
        blockColors.put(Blocks.CARROTS, 0xFF007C00);
        blockColors.put(Blocks.BEETROOTS, 0xFF007C00);
        blockColors.put(Blocks.SWEET_BERRY_BUSH, 0xFF007C00);
        blockColors.put(Blocks.TALL_GRASS, 0xFF007C00);
        blockColors.put(Blocks.SHORT_GRASS, 0xFF007C00);
        blockColors.put(Blocks.FERN, 0xFF007C00);
        blockColors.put(Blocks.LARGE_FERN, 0xFF007C00);
        blockColors.put(Blocks.VINE, 0xFF007C00);
        blockColors.put(Blocks.OAK_LEAVES, 0xFF007C00);
        blockColors.put(Blocks.SPRUCE_LEAVES, 0xFF007C00);
        blockColors.put(Blocks.BIRCH_LEAVES, 0xFF007C00);
        blockColors.put(Blocks.ACACIA_LEAVES, 0xFF007C00);
        blockColors.put(Blocks.JUNGLE_LEAVES, 0xFF007C00);
        blockColors.put(Blocks.DARK_OAK_LEAVES, 0xFF007C00);
        blockColors.put(Blocks.MANGROVE_LEAVES, 0xFF007C00);
        blockColors.put(Blocks.AZALEA_LEAVES, 0xFF007C00);
        blockColors.put(Blocks.FLOWERING_AZALEA_LEAVES, 0xFF007C00);
        blockColors.put(Blocks.AZALEA, 0xFF007C00);
        blockColors.put(Blocks.CACTUS, 0xFF007C00);
        blockColors.put(Blocks.BAMBOO, 0xFF007C00);
        blockColors.put(Blocks.CAVE_VINES, 0xFF007C00);
        blockColors.put(Blocks.CAVE_VINES_PLANT, 0xFF007C00);
        blockColors.put(Blocks.SMALL_DRIPLEAF, 0xFF007C00);
        blockColors.put(Blocks.BIG_DRIPLEAF, 0xFF007C00);
        blockColors.put(Blocks.BIG_DRIPLEAF_STEM, 0xFF007C00);

        // 8 snow
        blockColors.put(Blocks.SNOW, 0xFFFFFFFF);
        blockColors.put(Blocks.SNOW_BLOCK, 0xFFFFFFFF);
        blockColors.put(Blocks.WHITE_BED, 0xFFFFFFFF);
        blockColors.put(Blocks.WHITE_WOOL, 0xFFFFFFFF);
        blockColors.put(Blocks.WHITE_STAINED_GLASS, 0xFFFFFFFF);
        blockColors.put(Blocks.WHITE_STAINED_GLASS_PANE, 0xFFFFFFFF);
        blockColors.put(Blocks.WHITE_CARPET, 0xFFFFFFFF);
        blockColors.put(Blocks.WHITE_SHULKER_BOX, 0xFFFFFFFF);
        blockColors.put(Blocks.WHITE_GLAZED_TERRACOTTA, 0xFFFFFFFF);
        blockColors.put(Blocks.WHITE_CONCRETE, 0xFFFFFFFF);
        blockColors.put(Blocks.WHITE_CONCRETE_POWDER, 0xFFFFFFFF);
        blockColors.put(Blocks.POWDER_SNOW, 0xFFFFFFFF);
        
        // 9 clay
        blockColors.put(Blocks.CLAY, 0xFF007C00);
        blockColors.put(Blocks.HEAVY_CORE, 0xFF007C00);
        
        // 10 dirt
        blockColors.put(Blocks.DIRT, 0xFF976D4D);
        blockColors.put(Blocks.COARSE_DIRT, 0xFF976D4D);
        blockColors.put(Blocks.FARMLAND, 0xFF976D4D);
        blockColors.put(Blocks.DIRT_PATH, 0xFF976D4D);
        blockColors.put(Blocks.GRANITE, 0xFF976D4D);
        blockColors.put(Blocks.GRANITE_SLAB, 0xFF976D4D);
        blockColors.put(Blocks.GRANITE_STAIRS, 0xFF976D4D);
        blockColors.put(Blocks.GRANITE_WALL, 0xFF976D4D);
        blockColors.put(Blocks.POLISHED_GRANITE, 0xFF976D4D);
        blockColors.put(Blocks.POLISHED_GRANITE_SLAB, 0xFF976D4D);
        blockColors.put(Blocks.POLISHED_GRANITE_STAIRS, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_BUTTON, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_DOOR, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_FENCE, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_FENCE_GATE, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_HANGING_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_LOG, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_PLANKS, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_PRESSURE_PLATE, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_STAIRS, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_SLAB, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_TRAPDOOR, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_WALL_HANGING_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_WALL_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.JUNGLE_WOOD, 0xFF976D4D);
        blockColors.put(Blocks.STRIPPED_JUNGLE_LOG, 0xFF976D4D);
        blockColors.put(Blocks.STRIPPED_JUNGLE_WOOD, 0xFF976D4D);
        blockColors.put(Blocks.JUKEBOX, 0xFF976D4D);
        blockColors.put(Blocks.BROWN_MUSHROOM_BLOCK, 0xFF976D4D);
        blockColors.put(Blocks.ROOTED_DIRT, 0xFF976D4D);
        blockColors.put(Blocks.HANGING_ROOTS, 0xFF976D4D);
        blockColors.put(Blocks.PACKED_MUD, 0xFF976D4D);

        // 11 stone

        // 12 water
        blockColors.put(Blocks.KELP, 0xFF4040FF);
        blockColors.put(Blocks.KELP_PLANT, 0xFF4040FF);
        blockColors.put(Blocks.WATER, 0xFF4040FF);
        blockColors.put(Blocks.BUBBLE_COLUMN, 0xFF4040FF);

        // 13 wood
        blockColors.put(Blocks.OAK_BUTTON, 0xFF8F7748);
        blockColors.put(Blocks.OAK_DOOR, 0xFF976D4D);
        blockColors.put(Blocks.OAK_FENCE, 0xFF976D4D);
        blockColors.put(Blocks.OAK_FENCE_GATE, 0xFF976D4D);
        blockColors.put(Blocks.OAK_HANGING_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.OAK_LOG, 0xFF976D4D);
        blockColors.put(Blocks.OAK_PLANKS, 0xFF976D4D);
        blockColors.put(Blocks.OAK_PRESSURE_PLATE, 0xFF976D4D);
        blockColors.put(Blocks.OAK_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.OAK_STAIRS, 0xFF976D4D);
        blockColors.put(Blocks.OAK_SLAB, 0xFF976D4D);
        blockColors.put(Blocks.OAK_TRAPDOOR, 0xFF976D4D);
        blockColors.put(Blocks.OAK_WALL_HANGING_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.OAK_WALL_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.OAK_WOOD, 0xFF976D4D);
        blockColors.put(Blocks.STRIPPED_OAK_LOG, 0xFF976D4D);
        blockColors.put(Blocks.STRIPPED_OAK_WOOD, 0xFF976D4D);
        blockColors.put(Blocks.NOTE_BLOCK, 0xFF976D4D);
        blockColors.put(Blocks.BOOKSHELF, 0xFF976D4D);
        blockColors.put(Blocks.CHEST, 0xFF976D4D);
        blockColors.put(Blocks.CRAFTING_TABLE, 0xFF976D4D);
        blockColors.put(Blocks.TRAPPED_CHEST, 0xFF976D4D);
        blockColors.put(Blocks.DAYLIGHT_DETECTOR, 0xFF976D4D);
        blockColors.put(Blocks.LOOM, 0xFF976D4D);
        blockColors.put(Blocks.BARREL, 0xFF976D4D);
        blockColors.put(Blocks.CARTOGRAPHY_TABLE, 0xFF976D4D);
        blockColors.put(Blocks.FLETCHING_TABLE, 0xFF976D4D);
        blockColors.put(Blocks.LECTERN, 0xFF976D4D);
        blockColors.put(Blocks.SMITHING_TABLE, 0xFF976D4D);
        blockColors.put(Blocks.COMPOSTER, 0xFF976D4D);
        blockColors.put(Blocks.BAMBOO, 0xFF976D4D);
        blockColors.put(Blocks.DEAD_BUSH, 0xFF976D4D);
        blockColors.put(Blocks.PETRIFIED_OAK_SLAB, 0xFF976D4D);
        blockColors.put(Blocks.BEE_NEST, 0xFF976D4D);
        blockColors.put(Blocks.BEEHIVE, 0xFF976D4D);

        // 14 quartz
        blockColors.put(Blocks.DIORITE, 0xFFFFFFF5);
        blockColors.put(Blocks.DIORITE_SLAB, 0xFFFFFFF5);
        blockColors.put(Blocks.DIORITE_STAIRS, 0xFFFFFFF5);
        blockColors.put(Blocks.DIORITE_WALL, 0xFFFFFFF5);
        blockColors.put(Blocks.POLISHED_DIORITE, 0xFFFFFFF5);
        blockColors.put(Blocks.POLISHED_DIORITE_SLAB, 0xFFFFFFF5);
        blockColors.put(Blocks.POLISHED_DIORITE_STAIRS, 0xFFFFFFF5);
        blockColors.put(Blocks.BIRCH_BUTTON, 0xFF8F7748);
        blockColors.put(Blocks.BIRCH_DOOR, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_FENCE, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_FENCE_GATE, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_HANGING_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_LOG, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_PLANKS, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_PRESSURE_PLATE, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_STAIRS, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_SLAB, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_TRAPDOOR, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_WALL_HANGING_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_WALL_SIGN, 0xFF976D4D);
        blockColors.put(Blocks.BIRCH_WOOD, 0xFF976D4D);
        blockColors.put(Blocks.STRIPPED_BIRCH_LOG, 0xFF976D4D);
        blockColors.put(Blocks.STRIPPED_BIRCH_WOOD, 0xFF976D4D);
        blockColors.put(Blocks.QUARTZ_BLOCK, 0xFFFFFFF5);
        blockColors.put(Blocks.QUARTZ_BRICKS, 0xFFFFFFF5);
        blockColors.put(Blocks.QUARTZ_PILLAR, 0xFFFFFFF5);
        blockColors.put(Blocks.QUARTZ_SLAB, 0xFFFFFFF5);
        blockColors.put(Blocks.QUARTZ_STAIRS, 0xFFFFFFF5);
        blockColors.put(Blocks.SEA_LANTERN, 0xFFFFFFF5);
        blockColors.put(Blocks.TARGET, 0xFFFFFFF5);

        // 15 color orange
        blockColors.put(Blocks.ACACIA_BUTTON, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_DOOR, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_FENCE, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_FENCE_GATE, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_HANGING_SIGN, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_LOG, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_PLANKS, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_PRESSURE_PLATE, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_SIGN, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_STAIRS, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_SLAB, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_TRAPDOOR, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_WALL_HANGING_SIGN, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_WALL_SIGN, 0xFFD87F33);
        blockColors.put(Blocks.ACACIA_WOOD, 0xFFD87F33);
        blockColors.put(Blocks.STRIPPED_ACACIA_LOG, 0xFFD87F33);
        blockColors.put(Blocks.STRIPPED_ACACIA_WOOD, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_BANNER, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_BED, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_CANDLE, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_CARPET, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_CONCRETE, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_CONCRETE_POWDER, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_GLAZED_TERRACOTTA, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_STAINED_GLASS, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_STAINED_GLASS_PANE, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_TERRACOTTA, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_WALL_BANNER, 0xFFD87F33);
        blockColors.put(Blocks.ORANGE_WOOL, 0xFFD87F33);
        blockColors.put(Blocks.RED_SAND, 0xFFD87F33);
        blockColors.put(Blocks.RED_SANDSTONE, 0xFFD87F33);
        blockColors.put(Blocks.RED_SANDSTONE_SLAB, 0xFFD87F33);
        blockColors.put(Blocks.RED_SANDSTONE_STAIRS, 0xFFD87F33);
        blockColors.put(Blocks.RED_SANDSTONE_WALL, 0xFFD87F33);
        blockColors.put(Blocks.CARVED_PUMPKIN, 0xFFD87F33);
        blockColors.put(Blocks.PUMPKIN, 0xFFD87F33);
        blockColors.put(Blocks.JACK_O_LANTERN, 0xFFD87F33);
        blockColors.put(Blocks.TERRACOTTA, 0xFFD87F33);
        blockColors.put(Blocks.HONEY_BLOCK, 0xFFD87F33);
        blockColors.put(Blocks.HONEYCOMB_BLOCK, 0xFFD87F33);
        blockColors.put(Blocks.CHISELED_COPPER, 0xFFD87F33);
        blockColors.put(Blocks.COPPER_BLOCK, 0xFFD87F33);
        blockColors.put(Blocks.COPPER_BULB, 0xFFD87F33);
        blockColors.put(Blocks.COPPER_DOOR, 0xFFD87F33);
        blockColors.put(Blocks.COPPER_GRATE, 0xFFD87F33);
        blockColors.put(Blocks.COPPER_TRAPDOOR, 0xFFD87F33);
        blockColors.put(Blocks.WAXED_CHISELED_COPPER, 0xFFD87F33);
        blockColors.put(Blocks.WAXED_COPPER_BLOCK, 0xFFD87F33);
        blockColors.put(Blocks.WAXED_COPPER_BULB, 0xFFD87F33);
        blockColors.put(Blocks.WAXED_COPPER_DOOR, 0xFFD87F33);
        blockColors.put(Blocks.WAXED_COPPER_GRATE, 0xFFD87F33);
        blockColors.put(Blocks.WAXED_COPPER_TRAPDOOR, 0xFFD87F33);
        blockColors.put(Blocks.WAXED_CUT_COPPER, 0xFFD87F33);
        blockColors.put(Blocks.WAXED_CUT_COPPER_SLAB, 0xFFD87F33);
        blockColors.put(Blocks.WAXED_CUT_COPPER_STAIRS, 0xFFD87F33);
        blockColors.put(Blocks.LIGHTNING_ROD, 0xFFD87F33);
        blockColors.put(Blocks.RAW_COPPER_BLOCK, 0xFFD87F33);
    }

@SubscribeEvent(priority = EventPriority.NORMAL)
public static void eventHandler(RenderGuiEvent.Pre event) {
    // Get screen dimensions
    int screenWidth = event.getGuiGraphics().guiWidth();
    int screenHeight = event.getGuiGraphics().guiHeight();

    // Get player and world
    Player player = Minecraft.getInstance().player;
    if (player == null) return;

    // Check if the UI is hidden (F1 pressed)
    boolean isGuiHidden = Minecraft.getInstance().options.hideGui;

    // Check if the player has a compass
    boolean hasCompass = player.getOffhandItem().getItem() == Items.COMPASS;

    // Only render the minimap if UI is not hidden and showMinimap is true
    if (isGuiHidden || !ToggleMinimapCommand.showMinimap || !hasCompass) {
        return;
    }

    Level world = player.getCommandSenderWorld();
    double playerX = player.getX();
    double playerY = player.getY();
    double playerZ = player.getZ();

    // Minimap positioning
    int minimapX = 10, minimapY = 10;
    int backgroundX = minimapX - 6, backgroundY = minimapY - 6;
    int backgroundWidth = MINIMAP_SIZE + 10, backgroundHeight = MINIMAP_SIZE + 10;

    event.getGuiGraphics().blit(MINIMAP_BACKGROUND, 4, 4, 0, 0, 110, 110, 110, 110);

    // Precompute scale factor
    int scaleFactor = MINIMAP_SIZE / (MINIMAP_RADIUS * 2);

    // Mutable position for reuse
    BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
    
	if (true) 
	{
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					CompassReturnsProcedure.execute(player), 5, 120, -1, false);
	}
	
    // Loop over minimap area
    for (int x = -MINIMAP_RADIUS; x <= MINIMAP_RADIUS; x++) 
    {
        for (int z = -MINIMAP_RADIUS; z <= MINIMAP_RADIUS; z++) 
        {
        	// Get surface block position
            	BlockPos surfacePos = getSurfaceBlock(world, (int) (playerX + x), (int) (playerZ + z), mutablePos);

            	// Get block color
            	int blockColor = getBlockColor(world, world.getBlockState(surfacePos).getBlock(), surfacePos.getY());
	
           		 // Compute minimap pixel position
            	int minimapXPos = minimapX + (x + MINIMAP_RADIUS) * scaleFactor;
            	int minimapZPos = minimapY + (z + MINIMAP_RADIUS) * scaleFactor;

            	event.getGuiGraphics().fill(minimapXPos, minimapZPos, minimapXPos + 3, minimapZPos + 3, blockColor); // change the numbers to set the pixel sizes
        }
    }

    frameCounter++;
   // I barely know how this shit works.
    // Draw player position on minimap
    int playerMinimapX = minimapX + MINIMAP_RADIUS * scaleFactor;
    int playerMinimapY = minimapY + MINIMAP_RADIUS * scaleFactor;
    int playerSize = 10;
    
    event.getGuiGraphics().blit(PLAYER_ICON, playerMinimapX, playerMinimapY, 0, 0, playerSize, playerSize, playerSize, playerSize);

    // Draw North Marker (Arrow or icon)
    int northX = 1;
    int northY = 7;
    int northSize = 10;
    
    
    event.getGuiGraphics().blit(NORTH_ICON, northX, northY, 0, 0, northSize, northSize, northSize, northSize);
}


    // Optimized method to get the surface block
    private static BlockPos getSurfaceBlock(Level world, int x, int z, BlockPos.MutableBlockPos mutablePos) {
    int maxY = world.getMaxBuildHeight();
    int minY = world.getMinBuildHeight();

    for (int y = maxY - 1; y >= minY; y--) {
        mutablePos.set(x, y, z);
        if (!world.getBlockState(mutablePos).isAir()) {
            return mutablePos.immutable(); // Return an immutable copy
        }
    }
    return mutablePos.set(x, minY, z).immutable(); // Default to lowest point if no block is found
    }

    // Get block color with default
    private static int getBlockColor(Level world, Block block, int y) {
        int baseColor = blockColors.getOrDefault(block, 0xFF707070); // Default to stone color if block colour isn't mapped (cant be fucked to map stone manually)

        // Extract RGB components from the base color
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;

        // Get min and max world height dynamically
        int minY = world.getMinBuildHeight(); // Example: -64
        int maxY = world.getMaxBuildHeight(); // Example: 320

        // Normalize height (0.0 = lowest point, 1.0 = highest point)
        float heightFactor = (float) (y - minY) / (maxY - minY);

        // Stronger contrast
        float contrastFactor = 0.095f + (heightFactor * 1.9f); // fucked if I know HOW this works but it causes a slight contrast based off the two numbers..

        // Apply contrast factor separately to R, G, and B
        r = Math.min(255, Math.max(0, (int) (r * contrastFactor)));
        g = Math.min(255, Math.max(0, (int) (g * contrastFactor)));
        b = Math.min(255, Math.max(0, (int) (b * contrastFactor)));
    
        return (0xFF << 24) | (r << 16) | (g << 8) | b; // Return ARGB color
    }
// I'm 11 hours straight into coding...I think I figured out what it means to be insane....Ive broken my hand again punching shit when i GET ANOTHER FUCKING ERRORRRRRRRR!

// oh also I'm doing this ON MY 22 birthday so happy birthday to me I guess, instead of partying and getting drunk im writing a mod for minecraft probally 10 people will play....happy fucking birthday
}
