package data.methods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.hullmods.Meng_MingGodCenter;
import data.methods.shaders.ShaderUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;
import java.util.ArrayList;

/**
 * Spatial exchange rendering and resolution plugin for MingGod's tactical system.
 */
public class Meng_MingGodSpaceCutPlugin implements CombatLayeredRenderingPlugin {
    public static final float SPACE_RADIUS = 3000f;
    public static final String ACTIVE_PLUGIN_KEY = "Meng_MingGodSpaceCutPlugin";
    private static final String TIME_MULT_KEY = "Meng_MingGodSpaceCutPluginTime";

    private static final float TOTAL_CUT_TIME = 1f;
    private static final float HOLD_TIME = 8.5f;
    private static final float FADE_TIME = 0.5f;
    private static final int SECTOR_COUNT = 8;
    private static final float SECTOR_ANGLE = (float) (Math.PI * 2d / SECTOR_COUNT);
    private static final float SWAP_FLASH_DURATION = 0.3f;
    private static final float RESOLVE_START = 9.3f;
    private static final float RESOLVE_FLASH_DURATION = 0.5f;
    private static final float CAMERA_RECOVERY_DURATION = 0.4f;
    private static final int MAX_SWAPS = 3;
    private static final float BORDER_DAMAGE = 50f;
    private static final float DAMAGE_POINT_SPACING = 5f;
    private static final int DAMAGE_PULSES = 2;
    private static final float DAMAGE_PULSE_INTERVAL = 0.1f;
    private static final float RENDER_RADIUS_MULT = 1.2f;
    private static final float VISUAL_ARC_INTERVAL = 2.2f;
    private static final float VISUAL_ARC_SPEED = 14000f;
    private static final Color AURA_COLOR = new Color(148, 36, 255);
    private static final Color CORE_COLOR = new Color(245, 214, 255);
    private static final Color ARC_FRINGE_COLOR = new Color(112, 28, 255, 190);
    private static final Color ARC_CORE_COLOR = new Color(245, 220, 255, 255);

    private final Vector2f center;
    private final ShipAPI source;
    private final float timelineMult;
    private final String timeMultKey;
    private float elapsed;
    private boolean expired;
    private boolean glInitialized;
    private int shaderProgram;
    private ShaderUtil.VAOData vao;
    private int modelMatrixLoc;
    private int sizeLoc;
    private int timeLoc;
    private int alphaLoc;
    private int auraColorLoc;
    private int coreColorLoc;
    private int screenTextureLoc;
    private int screenTextureId;
    private int screenTextureWidth;
    private int screenTextureHeight;
    private IntBuffer viewportBuffer;
    private final int[] sectorMap = new int[SECTOR_COUNT];
    private IntBuffer sectorMapBuffer;
    private int sectorMapLoc;
    private int hoverSector = -1;
    private int selectedSector = -1;
    private int swapFirstSector = -1;
    private int swapSecondSector = -1;
    private int hoverSectorLoc;
    private int selectedSectorLoc;
    private int swapFirstSectorLoc;
    private int swapSecondSectorLoc;
    private int swapFlashLoc;
    private int resolveFlashLoc;
    private boolean kWasDown;
    private float swapFlash;
    private float resolveFlash;
    private int swapCount;
    private ResolveState resolveState = ResolveState.INPUT;
    private enum ResolveState { INPUT, DAMAGE, FLASH, TELEPORTED }
    private final ArrayList<Vector2f> damagePoints = new ArrayList<Vector2f>();
    private int damagePulse;
    private float damagePulseTimer;
    private boolean activationVisualTriggered;
    private float visualArcTimer;
    private int queuedVisualArcs;
    private float queuedArcThickness;
    private boolean spatialRenderingComplete;
    private boolean cameraLocked;
    private float lockedViewMult;
    private float expandedVisibleWidth;
    private float expandedVisibleHeight;
    private boolean cameraRecovering;
    private float cameraRecoveryElapsed;

