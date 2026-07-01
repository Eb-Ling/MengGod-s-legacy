package data.example;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

/**
 * “燃料勺”专长的战役层推进脚本。
 *
 * 规则：
 * 1. 玩家学习燃料勺后，在当前星系内寻找最近的非黑洞恒星；
 * 2. 从恒星表面计算距离，800 单位处为最低抽取速度，0 单位处为最高抽取速度，中间线性增长；
 * 3. Lv1-Lv5 的最低/最高速度已经硬编码在 PCSpecializationConstants；
 * 4. 只有燃料容量未满且实际补充燃料时，才生成从恒星指向舰队的气态虹吸粒子。
 */
public class PCFuelScoopScript implements EveryFrameScript {

    protected static final float PARTICLE_INTERVAL = 0.045f;
    protected static final float PARTICLE_STREAM_WIDTH = 96f;
    protected static final float PARTICLE_CORE_WIDTH = 13f;
    protected static final float PARTICLE_BASE_DURATION = 1.15f;
    protected static final Color PARTICLE_CORE = new Color(255, 230, 160, 205);
    protected static final Color PARTICLE_FRINGE = new Color(255, 122, 70, 125);
    protected static final Color PARTICLE_SMOKE = new Color(166, 78, 58, 62);
    protected static final Color PARTICLE_INTAKE = new Color(255, 242, 190, 230);

    protected final Random random = new Random();
    protected float particleTimer = 0f;

    @Override
    public boolean isDone() {
        return Global.getSector() == null;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.isInNewGameAdvance()) {
            deactivateSiphonVisual();
            return;
        }

        PCFuelScoopVisualRenderer.advance(amount);

        CampaignFleetAPI fleet = sector.getPlayerFleet();
        if (fleet == null || fleet.getCargo() == null) {
            return;
        }

        float days = sector.getClock().convertToDays(amount);
        if (days <= 0f) {
            return;
        }

        int level = PCSpecializationData.get().getLevel(PCSpecializationConstants.FUEL_SCOOP);
        if (level <= 0) {
            deactivateSiphonVisual();
            return;
        }

        CargoAPI cargo = fleet.getCargo();
        float fuelSpace = Math.max(0f, cargo.getMaxFuel() - cargo.getFuel());
        if (fuelSpace <= 0.01f) {
            deactivateSiphonVisual();
            return;
        }

        PlanetAPI star = findNearestValidStar(fleet);
        if (star == null) {
            deactivateSiphonVisual();
            return;
        }

        float surfaceDistance = getSurfaceDistance(fleet, star);
        if (surfaceDistance > PCSpecializationConstants.FUEL_SCOOP_MAX_SURFACE_DISTANCE) {
            deactivateSiphonVisual();
            return;
        }

        float fuelPerDay = getFuelPerDay(level, surfaceDistance);
        if (fuelPerDay <= 0f) {
            deactivateSiphonVisual();
            return;
        }

        float addedFuel = Math.min(fuelSpace, fuelPerDay * days);
        if (addedFuel <= 0f) {
            deactivateSiphonVisual();
            return;
        }

