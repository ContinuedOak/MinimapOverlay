/*
 This code was written for Neo-Forge 1.21.1
*/
package com.oakmods.minimap.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.LightLayer;
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

import com.oakmods.minimap.procedures.ReturnFacingProcedure;
import com.oakmods.minimap.procedures.ReturnCoordsProcedure;
import com.oakmods.minimap.procedures.DisplayDayProcedure;
import com.oakmods.minimap.procedures.DisplayCoordsProcedure;
import com.oakmods.minimap.procedures.ReturnCavemodeProcedure;
import com.oakmods.minimap.configuration.ClientConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.neoforged.neoforge.common.damagesource.IScalingFunction;
import net.neoforged.neoforge.common.damagesource.IScalingFunction;

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
        final UUID uuid;

        PlayerIconData(float screenX, float screenY, float rotation, UUID uuid) {
            this.screenX = screenX;
            this.screenY = screenY;
            this.rotation = rotation;
            this.uuid = uuid;        
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || ClientConfiguration.MINIMAP_HIDE.get()) return;

        int SIZE = ((Double) ClientConfiguration.MINIMAP_SCALE.get()).intValue();
        if (SIZE <= 0) return;

        int offsetX = ClientConfiguration.MINIMAP_TOPL.get()
            ? 4
            : mc.getWindow().getGuiScaledWidth() - SIZE - 4;
            
        int offsetY = 4;

        Level world = player.level();

    // --- Display Day Counter ---
    if (!ClientConfiguration.REQUIRE_CLOCK.get() || DisplayDayProcedure.execute(player)) {
        long day = world.getDayTime() / 24000L + 1;
        String dayText = "Day " + day;
        boolean showCoords = !ClientConfiguration.REQUIRE_COMPASS.get() || DisplayCoordsProcedure.execute(player);
        int dayTextY = offsetY + SIZE + (showCoords ? 30 : 10);

        int textX = offsetX;
        if (!ClientConfiguration.MINIMAP_TOPL.get()) {
            int textWidth = mc.font.width(dayText);
            textX = offsetX + SIZE - textWidth;
        }

        event.getGuiGraphics().drawString(mc.font, dayText, textX + 1, dayTextY + 1, 0x000000, false);
        event.getGuiGraphics().drawString(mc.font, dayText, textX, dayTextY, 0xFFFFFF, false);
    }

    // --- Display Coords/Facing ---
    if (!ClientConfiguration.REQUIRE_COMPASS.get() || DisplayCoordsProcedure.execute(player)) {
        String coords = ReturnCoordsProcedure.execute(player);
        String facing = ReturnFacingProcedure.execute(player);
        int textX = offsetX;
        if (!ClientConfiguration.MINIMAP_TOPL.get()) {
            int textWidth = mc.font.width(coords);
            textX = offsetX + SIZE - textWidth;
        }
        int textY = offsetY + SIZE + 10;

        event.getGuiGraphics().drawString(mc.font, coords, textX + 1, textY + 1, 0x000000, false);
        event.getGuiGraphics().drawString(mc.font, facing, textX + 1, textY + 11, 0x000000, false);
        event.getGuiGraphics().drawString(mc.font, coords, textX, textY, 0xFFFFFF, false);
        event.getGuiGraphics().drawString(mc.font, facing, textX, textY + 10, 0xFFFFFF, false);
    }

        if (minimapTexture == null || SIZE != lastSize) {
            minimapTexture = new DynamicTexture(SIZE, SIZE, true);
            minimapLocation = mc.getTextureManager().register("minimap", minimapTexture);
            colorBuffer = new int[SIZE * SIZE];
            lastSize = SIZE;
        }

        // --- Sample Block ---
        BlockPos center = player.blockPosition();
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastUpdateTime >= ClientConfiguration.MINIMAP_REFRESH.get()) {
            lastUpdateTime = currentTime;

            boolean isNether = world.dimension() == Level.NETHER;
            boolean isEnd = world.dimension() == Level.END;
            boolean isOverworld = world.dimension() == Level.OVERWORLD;
            
            boolean isCavemode = ReturnCavemodeProcedure.execute(
                player.level(), 
                player.getX(), 
                player.getY() + 7, 
                player.getZ()
            );
            
            for (int dz = -SIZE / 2; dz < SIZE / 2; dz++) {
                for (int dx = -SIZE / 2; dx < SIZE / 2; dx++) {
                    int worldX = center.getX() + dx;
                    int worldZ = center.getZ() + dz;

                    int sampleY;
                    int groundY;

                    if (isNether) {
                        sampleY = center.getY();
                        groundY = sampleY;
                    } else{
                    	if(isCavemode && ClientConfiguration.CAVE_MODE.get() && isOverworld){
                    		sampleY = center.getY();
                    		groundY = sampleY;
                    	}else{
                    		groundY = world.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                    		sampleY = Math.min(groundY + 3, world.getMaxBuildHeight() - 1);
                    	}
                    }

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
                    MapColor defaultMapColor = blockState.getBlock().defaultMapColor();
                    boolean applyBiomeTint = defaultMapColor == MapColor.PLANT || defaultMapColor == MapColor.GRASS || defaultMapColor == MapColor.WATER;

                    if (applyBiomeTint) {
                        int tintColor = world.getBiome(pos).value().getFoliageColor();
                        tintR = (tintColor >> 16) & 0xFF;
                        tintG = (tintColor >> 8) & 0xFF;
                        tintB = tintColor & 0xFF;

                        // Brighten tint by blending with white
                        float brightenFactor = ClientConfiguration.MINIMAP_BIOME_BRIGHTNESS.get().floatValue();
                        tintR = (int)(tintR + (255 - tintR) * brightenFactor);
                        tintG = (int)(tintG + (255 - tintG) * brightenFactor);
                        tintB = (int)(tintB + (255 - tintB) * brightenFactor);
                    }

                    int r = ((color >> 16) & 0xFF) * tintR / 255;
                    int g = ((color >> 8) & 0xFF) * tintG / 255;
                    int b = (color & 0xFF) * tintB / 255;

                    // --- Terrain height-based lighting ---
                    int heightHere = groundY;
                    int heightRight = isNether ? groundY : world.getHeight(Heightmap.Types.WORLD_SURFACE, worldX + 1, worldZ);
                    int heightDown = isNether ? groundY : world.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ + 1);
                    int slope = (heightHere - heightRight) + (heightHere - heightDown);

                    int blockLight = world.getBrightness(LightLayer.BLOCK, pos);
                    
                    // Convert to 0–1 range
                    float blockBrightness = blockLight / 15f;
                    float skyBrightness;
                    float celestialAngle = world.getTimeOfDay(1.0f);

                    skyBrightness = Mth.clamp(Mth.cos(celestialAngle * 2f * (float)Math.PI) * 0.5f + 0.5f, 0f, 1f);
                    
                    float brightness = Mth.clamp(1.0f - (slope * 0.05f), 0.6f, 1.0f);

                    float totalBrightness = 0.6f * skyBrightness + 0.4f * blockBrightness;

                    if(ClientConfiguration.DYNAMIC_LIGHT.get() && !isCavemode){
                    	r *= brightness + 1 * totalBrightness;
                    	g *= brightness + 1 * totalBrightness;
                    	b *= brightness + 1 * totalBrightness;
                    }else{
                    	r *= brightness;
                    	g *= brightness;
                    	b *= brightness;
                    }

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
                            (float)(offsetX + halfSize + dx),
                            (float)(offsetY + halfSize + dz),
                            180 + other.getYRot(),
                            other.getUUID()
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
                ResourceLocation.withDefaultNamespace(ClientConfiguration.MINIMAP_BG.get()), // Minimap Background File Path
                offsetX - backgroundOffset, offsetY - backgroundOffset,
                0, 0, SIZE + (backgroundOffset * 2), SIZE + (backgroundOffset * 2),
                SIZE + (backgroundOffset * 2), SIZE + (backgroundOffset * 2)
        );

        event.getGuiGraphics().blit(minimapLocation, offsetX, offsetY, 0, 0, SIZE, SIZE, SIZE, SIZE);

        PoseStack poseStack = event.getGuiGraphics().pose();

        float iconSize = 4f;
        float scaleFactor = 2.0f;

        // Player icon (you) – not tinted
        poseStack.pushPose();
        poseStack.translate(offsetX + SIZE / 2f, offsetY + SIZE / 2f, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180 + player.getYRot()));
        poseStack.scale(scaleFactor, scaleFactor, 1f);
        poseStack.translate(-iconSize / 2f, -iconSize / 2f, 0);

        event.getGuiGraphics().blit(
                ResourceLocation.withDefaultNamespace(ClientConfiguration.USER_ICON.get()), // Client's Icon
                0, 0, 0, 0, (int)iconSize, (int)iconSize, (int)iconSize, (int)iconSize
        );
        poseStack.popPose();

        // Other players – colour set by UUID (This allows for unquie colours on players and also keeps their colors the exact same between sessions and worlds)
        for (PlayerIconData icon : cachedPlayerIcons) {
        	
        	if(ClientConfiguration.HIDE_ONLINE_PLAYERS.get()) // If true, this is stop the rest of the code from running also saving resources on the users end
        	   return;
            poseStack.pushPose();
            poseStack.translate(icon.screenX, icon.screenY, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(icon.rotation));
            poseStack.scale(scaleFactor, scaleFactor, 1f);
            poseStack.translate(-iconSize / 2f, -iconSize / 2f, 0);

            int hash = icon.uuid.hashCode();
            float red = ((hash >> 16) & 0xFF) / 255f;
            float green = ((hash >> 8) & 0xFF) / 255f;
            float blue = (hash & 0xFF) / 255f;

            RenderSystem.setShaderColor(red, green, blue, 1f);
            event.getGuiGraphics().blit(
                    ResourceLocation.withDefaultNamespace(ClientConfiguration.PLAYER_ICON.get()), // Other's Icon
                    0, 0, 0, 0, (int)iconSize, (int)iconSize, (int)iconSize, (int)iconSize
            );
            RenderSystem.setShaderColor(1, 1, 1, 1);
            poseStack.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}