    public Meng_MingGodSpaceCutPlugin(Vector2f center, ShipAPI source, float globalTimeMult) {
        this.center = new Vector2f(center);
        this.source = source;
        this.timelineMult = 1f / globalTimeMult;
        this.timeMultKey = TIME_MULT_KEY + System.identityHashCode(this);
        Global.getCombatEngine().getTimeMult().modifyMult(timeMultKey, globalTimeMult);
        source.setCustomData(ACTIVE_PLUGIN_KEY, this);
        for (int i = 0; i < SECTOR_COUNT; i++) {
            sectorMap[i] = i;
        }
    }

    @Override
    public void init(CombatEntityAPI entity) {
    }

    @Override
    public void cleanup() {
        releaseCamera();
        Global.getCombatEngine().getTimeMult().unmodify(timeMultKey);
        if (source.getCustomData().get(ACTIVE_PLUGIN_KEY) == this) {
            source.removeCustomData(ACTIVE_PLUGIN_KEY);
        }
        ShaderUtil.deleteProgram(shaderProgram);
        ShaderUtil.deleteVAO(vao);
        shaderProgram = 0;
        vao = null;
        if (screenTextureId != 0) GL11.glDeleteTextures(screenTextureId);
        screenTextureId = 0;
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public void advance(float amount) {
        float timelineAmount = amount * timelineMult;
        if (!activationVisualTriggered) {
            triggerActivationVisual();
            activationVisualTriggered = true;
        }
        visualArcTimer += timelineAmount;
        while (visualArcTimer >= VISUAL_ARC_INTERVAL) {
            visualArcTimer -= VISUAL_ARC_INTERVAL;
            if (queuedVisualArcs > 0) {
                queuedVisualArcs--;
                spawnSingleVisualArc(queuedArcThickness);
            } else {
                spawnSingleVisualArc(12f);
            }
        }
        if (cameraRecovering) {
            advanceCameraRecovery(timelineAmount);
        } else {
            lockCamera();
        }
        elapsed += timelineAmount;
        if (resolveState == ResolveState.INPUT && elapsed >= RESOLVE_START) {
            buildDamagePoints();
            damagePulse = 0;
            damagePulseTimer = 0f;
            applyDamagePulse();
            damagePulse++;
            resolveState = ResolveState.DAMAGE;
        }
        if (resolveState == ResolveState.DAMAGE) {
            damagePulseTimer += timelineAmount;
            while (damagePulse < DAMAGE_PULSES && damagePulseTimer >= DAMAGE_PULSE_INTERVAL) {
                damagePulseTimer -= DAMAGE_PULSE_INTERVAL;
                applyDamagePulse();
                damagePulse++;
            }
            if (damagePulse >= DAMAGE_PULSES) {
                resolveState = ResolveState.FLASH;
                resolveFlash = RESOLVE_FLASH_DURATION;
            }
        }
        if (resolveState == ResolveState.FLASH) {
            resolveFlash = Math.max(0f, resolveFlash - timelineAmount);
            if (resolveFlash <= 0f) {
                teleportEntities();
                resolveState = ResolveState.TELEPORTED;
                spatialRenderingComplete = true;
            }
        }
        if (resolveState != ResolveState.INPUT) {
            hoverSector = -1;
            selectedSector = -1;
        }
        if (resolveState == ResolveState.INPUT) {
            hoverSector = getHoverSector();
            updateSelection();
        }
        swapFlash = Math.max(0f, swapFlash - timelineAmount);
        if (elapsed >= TOTAL_CUT_TIME + HOLD_TIME + FADE_TIME && !cameraRecovering) {
            beginCameraRecovery();
        }
        if (elapsed >= TOTAL_CUT_TIME + HOLD_TIME + FADE_TIME && !cameraLocked && !cameraRecovering) {
            expired = true;
        }
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.JUST_BELOW_WIDGETS);
    }