        cargo.addFuel(addedFuel);
        activateSiphonVisual(fleet, star, fuelPerDay, surfaceDistance);
        spawnSiphonParticles(fleet, star, fuelPerDay, amount);
    }

    protected PlanetAPI findNearestValidStar(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.isInHyperspace() || fleet.getContainingLocation() == null) {
            return null;
        }

        LocationAPI location = fleet.getContainingLocation();
        PlanetAPI result = null;
        float minSurfaceDistance = Float.MAX_VALUE;

        for (PlanetAPI planet : location.getPlanets()) {
            if (!isValidFuelSource(planet)) {
                continue;
            }

            float surfaceDistance = getSurfaceDistance(fleet, planet);
            if (surfaceDistance < minSurfaceDistance) {
                minSurfaceDistance = surfaceDistance;
                result = planet;
            }
        }

        return result;
    }

    protected boolean isValidFuelSource(PlanetAPI planet) {
        return planet != null
                && planet.isStar()
                && !planet.isBlackHole()
                && planet.getContainingLocation() != null;
    }

    protected float getSurfaceDistance(CampaignFleetAPI fleet, PlanetAPI star) {
        if (fleet == null || star == null) {
            return Float.MAX_VALUE;
        }

        float centerDistance = Misc.getDistance(fleet.getLocation(), star.getLocation());
        return Math.max(0f, centerDistance - Math.max(0f, star.getRadius()));
    }

    protected float getFuelPerDay(int level, float surfaceDistance) {
        int safeLevel = Math.max(1, Math.min(5, level));
        float min = PCSpecializationConstants.getFuelScoopMinFuelPerDay(safeLevel);
        float max = PCSpecializationConstants.getFuelScoopMaxFuelPerDay(safeLevel);

        float minDistance = PCSpecializationConstants.FUEL_SCOOP_MIN_SURFACE_DISTANCE;
        float maxDistance = PCSpecializationConstants.FUEL_SCOOP_MAX_SURFACE_DISTANCE;
        float progress = (maxDistance - surfaceDistance) / Math.max(1f, maxDistance - minDistance);
        progress = Math.max(0f, Math.min(1f, progress));

        return min + (max - min) * progress;
    }

    protected void activateSiphonVisual(CampaignFleetAPI fleet, PlanetAPI star, float fuelPerDay, float surfaceDistance) {
        if (fleet == null || star == null || fleet.getContainingLocation() == null) {
            return;
        }

        LocationAPI location = fleet.getContainingLocation();
        PCFuelScoopVisualRenderer.activate(location, fleet, star, fuelPerDay, surfaceDistance);
    }

    protected void deactivateSiphonVisual() {
        particleTimer = 0f;
        PCFuelScoopVisualRenderer.deactivate();
    }

    protected void spawnSiphonParticles(CampaignFleetAPI fleet, PlanetAPI star, float fuelPerDay, float amount) {
        if (fleet == null || star == null || fleet.getContainingLocation() == null) {
            return;
        }

        LocationAPI location = fleet.getContainingLocation();
        if (!location.isCurrentLocation()) {
            return;
        }

        particleTimer += amount;
        int burstCount = 0;
        while (particleTimer >= PARTICLE_INTERVAL && burstCount < 6) {
            particleTimer -= PARTICLE_INTERVAL;
            burstCount++;
            addParticlePair(location, fleet, star, fuelPerDay);
        }
    }

    protected void addParticlePair(LocationAPI location, CampaignFleetAPI fleet, PlanetAPI star, float fuelPerDay) {
        Vector2f from = getSurfacePoint(star, fleet.getLocation());
        Vector2f to = new Vector2f(fleet.getLocation());
        Vector2f line = Vector2f.sub(to, from, new Vector2f());
        float length = line.length();
        if (length <= 1f) {
            return;
        }

        line.normalise();
        Vector2f normal = new Vector2f(-line.y, line.x);
        float intensity = Math.max(0.45f, Math.min(1.35f, fuelPerDay / 120f));

        /*
         * 第一层：宽而淡的气体羽流。
         *
         * 参考图的主体不是离散火花，而是从恒星表面喷出的锥形雾状气流。
         * 因此横向宽度必须在恒星侧最大、靠近舰队时收窄；粒子尺寸也应更大、
         * 透明度更低、寿命更长，形成连续云带而不是单个亮点。
         */
        for (int i = 0; i < 7; i++) {
            float along = random.nextFloat();
            float coneWidth = PARTICLE_STREAM_WIDTH * (1f - along) + 5f;

            Vector2f loc = getPointAlongStream(from, line, normal, length, along, coneWidth);
            Vector2f vel = new Vector2f(line);
            vel.scale(14f + random.nextFloat() * 34f + fuelPerDay * 0.12f);

            float size = (18f + random.nextFloat() * 34f) * intensity * (1.15f - along * 0.45f);
            float duration = PARTICLE_BASE_DURATION + random.nextFloat() * 0.95f;
            Color color = random.nextFloat() < 0.55f ? PARTICLE_SMOKE : PARTICLE_FRINGE;
            location.addHitParticle(loc, vel, size, 0.32f, duration, color);
        }

        /*
         * 第二层：细亮核心流。
         *
         * 这层保持在中心线附近，负责表现“虹吸管”的方向性和连续亮线；
         * 宽度远小于气体羽流，越靠近舰队越亮但不再变宽。
         */
        for (int i = 0; i < 4; i++) {
            float along = random.nextFloat();
            float coreWidth = PARTICLE_CORE_WIDTH * (0.65f + 0.35f * (1f - along));

            Vector2f loc = getPointAlongStream(from, line, normal, length, along, coreWidth);
            Vector2f vel = new Vector2f(line);
            vel.scale(58f + random.nextFloat() * 88f + fuelPerDay * 0.4f);

            float size = (5f + random.nextFloat() * 10f) * intensity * (0.75f + along * 0.45f);
            float duration = 0.55f + random.nextFloat() * 0.55f;
            Color color = random.nextFloat() < 0.7f ? PARTICLE_CORE : PARTICLE_FRINGE;
            location.addHitParticle(loc, vel, size, 0.82f, duration, color);
        }

        /*
         * 第三层：舰队入口吸积闪光。
         *
         * 少量小粒子集中在舰队附近，使玩家能看出气流正在被舰队吸入，
         * 但粒子半径必须小，避免重新变成舰队周围的大团爆闪。
         */
        if (random.nextFloat() < 0.75f) {
            float along = 0.9f + random.nextFloat() * 0.1f;
            Vector2f loc = getPointAlongStream(from, line, normal, length, along, 11f);
            Vector2f vel = new Vector2f(line);
            vel.scale(75f + random.nextFloat() * 80f);

            float size = (4f + random.nextFloat() * 7f) * intensity;
            location.addHitParticle(loc, vel, size, 1f, 0.38f + random.nextFloat() * 0.22f, PARTICLE_INTAKE);
        }
    }

    protected Vector2f getPointAlongStream(Vector2f from, Vector2f line, Vector2f normal, float length, float along, float width) {
        Vector2f loc = new Vector2f(from);

        Vector2f alongOffset = new Vector2f(line);
        alongOffset.scale(length * along);
        Vector2f.add(loc, alongOffset, loc);

        Vector2f sideOffset = new Vector2f(normal);
        sideOffset.scale((random.nextFloat() - 0.5f) * width);
        Vector2f.add(loc, sideOffset, loc);

        return loc;
    }

    protected Vector2f getSurfacePoint(PlanetAPI star, Vector2f target) {
        Vector2f result = new Vector2f(star.getLocation());
        Vector2f dir = Vector2f.sub(target, star.getLocation(), new Vector2f());
        if (dir.length() <= 1f) {
            return result;
        }

        dir.normalise();
        dir.scale(Math.max(0f, star.getRadius()));
        Vector2f.add(result, dir, result);
        return result;
    }
}