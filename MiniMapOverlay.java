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

import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;


import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber({Dist.CLIENT})
public class MiniMapOverlay {

    // Minimap size constants
    private static final int MINIMAP_SIZE = 100; // Minimap size in pixels
    // WARNING DO NOT SET ANY HIGHER THEN 16 or it will cause LAG
    private static final int MINIMAP_RADIUS = 16; // How many blocks to show around the player
    private static final int SURFACE_SCAN_DEPTH = 120; // How deep we scan down to find the surface block

    // A map to store block colors dynamically
    private static Map<Block, Integer> blockColors = new HashMap<>();

    static {
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

        // 11 stone

        // 12 water
        blockColors.put(Blocks.KELP, 0xFF4040FF);
        blockColors.put(Blocks.KELP_PLANT, 0xFF4040FF);
        blockColors.put(Blocks.WATER, 0xFF4040FF);
        blockColors.put(Blocks.BUBBLE_COLUMN, 0xFF4040FF);
        
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        // Get the screen dimensions
        int screenWidth = event.getGuiGraphics().guiWidth();
        int screenHeight = event.getGuiGraphics().guiHeight();

        // Get the player's position and world
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Get the player's world (use getCommandSenderWorld instead of player.level)
        Level world = player.getCommandSenderWorld();  // Use this method to get the world
        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();

        // Set the minimap position at the top-left of the screen
        int minimapX = 10; // Minimap X position (pixels)
        int minimapY = 10; // Minimap Y position (pixels)

        int backgroundX = minimapX - 7;  // Background Horizontal offset 
        int backgroundY = minimapY - 7;  // Background Vertical offset
        int backgroundWidth = MINIMAP_SIZE + 10;  // Background width
        int backgroundHeight = MINIMAP_SIZE + 10;  // Background height

        // Render the minimap background
        event.getGuiGraphics().blit(ResourceLocation.parse("minecraft:textures/map/map_background.png"), backgroundX, backgroundY, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);

        // Loop over the area around the player to gather world data
        for (int x = -MINIMAP_RADIUS; x <= MINIMAP_RADIUS; x++) {
            for (int z = -MINIMAP_RADIUS; z <= MINIMAP_RADIUS; z++) {
                // Calculate the position in the world
                BlockPos pos = new BlockPos((int)(playerX + x), (int)(playerY), (int)(playerZ + z));  // Cast to int

                // Get the surface block at this X, Z position
                BlockPos surfacePos = getSurfaceBlock(world, pos);

                // Get the block at the surface position
                Block block = world.getBlockState(surfacePos).getBlock();

                // Determine the color for the block using the block-to-color map
                int blockColor = getBlockColor(block);

                // Draw the block as a pixel on the minimap
                int minimapXPos = minimapX + (x + MINIMAP_RADIUS) * (MINIMAP_SIZE / (MINIMAP_RADIUS * 2));
                int minimapZPos = minimapY + (z + MINIMAP_RADIUS) * (MINIMAP_SIZE / (MINIMAP_RADIUS * 2));

                event.getGuiGraphics().fill(minimapXPos, minimapZPos, minimapXPos + 3, minimapZPos + 3, blockColor);
            }
        }

        // Draw the player icon on the minimap (e.g., a red dot)
        int playerMinimapX = minimapX + (MINIMAP_RADIUS) * (MINIMAP_SIZE / (MINIMAP_RADIUS * 2));
        int playerMinimapY = minimapY + (MINIMAP_RADIUS) * (MINIMAP_SIZE / (MINIMAP_RADIUS * 2));

        event.getGuiGraphics().fill(playerMinimapX, playerMinimapY, playerMinimapX + 3, playerMinimapY + 3, 0xFF000000); // Red dot

        event.getGuiGraphics().blit(ResourceLocation.parse("minecraft:textures/map/decorations/player_off_map.png"), playerMinimapX, playerMinimapY, 0, 0, 8, 8, 8, 8);

    }


    // A simple method to get the color for different blocks
    private static int getBlockColor(Block block) {
        return blockColors.getOrDefault(block, 0xFF000000); // Default to black if not found
    }

    // Get the highest block at the given X and Z coordinates (surface block)
    private static BlockPos getSurfaceBlock(Level world, BlockPos pos) {
        // Scan from the max build height down to find the first solid block
        for (int y = world.getMaxBuildHeight() - 1; y >= 0; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            Block block = world.getBlockState(checkPos).getBlock();

            // Check if the block is solid and can be considered as a surface block
            if (!block.equals(Blocks.AIR)) {
                return checkPos;  // This is the surface block
            }
        }
        // If no surface block is found (which shouldn't happen), return the original position
        return pos;
    }
}