    @Override
    public float getRenderRadius() {
        return SPACE_RADIUS * RENDER_RADIUS_MULT;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer != CombatEngineLayers.JUST_BELOW_WIDGETS || expired || spatialRenderingComplete) {
            return;
        }

        initGL();
        if (shaderProgram <= 0 || vao == null) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        ensureScreenTexture();
        captureScreen();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL20.glUseProgram(shaderProgram);

        FloatBuffer modelMatrix = ShaderUtil.buildModelMatrix(center.x, center.y, 0f);
        GL20.glUniformMatrix4(modelMatrixLoc, false, modelMatrix);
        GL20.glUniform2f(sizeLoc, SPACE_RADIUS * 2f * RENDER_RADIUS_MULT,
                SPACE_RADIUS * 2f * RENDER_RADIUS_MULT);
        GL20.glUniform1f(timeLoc, elapsed);
        GL20.glUniform1f(alphaLoc, getGlobalAlpha());
        GL20.glUniform3f(auraColorLoc, AURA_COLOR.getRed() / 255f, AURA_COLOR.getGreen() / 255f, AURA_COLOR.getBlue() / 255f);
        GL20.glUniform3f(coreColorLoc, CORE_COLOR.getRed() / 255f, CORE_COLOR.getGreen() / 255f, CORE_COLOR.getBlue() / 255f);
        GL20.glUniform1i(hoverSectorLoc, hoverSector);
        GL20.glUniform1i(selectedSectorLoc, selectedSector);
        GL20.glUniform1i(swapFirstSectorLoc, swapFirstSector);
        GL20.glUniform1i(swapSecondSectorLoc, swapSecondSector);
        GL20.glUniform1f(swapFlashLoc, swapFlash / SWAP_FLASH_DURATION);
        GL20.glUniform1f(resolveFlashLoc, resolveFlash / RESOLVE_FLASH_DURATION);
        sectorMapBuffer.clear();
        sectorMapBuffer.put(sectorMap);
        sectorMapBuffer.flip();
        GL20.glUniform1(sectorMapLoc, sectorMapBuffer);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenTextureId);
        GL20.glUniform1i(screenTextureLoc, 0);
        ShaderUtil.drawVAOQuad(vao.vaoId);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL20.glUseProgram(0);
        GL11.glPopAttrib();
    }

    private float getGlobalAlpha() {
        float fadeStart = TOTAL_CUT_TIME + HOLD_TIME;
        if (elapsed <= fadeStart) {
            return 1f;
        }
        return 1f - clamp01((elapsed - fadeStart) / FADE_TIME);
    }

    private void initGL() {
        if (glInitialized) {
            return;
        }
        glInitialized = true;
        shaderProgram = ShaderUtil.createShaderProgramFromFiles(
                "data/shaders/meng/minggod_space_cut.vert",
                "data/shaders/meng/minggod_space_cut.frag",
                "MingGodSpaceCut");
        if (shaderProgram <= 0) {
            return;
        }
        int[] locations = ShaderUtil.getUniformLocations(shaderProgram,
                "modelMatrix", "size", "u_time", "u_alpha", "u_auraColor", "u_coreColor", "u_screenTexture",
                "u_sectorMap[0]", "u_hoverSector", "u_selectedSector", "u_swapFirstSector", "u_swapSecondSector", "u_swapFlash", "u_resolveFlash");
        modelMatrixLoc = locations[0];
        sizeLoc = locations[1];
        timeLoc = locations[2];
        alphaLoc = locations[3];
        auraColorLoc = locations[4];
        coreColorLoc = locations[5];
        screenTextureLoc = locations[6];
        sectorMapLoc = locations[7];
        hoverSectorLoc = locations[8];
        selectedSectorLoc = locations[9];
        swapFirstSectorLoc = locations[10];
        swapSecondSectorLoc = locations[11];
        swapFlashLoc = locations[12];
        resolveFlashLoc = locations[13];
        vao = ShaderUtil.createUniversalRectVAO();
        viewportBuffer = BufferUtils.createIntBuffer(16);
        sectorMapBuffer = BufferUtils.createIntBuffer(SECTOR_COUNT);
    }

    private int getHoverSector() {
        Vector2f mouseTarget = source.getMouseTarget();
        float offsetX = mouseTarget.x - center.x;
        float offsetY = mouseTarget.y - center.y;
        if (offsetX * offsetX + offsetY * offsetY > SPACE_RADIUS * SPACE_RADIUS) return -1;

        float angle = (float) Math.atan2(offsetY, offsetX);
        if (angle < 0f) {
            angle += (float) (Math.PI * 2d);
        }
        return Math.min(SECTOR_COUNT - 1, (int) (angle / SECTOR_ANGLE));
    }

    private void updateSelection() {
        boolean kIsDown = Keyboard.isKeyDown(Keyboard.KEY_K);
        if (kIsDown && !kWasDown && hoverSector >= 0 && swapCount < MAX_SWAPS) {
            if (selectedSector < 0) {
                selectedSector = hoverSector;
            } else if (selectedSector == hoverSector) {
                selectedSector = -1;
            } else {
                performSwap(selectedSector, hoverSector);
                selectedSector = -1;
            }
        }
        kWasDown = kIsDown;
    }

    public boolean requestSwap(int firstSector, int secondSector) {
        if (resolveState != ResolveState.INPUT || swapCount >= MAX_SWAPS) return false;
        if (firstSector < 0 || firstSector >= SECTOR_COUNT) return false;
        if (secondSector < 0 || secondSector >= SECTOR_COUNT || firstSector == secondSector) return false;
        performSwap(firstSector, secondSector);
        return true;
    }

    public int[] getSectorMapSnapshot() {
        return sectorMap.clone();
    }

    private void performSwap(int firstSector, int secondSector) {
        int previousSource = sectorMap[firstSector];
        sectorMap[firstSector] = sectorMap[secondSector];
        sectorMap[secondSector] = previousSource;
        swapFirstSector = firstSector;
        swapSecondSector = secondSector;
        swapFlash = SWAP_FLASH_DURATION;
        swapCount++;
        triggerSwapVisual();
    }

    private void triggerActivationVisual() {
        source.setJitter(this, AURA_COLOR, 12f, 4, 15f);
        spawnVisualArcs(3, 16f);
    }

    private void triggerSwapVisual() {
        source.setJitter(this, CORE_COLOR, 18f, 6, 25f);
        spawnVisualArcs(3, 20f);
    }

    private void spawnVisualArcs(int count, float thickness) {
        if (count <= 0) return;
        spawnSingleVisualArc(thickness);
        queuedVisualArcs += count - 1;
        queuedArcThickness = thickness;
    }

    private void spawnSingleVisualArc(float thickness) {
        EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
        params.segmentLengthMult = 15f;
        params.zigZagReductionFactor = 0.25f;
        params.fadeOutDist = 30f;
        params.minFadeOutMult = 10f;
        params.flickerRateMult = 0.3f;
        float distance = Vector2f.sub(center, source.getLocation(), new Vector2f()).length();
        params.movementDurOverride = Math.max(0.025f, distance / VISUAL_ARC_SPEED);
        params.brightSpotFullFraction = 0.5f;
        EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(source.getLocation(), source, center, null,
                thickness, ARC_FRINGE_COLOR, ARC_CORE_COLOR, params);
        arc.setCoreWidthOverride(thickness * 0.5f);
        arc.setRenderGlowAtStart(false);
        arc.setFadedOutAtStart(true);
        arc.setSingleFlickerMode(true);
        arc.setRenderGlowAtEnd(false);
    }

    private void lockCamera() {
        if (!cameraLocked && !isPlayerAreaOverlapping()) return;

        ViewportAPI viewport = Global.getCombatEngine().getViewport();
        if (!cameraLocked) {
            if (viewport.isExternalControl()) return;

            lockedViewMult = viewport.getViewMult();
            float aspectRatio = viewport.getVisibleWidth() / viewport.getVisibleHeight();
            float requiredSpan = SPACE_RADIUS * 2f * 1.1f;
            expandedVisibleHeight = aspectRatio >= 1f ? requiredSpan : requiredSpan / aspectRatio;
            expandedVisibleWidth = expandedVisibleHeight * aspectRatio;
            viewport.setExternalControl(true);
            cameraLocked = true;
        }
        viewport.setCenter(center);
        viewport.set(center.x - expandedVisibleWidth * 0.5f, center.y - expandedVisibleHeight * 0.5f,
                expandedVisibleWidth, expandedVisibleHeight);
    }

    private boolean isPlayerAreaOverlapping() {
        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();
        float overlapRadius = SPACE_RADIUS * 2f;
        return Vector2f.sub(playerShip.getLocation(), center, new Vector2f()).lengthSquared() <= overlapRadius * overlapRadius;
    }

    private void releaseCamera() {
        if (!cameraLocked) return;

        Global.getCombatEngine().getViewport().setExternalControl(false);
        cameraLocked = false;
    }

    private void beginCameraRecovery() {
        if (!cameraLocked) {
            expired = true;
            return;
        }
        cameraRecovering = true;
        cameraRecoveryElapsed = 0f;
    }

    private void advanceCameraRecovery(float amount) {
        ViewportAPI viewport = Global.getCombatEngine().getViewport();
        cameraRecoveryElapsed = Math.min(CAMERA_RECOVERY_DURATION, cameraRecoveryElapsed + amount);
        float progress = cameraRecoveryElapsed / CAMERA_RECOVERY_DURATION;
        progress = progress * progress * (3f - 2f * progress);
        Vector2f target = source.getLocation();
        viewport.setCenter(new Vector2f(center.x + (target.x - center.x) * progress,
                center.y + (target.y - center.y) * progress));
        viewport.setViewMult(lockedViewMult);
        if (cameraRecoveryElapsed >= CAMERA_RECOVERY_DURATION) {
            releaseCamera();
            cameraRecovering = false;
            expired = true;
        }
    }

    private void buildDamagePoints() {
        damagePoints.clear();
        for (int boundary = 0; boundary < SECTOR_COUNT; boundary++) {
            int clockwiseSector = boundary;
            int counterClockwiseSector = (boundary + SECTOR_COUNT - 1) % SECTOR_COUNT;
            if (areClockwiseConnected(counterClockwiseSector, clockwiseSector)) continue;
            float angle = boundary * SECTOR_ANGLE;
            float cosine = (float) Math.cos(angle);
            float sine = (float) Math.sin(angle);
            int steps = (int) Math.ceil(SPACE_RADIUS / DAMAGE_POINT_SPACING);
            for (int i = 0; i <= steps; i++) {
                float distance = Math.min(SPACE_RADIUS, i * DAMAGE_POINT_SPACING);
                damagePoints.add(new Vector2f(center.x + cosine * distance, center.y + sine * distance));
            }
        }
        int arcSteps = (int) Math.ceil((float) (Math.PI * 2d * SPACE_RADIUS) / DAMAGE_POINT_SPACING);
        for (int i = 0; i < arcSteps; i++) {
            float angle = (float) (Math.PI * 2d * i / arcSteps);
            damagePoints.add(new Vector2f(center.x + (float) Math.cos(angle) * SPACE_RADIUS,
                    center.y + (float) Math.sin(angle) * SPACE_RADIUS));
        }
    }

    private boolean areClockwiseConnected(int counterClockwiseSector, int clockwiseSector) {
        int sourceBefore = sectorMap[counterClockwiseSector];
        int sourceAfter = sectorMap[clockwiseSector];
        int clockwiseDifference = (sourceAfter - sourceBefore + SECTOR_COUNT) % SECTOR_COUNT;
        return clockwiseDifference == 1;
    }

    private void applyDamagePulse() {
        if (damagePoints.isEmpty()) return;
        for (Vector2f point : damagePoints) {
            for (ShipAPI ship : Global.getCombatEngine().getShips()) {
                if (ship == source || ship.getOwner() == source.getOwner()) continue;
                if (!ship.isAlive() || MathUtils.getDistance(point,ship.getLocation())>=ship.getCollisionRadius()) continue;
                Global.getCombatEngine().applyDamage(ship, point, BORDER_DAMAGE,
                        DamageType.ENERGY, 0f, false, true, source, true);
            }
        }
    }

    private void teleportEntities() {
        teleportSignFields();
        CombatEntityAPI[] entities = collectEntities();
        for (CombatEntityAPI entity : entities) {
            if (entity == null || entity.wasRemoved()) continue;
            Vector2f offset = Vector2f.sub(entity.getLocation(), center, new Vector2f());
            if (offset.length() > SPACE_RADIUS) continue;
            int currentSector = getSector(offset);
            int destinationSector = currentSector;
            for (int i = 0; i < SECTOR_COUNT; i++) {
                if (sectorMap[i] == currentSector) {
                    destinationSector = i;
                    break;
                }
            }
            float rotation = (destinationSector - currentSector) * SECTOR_ANGLE;
            float sine = (float) Math.sin(rotation);
            float cosine = (float) Math.cos(rotation);
            entity.getLocation().set(center.x + offset.x * cosine - offset.y * sine,
                    center.y + offset.x * sine + offset.y * cosine);
            entity.getVelocity().set(entity.getVelocity().x * cosine - entity.getVelocity().y * sine,
                    entity.getVelocity().x * sine + entity.getVelocity().y * cosine);
            entity.setFacing(entity.getFacing() + (float) Math.toDegrees(rotation));
        }
    }

    private void teleportSignFields() {
        for (Meng_MingGodCenter.Meng_MingGodSignPlugin sign : Meng_MingGodCenter.getActiveSignFields(Global.getCombatEngine())) {
            Vector2f offset = Vector2f.sub(sign.getFieldLocation(), center, new Vector2f());
            if (offset.length() > SPACE_RADIUS) continue;
            int currentSector = getSector(offset);
            int destinationSector = getDestinationSector(currentSector);
            float rotation = (destinationSector - currentSector) * SECTOR_ANGLE;
            sign.applySpatialTransform(center, (float) Math.cos(rotation), (float) Math.sin(rotation));
        }
    }

    private int getDestinationSector(int currentSector) {
        for (int i = 0; i < SECTOR_COUNT; i++) {
            if (sectorMap[i] == currentSector) return i;
        }
        return currentSector;
    }

    private CombatEntityAPI[] collectEntities() {
        java.util.ArrayList<CombatEntityAPI> entities = new java.util.ArrayList<CombatEntityAPI>();
        entities.addAll(Global.getCombatEngine().getShips());
        entities.addAll(Global.getCombatEngine().getMissiles());
        entities.addAll(Global.getCombatEngine().getProjectiles());
        entities.addAll(Global.getCombatEngine().getAsteroids());
        return entities.toArray(new CombatEntityAPI[entities.size()]);
    }

    private int getSector(Vector2f offset) {
        float angle = (float) Math.atan2(offset.y, offset.x);
        if (angle < 0f) angle += (float) (Math.PI * 2d);
        return Math.min(SECTOR_COUNT - 1, (int) (angle / SECTOR_ANGLE));
    }

    private void ensureScreenTexture() {
        viewportBuffer.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer);
        int width = viewportBuffer.get(2);
        int height = viewportBuffer.get(3);
        if (screenTextureId != 0 && screenTextureWidth == width && screenTextureHeight == height) return;

        if (screenTextureId != 0) GL11.glDeleteTextures(screenTextureId);
        screenTextureWidth = width;
        screenTextureHeight = height;
        screenTextureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenTextureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private void captureScreen() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenTextureId);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, screenTextureWidth, screenTextureHeight);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
