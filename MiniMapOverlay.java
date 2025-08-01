package com.oakmods.oaksrework.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.oakmods.oaksrework.configuration.ClientConfiguration;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class MinimapOverlay {

    private static DynamicTexture minimapTexture;
    private static ResourceLocation minimapLocation;
    private static int[] colorBuffer;
    private static int lastSize = -1;
    private static long lastUpdateTime = 0;

    private static final List<PlayerIconData> cachedPlayerIcons = new ArrayList<>();

    private static class PlayerIconData {
        final float screenX, screenY, rotation;
        PlayerIconData(float screenX, float screenY, float rotation) {
            this.screenX = screenX;
            this.screenY = screenY;
            this.rotation = rotation;
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || ClientConfiguration.MINIMAP_HIDE.get()) return;

        int SIZE = ClientConfiguration.MINIMAP_SCALE.get().intValue();
        if (SIZE <= 0) return;

        if (minimapTexture == null || SIZE != lastSize) {
            minimapTexture = new DynamicTexture(SIZE, SIZE, true);
            minimapLocation = mc.getTextureManager().register("minimap", minimapTexture);
            colorBuffer = new int[SIZE * SIZE];
            lastSize = SIZE;
        }

        Level world = player.level();
        int offsetX = 4;
        int offsetY = 4;
        BlockPos center = player.blockPosition();
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastUpdateTime >= 250) {
            lastUpdateTime = currentTime;

            // Generate minimap
            for (int dz = -SIZE / 2; dz < SIZE / 2; dz++) {
                for (int dx = -SIZE / 2; dx < SIZE / 2; dx++) {
                    int worldX = center.getX() + dx;
                    int worldZ = center.getZ() + dz;
                    int groundY = world.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                    int sampleY = Math.min(groundY + 3, world.getMaxBuildHeight() - 1);

                    BlockPos pos = new BlockPos(worldX, sampleY, worldZ);
                    BlockState blockState = world.getBlockState(pos);
                    if (blockState.isAir()) {
                        pos = new BlockPos(worldX, groundY - 1, worldZ);
                        blockState = world.getBlockState(pos);
                    }

                    int packedId = 0;
                    if (!blockState.isAir()) {
                        MapColor mapColor = blockState.getMapColor(world, pos);
                        packedId = (mapColor.id << 2) | 1;
                    }

                    int color = MapColor.getColorFromPackedId(packedId);

                    // --- Biome Tinting with Brightening ---
                    int tintR = 255, tintG = 255, tintB = 255;

                    // Apply biome tint for foliage/grass-like blocks
                    boolean applyBiomeTint = false;
                    MapColor defaultMapColor = blockState.getBlock().defaultMapColor();
                    if (defaultMapColor == MapColor.PLANT || defaultMapColor == MapColor.GRASS) {
                        applyBiomeTint = true;
                    }

                    if (applyBiomeTint) {
                        var biome = world.getBiome(pos).value();
                        int tintColor = biome.getFoliageColor(); // or biome.getGrassColor(pos.getX(), pos.getZ())

                        tintR = (tintColor >> 16) & 0xFF;
                        tintG = (tintColor >> 8) & 0xFF;
                        tintB = tintColor & 0xFF;

                        // Brighten tint by blending with white
                        float brightenFactor = 0.5f; // Adjust this from 0 to 1 as needed
                        tintR = (int) (tintR + (255 - tintR) * brightenFactor);
                        tintG = (int) (tintG + (255 - tintG) * brightenFactor);
                        tintB = (int) (tintB + (255 - tintB) * brightenFactor);
                    }

                    int r = ((color >> 16) & 0xFF) * tintR / 255;
                    int g = ((color >> 8) & 0xFF) * tintG / 255;
                    int b = (color & 0xFF) * tintB / 255;

                    // --- Terrain height-based lighting ---
                    int heightHere = groundY;
                    int heightRight = world.getHeight(Heightmap.Types.WORLD_SURFACE, worldX + 1, worldZ);
                    int heightDown  = world.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ + 1);
                    int slopeX = heightHere - heightRight;
                    int slopeZ = heightHere - heightDown;
                    int slope = slopeX + slopeZ;

                    float brightness = 1.0f - (slope * 0.05f);
                    brightness = Mth.clamp(brightness, 0.6f, 1.0f);

                    r = (int)(r * brightness);
                    g = (int)(g * brightness);
                    b = (int)(b * brightness);

                    int finalColor = (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);

                    int px = dx + SIZE / 2;
                    int pz = dz + SIZE / 2;

                    if (px >= 0 && px < SIZE && pz >= 0 && pz < SIZE) {
                        colorBuffer[px + pz * SIZE] = finalColor;
                    }
                }
            }

            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    minimapTexture.getPixels().setPixelRGBA(j, i, colorBuffer[j + i * SIZE]);
                }
            }
            minimapTexture.upload();

            // --- Cache only nearby players (better performance) ---
            cachedPlayerIcons.clear();
            double halfSize = SIZE / 2.0;
            double px = player.getX();
            double pz = player.getZ();

            for (Player other : world.players()) {
                if (other == player) continue;

                double dx = other.getX() - px;
                double dz = other.getZ() - pz;

                if (Math.abs(dx) <= halfSize && Math.abs(dz) <= halfSize) {
                    cachedPlayerIcons.add(new PlayerIconData(
                        (float) (offsetX + halfSize + dx),
                        (float) (offsetY + halfSize + dz),
                        180 + other.getYRot()
                    ));
                }
            }
        }

        int backgroundOffset = 4;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        event.getGuiGraphics().blit(
            ResourceLocation.withDefaultNamespace("textures/map/map_background.png"),
            offsetX - backgroundOffset, offsetY - backgroundOffset,
            0, 0, SIZE + (backgroundOffset * 2), SIZE + (backgroundOffset * 2),
            SIZE + (backgroundOffset * 2), SIZE + (backgroundOffset * 2)
        );

        event.getGuiGraphics().blit(minimapLocation, offsetX, offsetY, 0, 0, SIZE, SIZE, SIZE, SIZE);

        PoseStack poseStack = event.getGuiGraphics().pose();

        // Player icon (you)
        float iconSize = 4f;
        float scaleFactor = 2.0f; // your desired scale

        poseStack.pushPose();

       // Move to the icon's center position on screen
        poseStack.translate(offsetX + SIZE / 2f, offsetY + SIZE / 2f, 0);

       // Rotate around Z axis by player rotation
       poseStack.mulPose(Axis.ZP.rotationDegrees(180 + player.getYRot()));

       // Scale the icon
       poseStack.scale(scaleFactor, scaleFactor, 1f);

       // Now move the drawing position so the icon is centered on that rotation/scaling origin
       poseStack.translate(-iconSize / 2f, -iconSize / 2f, 0);

       event.getGuiGraphics().blit(
           ResourceLocation.withDefaultNamespace("textures/map/decorations/player.png"),
           0, 0, 0, 0,
           (int)iconSize, (int)iconSize, (int)iconSize, (int)iconSize
       );

       poseStack.popPose();

        // Other players
        for (PlayerIconData icon : cachedPlayerIcons) {
           poseStack.pushPose();

           // Translate to icon position on screen
           poseStack.translate(icon.screenX, icon.screenY, 0);

           // Rotate
           poseStack.mulPose(Axis.ZP.rotationDegrees(icon.rotation));

           // Scale
           poseStack.scale(scaleFactor, scaleFactor, 1f);

           // Center icon on pivot point
           poseStack.translate(-iconSize / 2f, -iconSize / 2f, 0);

           event.getGuiGraphics().blit(
               ResourceLocation.withDefaultNamespace("textures/map/decorations/player.png"),
               0, 0, 0, 0,
               (int)iconSize, (int)iconSize, (int)iconSize, (int)iconSize
           );

           poseStack.popPose();
       }



        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}
