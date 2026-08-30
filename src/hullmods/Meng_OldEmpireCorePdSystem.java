package data.hullmods;

import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

/**
 * Final-stage black-hole-core point defense. Each channel opens from an arc
 * of the outer accretion horizon and converges only at the target.
 */
public final class Meng_OldEmpireCorePdSystem {
    public static final float ACTIVATION_VALUE = 80f;
    public static final float RANGE = 760f;
    public static final float DAMAGE_MULTIPLIER = 2.5f;
    public static final float TARGET_SPEED_MULT = 0.70f;

    private static final String CONTROLLER_KEY = "Meng_OldEmpire_core_pd_controller";
    private static final float TARGET_SCAN_INTERVAL = 0.18f;
    private static final float MATERIALIZE_TIME = 0.44f;
    private static final float DAMAGE_INTERVAL = 0.10f;
    private static final float MISSILE_DEFLECTION_RATE = 24f;
    private static final float FIGHTER_DEFLECTION_RATE = 9f;

    private Meng_OldEmpireCorePdSystem() {
    }

    public static void attach(ShipAPI ship, CombatEngineAPI engine) {
        if (ship == null || engine == null
                || !engine.isEntityInPlay(ship)
                || ship.getCustomData().get(CONTROLLER_KEY) instanceof Controller) {
            return;
        }
        Controller controller = new Controller(ship, engine);
        ship.setCustomData(CONTROLLER_KEY, controller);
        engine.addLayeredRenderingPlugin(controller);
    }

    private static final class Controller
            extends BaseCombatLayeredRenderingPlugin {
        private final ShipAPI ship;
        private final CombatEngineAPI engine;
        private final List<Channel> channels = new ArrayList<>();
        private float elapsed;
        private float scanTimer;

        private Controller(ShipAPI ship, CombatEngineAPI engine) {
            this.ship = ship;
            this.engine = engine;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return RANGE + 240f;
        }

        @Override
        public boolean isExpired() {
            return ship == null || engine == null
                    || !engine.isEntityInPlay(ship);
        }

        @Override
        public void advance(float amount) {
            if (engine.isPaused() || ship == null) {
                return;
            }
            elapsed += amount;

            if (!isActive()) {
                channels.clear();
                scanTimer = 0f;
                return;
            }

            Iterator<Channel> iterator = channels.iterator();
            while (iterator.hasNext()) {
                Channel channel = iterator.next();
                if (!isValidTarget(channel.target)
                        || distanceSquared(ship.getLocation(),
                                channel.target.getLocation()) > RANGE * RANGE) {
                    iterator.remove();
                    continue;
                }

                channel.age += amount;
                if (channel.age < MATERIALIZE_TIME) {
                    continue;
                }
                applyGraviticDisruption(channel, amount);
                channel.damageTimer -= amount;
                while (channel.damageTimer <= 0f
                        && isValidTarget(channel.target)) {
                    applyDamage(channel);
                    channel.damageTimer += DAMAGE_INTERVAL;
                }
            }

            int maximum = maxJets(ship);
            while (channels.size() > maximum) {
                channels.remove(channels.size() - 1);
            }

            scanTimer -= amount;
            if (scanTimer <= 0f && channels.size() < maximum) {
                fillChannels(maximum);
                scanTimer = TARGET_SCAN_INTERVAL;
            }
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (channels.isEmpty() || ship == null || !ship.isAlive()) {
                return;
            }

            float coreRadius = Math.max(8f, Meng_OldEmpireBlackHoleCoreHullmod
                    .getCoreRadiusForEffects(ship));
            Vector2f center = Meng_OldEmpireBlackHoleCoreHullmod
                    .getCoreCenterForEffects(ship, elapsed);
            if (!viewport.isNearViewport(center, getRenderRadius())) {
                return;
            }

            Color color = Meng_OldEmpireBlackHoleCoreHullmod.getCoreColor(ship);
            Color hot = blend(color, Color.WHITE, 0.58f);
            float viewportAlpha = viewport.getAlphaMult();

            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            for (Channel channel : channels) {
                if (!isValidTarget(channel.target)) {
                    continue;
                }
                renderChannel(channel, center, coreRadius,
                        color, hot, viewportAlpha);
            }

            GL11.glPopAttrib();
        }

        private boolean isActive() {
            return ship.isAlive() && !ship.isHulk() && !ship.isPhased()
                    && !ship.getFluxTracker().isOverloadedOrVenting()
                    && Meng_OldEmpireBlackHoleCoreHullmod.getCoreValue(ship)
                    > ACTIVATION_VALUE;
        }

        private void fillChannels(int maximum) {
            List<Candidate> candidates = collectCandidates();
            for (Candidate candidate : candidates) {
                if (channels.size() >= maximum) {
                    break;
                }
                if (!isTracked(candidate.target)) {
                    channels.add(new Channel(candidate.target,
                            (float) Math.random()));
                }
            }
        }

        private boolean isTracked(CombatEntityAPI target) {
            for (Channel channel : channels) {
                if (channel.target == target) {
                    return true;
                }
            }
            return false;
        }

        private List<Candidate> collectCandidates() {
            List<Candidate> candidates = new ArrayList<>();
            float rangeSquared = RANGE * RANGE;

            for (MissileAPI missile : engine.getMissiles()) {
                float distance = missile == null ? Float.MAX_VALUE
                        : distanceSquared(ship.getLocation(),
                                missile.getLocation());
                if (isValidMissile(missile) && distance <= rangeSquared) {
                    candidates.add(new Candidate(missile, 0, distance));
                }
            }

            for (ShipAPI fighter : engine.getShips()) {
                float distance = fighter == null ? Float.MAX_VALUE
                        : distanceSquared(ship.getLocation(),
                                fighter.getLocation());
                if (isValidFighter(fighter) && distance <= rangeSquared) {
                    candidates.add(new Candidate(fighter, 1, distance));
                }
            }

            candidates.sort(Comparator
                    .comparingInt((Candidate candidate) -> candidate.priority)
                    .thenComparingDouble(candidate -> candidate.distanceSquared));
            return candidates;
        }

        private boolean isValidMissile(MissileAPI missile) {
            return missile != null
                    && missile.getOwner() != ship.getOwner()
                    && missile.getOwner() != 100
                    && engine.isEntityInPlay(missile)
                    && missile.getHitpoints() > 0f
                    && !missile.isFading()
                    && !missile.isFizzling();
        }

        private boolean isValidFighter(ShipAPI fighter) {
            return fighter != null && fighter != ship
                    && fighter.getOwner() != ship.getOwner()
                    && fighter.getOwner() != 100
                    && engine.isEntityInPlay(fighter)
                    && fighter.isAlive() && !fighter.isHulk()
                    && !fighter.isPhased()
                    && (fighter.isFighter() || fighter.isDrone());
        }

        private boolean isValidTarget(CombatEntityAPI target) {
            if (target instanceof MissileAPI) {
                return isValidMissile((MissileAPI) target);
            }
            if (target instanceof ShipAPI) {
                return isValidFighter((ShipAPI) target);
            }
            return false;
        }

        private void applyDamage(Channel channel) {
            Vector2f center = Meng_OldEmpireBlackHoleCoreHullmod
                    .getCoreCenterForEffects(ship, elapsed);
            Vector2f impact = surfacePoint(center, channel.target);
            float dps = Meng_OldEmpireBlackHoleCoreHullmod.getCoreValue(ship)
                    * DAMAGE_MULTIPLIER;
            engine.applyDamage(channel.target, impact,
                    dps * DAMAGE_INTERVAL,
                    DamageType.FRAGMENTATION, 0f,
                    false, false, ship);

            Vector2f velocity = channel.target.getVelocity() == null
                    ? new Vector2f()
                    : new Vector2f(channel.target.getVelocity());
            engine.addHitParticle(impact, velocity,
                    7f, 0.72f, 0.07f,
                    new Color(255, 54, 42, 185));
        }

        private void applyGraviticDisruption(
                Channel channel, float amount) {
            CombatEntityAPI target = channel.target;
            Vector2f velocity = target.getVelocity();
            if (velocity == null) {
                return;
            }

            boolean missile = target instanceof MissileAPI;
            float maximumSpeed;
            if (missile) {
                maximumSpeed = ((MissileAPI) target).getMaxSpeed();
            } else if (target instanceof ShipAPI) {
                maximumSpeed = ((ShipAPI) target).getMutableStats()
                        .getMaxSpeed().getModifiedValue();
            } else {
                return;
            }

            float speedSquared = velocity.x * velocity.x
                    + velocity.y * velocity.y;
            float speedLimit = Math.max(0f,
                    maximumSpeed * TARGET_SPEED_MULT);
            if (speedLimit > 0f
                    && speedSquared > speedLimit * speedLimit) {
                float scale = speedLimit
                        / (float) Math.sqrt(speedSquared);
                velocity.x *= scale;
                velocity.y *= scale;
            }

            float wave = ((float) Math.sin(
                    elapsed * 7.4f + channel.seed * 31f)
                    + 0.45f * (float) Math.sin(
                            elapsed * 12.7f
                                    + channel.seed * 17f + 1.8f))
                    / 1.45f;
            float deflectionRate = missile
                    ? MISSILE_DEFLECTION_RATE
                    : FIGHTER_DEFLECTION_RATE;
            rotateInPlace(velocity, wave * deflectionRate * amount);

            float desiredAngularVelocity = wave
                    * (missile ? 22f : 7f);
            float response = clamp(amount * (missile ? 7f : 4f),
                    0f, 1f);
            target.setAngularVelocity(lerp(
                    target.getAngularVelocity(),
                    desiredAngularVelocity, response));

            Color distortion = new Color(175, 24, 36, 115);
            if (missile) {
                ((MissileAPI) target).setJitter(
                        this, distortion, 0.18f, 3, 1.5f);
            } else {
                ((ShipAPI) target).setJitter(
                        this, distortion, 0.14f, 3, 1.2f);
            }
        }

        private void renderChannel(Channel channel, Vector2f center,
                float coreRadius, Color color, Color hot,
                float viewportAlpha) {
            Vector2f impact = surfacePoint(center, channel.target);
            float targetAngle = angle(center, impact);
            float targetDistance = (float) Math.sqrt(
                    distanceSquared(center, impact));
            float outerRadius = coreRadius * 1.82f;
            float halfBaseAngle = 72f;

            float dx = impact.x - center.x;
            float dy = impact.y - center.y;
            float inverseDistance = targetDistance > 0.001f
                    ? 1f / targetDistance : 0f;
            Vector2f direction = new Vector2f(
                    dx * inverseDistance, dy * inverseDistance);
            Vector2f perpendicular = new Vector2f(
                    -direction.y, direction.x);

            Vector2f leftStart = offset(center,
                    targetAngle + halfBaseAngle, outerRadius);
            Vector2f rightStart = offset(center,
                    targetAngle - halfBaseAngle, outerRadius);
            float radians = (float) Math.toRadians(halfBaseAngle);
            float baseForward = outerRadius * (float) Math.cos(radians);
            float baseLateral = outerRadius * (float) Math.sin(radians);
            float travel = Math.max(8f, targetDistance - baseForward);

            // The first tangent hugs the horizon and pulls sharply inward.
            // The second is almost axial, so both sides meet only at impact.
            float control1Forward = baseForward + travel * 0.05f;
            float control2Forward = baseForward + travel * 0.76f;
            Vector2f leftControl1 = basisPoint(center,
                    direction, perpendicular,
                    control1Forward, baseLateral * 0.34f);
            Vector2f rightControl1 = basisPoint(center,
                    direction, perpendicular,
                    control1Forward, -baseLateral * 0.34f);
            Vector2f leftControl2 = basisPoint(center,
                    direction, perpendicular,
                    control2Forward, baseLateral * 0.035f);
            Vector2f rightControl2 = basisPoint(center,
                    direction, perpendicular,
                    control2Forward, -baseLateral * 0.035f);

            float mistAlpha = smooth(channel.age / 0.18f) * viewportAlpha;
            float edgeAlpha = smooth((channel.age - 0.08f) / 0.30f)
                    * viewportAlpha;
            float impactAlpha = smooth((channel.age - 0.25f) / 0.19f)
                    * viewportAlpha;
            float breathe = 0.90f + 0.10f
                    * (float) Math.sin(elapsed * 10.5f
                            + channel.seed * 12f);

            drawHorizonBase(center, targetAngle, outerRadius,
                    halfBaseAngle, color, hot, mistAlpha);
            drawTriangleMist(center, targetAngle, outerRadius,
                    halfBaseAngle, impact, direction, perpendicular,
                    targetDistance, coreRadius,
                    color, hot, mistAlpha, edgeAlpha, impactAlpha,
                    elapsed, channel.seed);

            drawConvergingEdge(leftStart, leftControl1, leftControl2,
                    impact, color, hot, mistAlpha, impactAlpha);
            drawConvergingEdge(rightStart, rightControl1, rightControl2,
                    impact, color, hot, mistAlpha, impactAlpha);

            float flowAlpha = edgeAlpha * breathe;
            drawCubicFlow(leftStart, leftControl1, leftControl2, impact,
                    elapsed * 0.72f + channel.seed,
                    color, hot, flowAlpha);
            drawCubicFlow(rightStart, rightControl1, rightControl2, impact,
                    elapsed * 0.72f + channel.seed + 0.43f,
                    color, hot, flowAlpha);

            drawGlow(leftStart, coreRadius * 0.30f,
                    color, mistAlpha * 0.09f);
            drawGlow(rightStart, coreRadius * 0.30f,
                    color, mistAlpha * 0.09f);
            drawGlow(impact, 11f, hot, impactAlpha * 0.60f);
        }
    }

    private static void drawConvergingEdge(Vector2f start,
            Vector2f control1, Vector2f control2, Vector2f impact,
            Color color, Color hot, float mistAlpha, float impactAlpha) {
        drawCubicGradient(start, control1, control2, impact,
                12f, 3.4f, color,
                mistAlpha * 0.014f, impactAlpha * 0.17f);
        drawCubicGradient(start, control1, control2, impact,
                4.8f, 1.7f, color,
                mistAlpha * 0.025f, impactAlpha * 0.58f);
        drawCubicGradient(start, control1, control2, impact,
                1.45f, 0.66f, hot,
                mistAlpha * 0.004f, impactAlpha * 0.92f);
    }

    private static void drawCubicGradient(Vector2f start,
            Vector2f control1, Vector2f control2, Vector2f end,
            float startWidth, float endWidth,
            Color color, float startAlpha, float endAlpha) {
        final int segments = 32;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            float along = i / (float) segments;
            float shaped = smooth(along);
            Vector2f point = cubic(
                    start, control1, control2, end, along);
            Vector2f tangent = cubicTangent(
                    start, control1, control2, end, along);
            float length = (float) Math.sqrt(
                    tangent.x * tangent.x + tangent.y * tangent.y);
            if (length < 0.001f) {
                length = 1f;
            }
            float px = -tangent.y / length;
            float py = tangent.x / length;
            float width = lerp(startWidth, endWidth, shaped);
            float alpha = lerp(startAlpha, endAlpha, shaped);
            glColor(color, alpha);
            GL11.glVertex2f(point.x + px * width, point.y + py * width);
            GL11.glVertex2f(point.x - px * width, point.y - py * width);
        }
        GL11.glEnd();
    }

    private static final class Candidate {
        private final CombatEntityAPI target;
        private final int priority;
        private final float distanceSquared;

        private Candidate(CombatEntityAPI target, int priority,
                float distanceSquared) {
            this.target = target;
            this.priority = priority;
            this.distanceSquared = distanceSquared;
        }
    }

    private static final class Channel {
        private final CombatEntityAPI target;
        private final float seed;
        private float age;
        private float damageTimer;

        private Channel(CombatEntityAPI target, float seed) {
            this.target = target;
            this.seed = seed;
        }
    }

    private static int maxJets(ShipAPI ship) {
        if (ship == null || ship.getHullSize() == null) {
            return 2;
        }
        switch (ship.getHullSize()) {
            case CAPITAL_SHIP:
                return 5;
            case CRUISER:
                return 3;
            default:
                return 2;
        }
    }

    private static Vector2f surfacePoint(
            Vector2f source, CombatEntityAPI target) {
        Vector2f center = target.getLocation();
        float dx = source.x - center.x;
        float dy = source.y - center.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001f) {
            return new Vector2f(center);
        }
        float radius = Math.max(2f, target.getCollisionRadius() * 0.82f);
        return new Vector2f(center.x + dx / length * radius,
                center.y + dy / length * radius);
    }

    private static void drawHorizonBase(Vector2f center,
            float targetAngle, float radius, float halfAngle,
            Color color, Color hot, float alpha) {
        drawArcBand(center, targetAngle, radius, halfAngle,
                11f, color, alpha * 0.024f);
        drawArcBand(center, targetAngle, radius, halfAngle,
                3.2f, hot, alpha * 0.070f);
    }

    private static void drawArcBand(Vector2f center, float targetAngle,
            float radius, float halfAngle, float width,
            Color color, float alpha) {
        final int segments = 32;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            float along = i / (float) segments;
            float localAngle = lerp(-halfAngle, halfAngle, along);
            float opacity = 0.42f + 0.58f
                    * (float) Math.sin(Math.PI * along);
            Vector2f inner = offset(center, targetAngle + localAngle,
                    radius - width);
            Vector2f outer = offset(center, targetAngle + localAngle,
                    radius + width);
            glColor(color, alpha * opacity);
            GL11.glVertex2f(inner.x, inner.y);
            GL11.glVertex2f(outer.x, outer.y);
        }
        GL11.glEnd();
    }

    private static void drawTriangleMist(Vector2f center,
            float targetAngle, float outerRadius, float halfBaseAngle,
            Vector2f impact, Vector2f direction, Vector2f perpendicular,
            float targetDistance, float coreRadius,
            Color color, Color hot, float mistAlpha, float edgeAlpha,
            float impactAlpha, float elapsed, float seed) {
        final int streams = 9;
        for (int i = 0; i < streams; i++) {
            float across = lerp(-0.88f, 0.88f,
                    i / (float) (streams - 1));
            float noise = (float) Math.sin(
                    seed * 19.7f + i * 2.31f);
            float startAngle = targetAngle
                    + across * halfBaseAngle + noise * 2.2f;
            float startRadius = outerRadius
                    * (0.985f + noise * 0.018f);
            Vector2f start = offset(center, startAngle, startRadius);
            float localRadians = (float) Math.toRadians(
                    startAngle - targetAngle);
            float localForward = startRadius
                    * (float) Math.cos(localRadians);
            float localLateral = startRadius
                    * (float) Math.sin(localRadians);
            float localTravel = Math.max(
                    6f, targetDistance - localForward);
            Vector2f control1 = basisPoint(center,
                    direction, perpendicular,
                    localForward + localTravel
                            * (0.048f + noise * 0.004f),
                    localLateral * (0.35f + noise * 0.018f));
            Vector2f control2 = basisPoint(center,
                    direction, perpendicular,
                    localForward + localTravel
                            * (0.75f + noise * 0.012f),
                    localLateral * (0.038f + noise * 0.006f));

            float hazeWidth = Math.max(5f,
                    coreRadius * (0.18f + 0.025f * (noise + 1f)));
            drawCubicGradient(start, control1, control2, impact,
                    hazeWidth, 2.2f, color,
                    mistAlpha * 0.0035f,
                    impactAlpha * 0.025f);
            drawCubicGradient(start, control1, control2, impact,
                    hazeWidth * 0.46f, 1.05f, color,
                    mistAlpha * 0.006f,
                    impactAlpha * 0.047f);

            float phase = elapsed * (0.43f + i * 0.009f)
                    + seed * 1.73f + i * 0.137f;
            drawCubicMistFlow(start, control1, control2, impact,
                    phase, hazeWidth * 0.72f, 1.35f,
                    color, hot, edgeAlpha * (0.70f + noise * 0.08f));
        }
    }

    private static void drawCubicFlow(Vector2f start,
            Vector2f control1, Vector2f control2, Vector2f end,
            float phase, Color color, Color hot, float alpha) {
        final int packets = 3;
        for (int i = 0; i < packets; i++) {
            float packetStart = fract(phase + i / (float) packets);
            drawWrappedCubicPacket(start, control1, control2, end,
                    packetStart, 0.25f, 6.4f, 1.5f,
                    color, hot, alpha, false);
        }
    }

    private static void drawCubicMistFlow(Vector2f start,
            Vector2f control1, Vector2f control2, Vector2f end,
            float phase, float startWidth, float endWidth,
            Color color, Color hot, float alpha) {
        final int packets = 2;
        for (int i = 0; i < packets; i++) {
            float packetStart = fract(phase + i / (float) packets);
            drawWrappedCubicPacket(start, control1, control2, end,
                    packetStart, 0.31f, startWidth, endWidth,
                    color, hot, alpha, true);
        }
    }

    private static void drawWrappedCubicPacket(Vector2f start,
            Vector2f control1, Vector2f control2, Vector2f end,
            float packetStart, float packetLength,
            float startWidth, float endWidth,
            Color color, Color hot, float alpha, boolean mist) {
        float packetEnd = packetStart + packetLength;
        if (packetEnd <= 1f) {
            drawCubicPacket(start, control1, control2, end,
                    packetStart, packetEnd, startWidth, endWidth,
                    color, hot, alpha, mist);
        } else {
            drawCubicPacket(start, control1, control2, end,
                    packetStart, 1f, startWidth, endWidth,
                    color, hot, alpha, mist);
            drawCubicPacket(start, control1, control2, end,
                    0f, packetEnd - 1f, startWidth, endWidth,
                    color, hot, alpha, mist);
        }
    }

    private static void drawCubicPacket(Vector2f start,
            Vector2f control1, Vector2f control2, Vector2f end,
            float from, float to, float startWidth, float endWidth,
            Color color, Color hot, float alpha, boolean mist) {
        if (to - from <= 0.002f) {
            return;
        }
        if (mist) {
            drawCubicPacketLayer(start, control1, control2, end,
                    from, to, startWidth, endWidth,
                    color, alpha * 0.17f, alpha * 0.42f);
            drawCubicPacketLayer(start, control1, control2, end,
                    from, to, startWidth * 0.38f, endWidth * 0.54f,
                    hot, alpha * 0.015f, alpha * 0.20f);
        } else {
            drawCubicPacketLayer(start, control1, control2, end,
                    from, to, startWidth, endWidth,
                    color, alpha * 0.06f, alpha * 0.34f);
            drawCubicPacketLayer(start, control1, control2, end,
                    from, to, startWidth * 0.34f, endWidth * 0.46f,
                    hot, alpha * 0.008f, alpha * 0.92f);
        }
    }

    private static void drawCubicPacketLayer(Vector2f start,
            Vector2f control1, Vector2f control2, Vector2f end,
            float from, float to, float startWidth, float endWidth,
            Color color, float sourceAlpha, float targetAlpha) {
        final int segments = 18;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            float along = i / (float) segments;
            float t = lerp(from, to, along);
            Vector2f point = cubic(
                    start, control1, control2, end, t);
            Vector2f tangent = cubicTangent(
                    start, control1, control2, end, t);
            float length = (float) Math.sqrt(
                    tangent.x * tangent.x + tangent.y * tangent.y);
            if (length < 0.001f) {
                length = 1f;
            }
            float px = -tangent.y / length;
            float py = tangent.x / length;
            float width = lerp(startWidth, endWidth, smooth(t));
            float packetFade = (float) Math.sin(Math.PI * along);
            float spatialAlpha = lerp(
                    sourceAlpha, targetAlpha, smooth(t));
            glColor(color, packetFade * spatialAlpha);
            GL11.glVertex2f(point.x + px * width, point.y + py * width);
            GL11.glVertex2f(point.x - px * width, point.y - py * width);
        }
        GL11.glEnd();
    }

    private static Vector2f cubic(Vector2f start, Vector2f control1,
            Vector2f control2, Vector2f end, float t) {
        float inverse = 1f - t;
        float inverse2 = inverse * inverse;
        float t2 = t * t;
        return new Vector2f(
                inverse2 * inverse * start.x
                        + 3f * inverse2 * t * control1.x
                        + 3f * inverse * t2 * control2.x
                        + t2 * t * end.x,
                inverse2 * inverse * start.y
                        + 3f * inverse2 * t * control1.y
                        + 3f * inverse * t2 * control2.y
                        + t2 * t * end.y);
    }

    private static Vector2f cubicTangent(Vector2f start,
            Vector2f control1, Vector2f control2,
            Vector2f end, float t) {
        float inverse = 1f - t;
        return new Vector2f(
                3f * inverse * inverse * (control1.x - start.x)
                        + 6f * inverse * t
                        * (control2.x - control1.x)
                        + 3f * t * t * (end.x - control2.x),
                3f * inverse * inverse * (control1.y - start.y)
                        + 6f * inverse * t
                        * (control2.y - control1.y)
                        + 3f * t * t * (end.y - control2.y));
    }

    private static void drawGlow(
            Vector2f center, float radius, Color color, float alpha) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        glColor(color, alpha);
        GL11.glVertex2f(center.x, center.y);
        for (int i = 0; i <= 24; i++) {
            float angle = 360f * i / 24f;
            Vector2f edge = offset(center, angle, radius);
            glColor(color, 0f);
            GL11.glVertex2f(edge.x, edge.y);
        }
        GL11.glEnd();
    }

    private static Color blend(Color a, Color b, float amount) {
        float value = clamp(amount, 0f, 1f);
        return new Color(
                Math.round(lerp(a.getRed(), b.getRed(), value)),
                Math.round(lerp(a.getGreen(), b.getGreen(), value)),
                Math.round(lerp(a.getBlue(), b.getBlue(), value)));
    }

    private static float angle(Vector2f from, Vector2f to) {
        return (float) Math.toDegrees(
                Math.atan2(to.y - from.y, to.x - from.x));
    }

    private static float distanceSquared(Vector2f a, Vector2f b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        return dx * dx + dy * dy;
    }

    private static Vector2f offset(
            Vector2f center, float angle, float distance) {
        double radians = Math.toRadians(angle);
        return new Vector2f(
                center.x + (float) Math.cos(radians) * distance,
                center.y + (float) Math.sin(radians) * distance);
    }

    private static Vector2f basisPoint(Vector2f center,
            Vector2f forward, Vector2f lateral,
            float forwardDistance, float lateralDistance) {
        return new Vector2f(
                center.x + forward.x * forwardDistance
                        + lateral.x * lateralDistance,
                center.y + forward.y * forwardDistance
                        + lateral.y * lateralDistance);
    }

    private static void rotateInPlace(Vector2f vector, float degrees) {
        if (Math.abs(degrees) < 0.0001f) {
            return;
        }
        double radians = Math.toRadians(degrees);
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        float x = vector.x;
        float y = vector.y;
        vector.x = x * cosine - y * sine;
        vector.y = x * sine + y * cosine;
    }

    private static float fract(float value) {
        float result = value - (float) Math.floor(value);
        return result < 0f ? result + 1f : result;
    }

    private static float smooth(float value) {
        float x = clamp(value, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    private static float lerp(float a, float b, float amount) {
        return a + (b - a) * amount;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void glColor(Color color, float alpha) {
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, clamp(alpha, 0f, 1f));
    }
}
