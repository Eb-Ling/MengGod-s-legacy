package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.methods.Meng_findbezierpoint;
import data.methods.Meng_arcfind;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class Meng_Noname_WeaponTree implements BeamEffectPlugin {

    private static final String id = "Meng_bossweaponscript";
    private static final int RENDER_SEGMENTS = 500;

    private final IntervalUtil arcInterval = new IntervalUtil(0.04f, 0.07f);
    private final IntervalUtil visualInterval = new IntervalUtil(0.03F, 0.05F);
    private BranchNode rootNode;
    private boolean init = false;
    private boolean init1 = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (beam.getBrightness() <= 0f) return;
        WeaponAPI weapon = beam.getWeapon();
        if (rootNode == null) {
            rootNode = new BranchNode();
        }
        rootNode.angle = weapon.getCurrAngle();
        Vector2f beamto = new Vector2f(beam.getRayEndPrevFrame().x - 5f * (float) Math.cos(Math.toRadians(weapon.getCurrAngle())), beam.getRayEndPrevFrame().y - 5f * (float) Math.sin(Math.toRadians(weapon.getCurrAngle())));
        if (!init1) {
            init1 = true;
            rootNode.pos.set(beamto);
        }
        rootNode.pos.set(beamto);

        if (beam.getBrightness() < 1f) return;
        if (engine.isPaused()) return;

        if (!init) {
            init = true;
            rootNode.pos.set(beam.getTo());
            ShipAPI source = beam.getSource();
            for (int i = 0; i <= 3; i++) {
                engine.addLayeredRenderingPlugin(new Meng_NonameWeapon_SpawnTreePlugin(beam, rootNode, 3, 120f, 48f, 24f, source, 1f, 0f));
            }
        }
    }

    private static class BranchNode {
        Vector2f pos = new Vector2f();
        float angle;
    }

    public static class Meng_NonameWeapon_SpawnTreePlugin implements CombatLayeredRenderingPlugin {
        final BranchNode nextNode;
        private final BeamAPI beam;
        private boolean spawned = false;
        private final BranchNode startNode;
        private float timer = 0f;
        private float alpha = 1f;
        private Vector2f point;
        private final float randomAngleOffset;
        private float angle;
        private final float startwidth;
        private float endwidth;
        private final float radius;
        private final int num;
        private final float arg;
        private final ShipAPI source;
        private final float damageMult;
        private final float emp;
        private final List<Meng_NonameWeapon_SpawnTreePlugin> children = new ArrayList<>();
        private boolean expired = false;
        private final IntervalUtil particleInterval = new IntervalUtil(0.08f, 0.12f);
        private Vector2f hitPointForRender = null;
        private int hitSegmentIndex = -1;

        public Meng_NonameWeapon_SpawnTreePlugin(BeamAPI beams, BranchNode startNode, int nums, float args, float startwidths, float endwidths,
                                                  ShipAPI source, float damageMult, float emp) {
            beam = beams;
            this.startNode = startNode;
            num = nums;
            arg = args;
            startwidth = startwidths;
            endwidth = endwidths;
            this.source = source;
            this.damageMult = damageMult;
            this.emp = emp;

            Random random = new Random();
            randomAngleOffset = -arg * 0.5f + arg * random.nextFloat();
            radius = 500f + 500f * random.nextFloat();
            if (num == 1) this.endwidth = 0f;
            nextNode = new BranchNode();
            angle = startNode.angle + randomAngleOffset;
            point = MathUtils.getPointOnCircumference(startNode.pos, radius, angle);
        }


        private static Vector2f getCollisionPointOnCircumference(Vector2f segStart, Vector2f segEnd,
                                                                   Vector2f circleCenter, float circleRadius) {
            Vector2f startToEnd = Vector2f.sub(segEnd, segStart, null);
            Vector2f startToCenter = Vector2f.sub(circleCenter, segStart, null);
            double ptLineDistSq = java.awt.geom.Line2D.ptLineDistSq(
                    segStart.x, segStart.y, segEnd.x, segEnd.y, circleCenter.x, circleCenter.y);
            float circleRadiusSq = circleRadius * circleRadius;

            if (startToCenter.lengthSquared() < circleRadiusSq) {
                return segStart;
            }

            if (ptLineDistSq > circleRadiusSq
                    || startToCenter.length() - circleRadius > startToEnd.length()) {
                return null;
            }

            startToEnd.normalise(startToEnd);
            double dist = Vector2f.dot(startToCenter, startToEnd)
                    - Math.sqrt(circleRadiusSq - ptLineDistSq);
            startToEnd.scale((float) dist);
            return Vector2f.add(segStart, startToEnd, null);
        }

        private static Vector2f getShipCollisionPoint(Vector2f segStart, Vector2f segEnd,
                                                       ShipAPI ship, float aim) {
            if (ship.getCollisionClass() == CollisionClass.NONE) return null;

            ShieldAPI shield = ship.getShield();
            if (shield == null || shield.isOff()) {
                return CollisionUtils.getCollisionPoint(segStart, segEnd, ship);
            }

            Vector2f circleCenter = shield.getLocation();
            float circleRadius = shield.getRadius();

            if (MathUtils.isPointWithinCircle(segStart, circleCenter, circleRadius)) {
                if (shield.isWithinArc(segStart)) {
                    return MathUtils.getPoint(segStart, 15, aim);
                } else {
                    return CollisionUtils.getCollisionPoint(segStart, segEnd, ship);
                }
            }

            Vector2f tmp1 = getCollisionPointOnCircumference(segStart, segEnd, circleCenter, circleRadius);
            if (tmp1 != null && shield.isWithinArc(tmp1)) {
                return MathUtils.getPoint(tmp1, 1, aim);
            }

            Vector2f hullHit = CollisionUtils.getCollisionPoint(segStart, segEnd, ship);
            if (hullHit != null) {
                return MathUtils.getPoint(hullHit, 1, aim);
            }
            return null;
        }

        /**
         * 检查线段是否命中实体列表中的目标，返回最近命中目标。
         */
        private static CombatEntityAPI checkSegment(Vector2f segStart, Vector2f segEnd, float aim,
                                                      ShipAPI source, List<CombatEntityAPI> entities,
                                                      CombatEntityAPI[] outTarget, Vector2f[] outHitPoint) {
            CombatEntityAPI bestTarget = null;
            Vector2f bestCol = null;
            float bestDistSq = Float.MAX_VALUE;

            for (CombatEntityAPI e : entities) {
                if (e.getCollisionClass() == CollisionClass.NONE) continue;

                Vector2f col = null;
                float collRadius = e.getCollisionRadius();

                if (e instanceof ShipAPI) {
                    ShipAPI s = (ShipAPI) e;
                    if ((source == null || e != source)
                            && s.getParentStation() != s
                            && !(s.getCollisionClass() == CollisionClass.FIGHTER
                                    && s.getOwner() == (source != null ? source.getOwner() : -1)
                                    && !s.getEngineController().isFlamedOut())) {

                        if (!CollisionUtils.getCollides(segStart, segEnd, s.getLocation(), collRadius))
                            continue;

                        col = getShipCollisionPoint(segStart, segEnd, s, aim);
                    }
                } else if (e instanceof CombatAsteroidAPI
                        || (e instanceof MissileAPI && (source == null || e.getOwner() != source.getOwner()))) {

                    if (!CollisionUtils.getCollides(segStart, segEnd, e.getLocation(), collRadius))
                        continue;

                    col = getCollisionPointOnCircumference(segStart, segEnd, e.getLocation(), collRadius);
                }

                if (col != null) {
                    float dSq = MathUtils.getDistanceSquared(segStart, col);
                    if (dSq < bestDistSq) {
                        bestDistSq = dSq;
                        bestTarget = e;
                        bestCol = col;
                    }
                }
            }

            if (bestTarget != null) {
                outTarget[0] = bestTarget;
                outHitPoint[0] = bestCol;
                return bestTarget;
            }
            return null;
        }

        // ===== 辅助方法 =====

        private float findBezierTimer(Vector2f hitPoint, Vector2f from, Vector2f end, Vector2f bezierMid) {
            float bestT = 0f;
            float bestDistSq = Float.MAX_VALUE;
            for (int i = 0; i <= 100; i++) {
                float t = (float) i / 100f;
                Vector2f bp = Meng_findbezierpoint.findpointf2(from, bezierMid, end, t);
                float dSq = MathUtils.getDistanceSquared(bp, hitPoint);
                if (dSq < bestDistSq) {
                    bestDistSq = dSq;
                    bestT = t;
                }
            }
            return bestT;
        }

        private void expireSubtree() {
            expired = true;
            for (Meng_NonameWeapon_SpawnTreePlugin child : children) {
                child.expireSubtree();
            }
            children.clear();
        }

        @Override
        public void cleanup() {
        }

        @Override
        public void init(CombatEntityAPI combatEntityAPI) {
        }

        @Override
        public boolean isExpired() {
            return expired || beam.getBrightness() <= 0f;
        }

        @Override
        public void advance(float amount) {
            if (num == 0) return;

            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine.isPaused()) return;

            angle = startNode.angle + randomAngleOffset;
            point = MathUtils.getPointOnCircumference(startNode.pos, radius, angle);
            alpha = beam.getBrightness();

            CombatEntityAPI hitTarget = null;
            Vector2f hitPoint = null;
            float hitT = 1f;
            int hitSegIdx = -1;

            if (timer > 0f) {
                Vector2f sourceloc = startNode.pos;
                Vector2f targetloc = point;
                float branchAngle = startNode.angle;
                float dist = MathUtils.getDistance(sourceloc, targetloc);
                float d = dist * 0.33f;
                Vector2f bezierMid = new Vector2f(
                        sourceloc.x + d * (float) Math.cos(Math.toRadians(branchAngle)),
                        sourceloc.y + d * (float) Math.sin(Math.toRadians(branchAngle)));

                List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(sourceloc, radius + 500);

                if (!entities.isEmpty()) {
                    int grown = (int) (RENDER_SEGMENTS * Math.min(1f, timer));
                    CombatEntityAPI[] outTarget = new CombatEntityAPI[1];
                    Vector2f[] outPoint = new Vector2f[1];

                    for (int i = 0; i < grown; i++) {
                        int nextI = Math.min(i + 1, RENDER_SEGMENTS);
                        float t = (float) i / RENDER_SEGMENTS;
                        float tNext = (float) nextI / RENDER_SEGMENTS;
                        Vector2f segStart = Meng_findbezierpoint.findpointf2(sourceloc, bezierMid, targetloc, t);
                        Vector2f segEnd = Meng_findbezierpoint.findpointf2(sourceloc, bezierMid, targetloc, tNext);

                        if (checkSegment(segStart, segEnd, branchAngle, source, entities, outTarget, outPoint) != null) {
                            hitTarget = outTarget[0];
                            hitPoint = outPoint[0];
                            hitSegIdx = i;
                            hitT = findBezierTimer(hitPoint, sourceloc, targetloc, bezierMid);
                            break;
                        }
                    }
                }
            }

            hitPointForRender = hitPoint;
            hitSegmentIndex = hitSegIdx;

            if (hitTarget != null) {
                timer = Math.min(timer, hitT);

                float currentDamage = beam.getDamage().getDamage() * damageMult;
                DamageType currentType = beam.getDamage().getType();
                engine.applyDamage(hitTarget, hitPoint, currentDamage * amount, currentType, emp, false, true, source);

                particleInterval.advance(amount);
                if (particleInterval.intervalElapsed()) {
                    float segWidth = endwidth + (startwidth - endwidth) * (1f - 1f / RENDER_SEGMENTS * hitSegmentIndex);
                    float baseSize = Math.max(10f, segWidth * 2f);
                    engine.addHitParticle(hitPoint, new Vector2f(),
                            (float) Math.random() * baseSize / 2f + baseSize, 1f, 0.3f,
                            new Color(83, 134, 229, 245));
                    engine.addHitParticle(hitPoint, new Vector2f(),
                            (float) Math.random() * baseSize / 4f + baseSize / 2f, 1f, 0.2f,
                            new Color(17, 248, 248, 245));
                }

                for (Meng_NonameWeapon_SpawnTreePlugin child : children) {
                    child.expireSubtree();
                }
                children.clear();
                spawned = false;
            } else {
                timer += amount;
            }

            if (timer >= 1f) {
                if (!spawned) {
                    spawned = true;
                    nextNode.pos.set(point);
                    for (int i = 0; i <= 3; i++) {
                        Meng_NonameWeapon_SpawnTreePlugin child = new Meng_NonameWeapon_SpawnTreePlugin(
                                beam, nextNode, num - 1, 120f, endwidth, endwidth * 0.5f,
                                source, damageMult * 0.5f, emp);
                        children.add(child);
                        engine.addLayeredRenderingPlugin(child);
                    }
                }
                nextNode.pos.set(point);
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 10000000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
                SpriteAPI sprite = Global.getSettings().getSprite("fx", "Meng_Beamb");
                if (num == 0) return;
                int totalRender = (int) (RENDER_SEGMENTS * Math.min(1f, timer));
                for (int i = 0; i < totalRender; i++) {
                    Vector2f sourceloc = startNode.pos;
                    Vector2f targetloc = point;
                    float arg = startNode.angle;
                    float d = MathUtils.getDistance(sourceloc, targetloc) * 0.33f;
                    float width = endwidth + (startwidth - endwidth) * (1f - 1f / RENDER_SEGMENTS * i);
                    Vector2f midpoint = new Vector2f(sourceloc.x + d * (float) Math.cos(Math.toRadians(arg)), sourceloc.y + d * (float) Math.sin(Math.toRadians(arg)));
                    Vector2f p = Meng_findbezierpoint.findpointf2(sourceloc, midpoint, targetloc, 1f / RENDER_SEGMENTS * i);

                    boolean isLast = (i == totalRender - 1);
                    boolean useHitPoint = isLast && hitPointForRender != null && i == hitSegmentIndex;
                    Vector2f nextp;
                    if (useHitPoint) {
                        nextp = hitPointForRender;
                    } else {
                        nextp = Meng_findbezierpoint.findpointf2(sourceloc, midpoint, targetloc, 1f / RENDER_SEGMENTS * Math.min(i + 1, RENDER_SEGMENTS));
                    }
                    float currarg = Meng_arcfind.Findarc(p, nextp);
                    Vector2f point1 = new Vector2f(p.x + 0.5f * width * (float) Math.cos(Math.toRadians(currarg + 90f)), p.y + 0.5f * width * (float) Math.sin(Math.toRadians(currarg + 90f)));
                    Vector2f point2 = new Vector2f(p.x + 0.5f * width * (float) Math.cos(Math.toRadians(currarg - 90f)), p.y + 0.5f * width * (float) Math.sin(Math.toRadians(currarg - 90f)));
                    GL11.glPushMatrix();
                    GL11.glTranslatef(0f, 0f, 0.0f);
                    GL11.glRotatef(0f, 0f, 0f, 1f);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    sprite.bindTexture();
                    GL11.glEnable(GL11.GL_BLEND);
                    Color color1 = new Color(17, 248, 248, (int) Math.floor(255 * beam.getBrightness() * alpha));
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GL11.glColor4ub((byte) color1.getRed(), (byte) color1.getGreen(), (byte) color1.getBlue(), (byte) color1.getAlpha());

                    GL11.glBegin(GL11.GL_QUAD_STRIP);
                    GL11.glTexCoord2f(i / 100f, 0f);
                    GL11.glVertex2f(point1.x, point1.y);
                    GL11.glTexCoord2f(i / 100f, 1f);
                    GL11.glVertex2f(point2.x, point2.y);

                    if (i == RENDER_SEGMENTS - 20) {
                        nextNode.angle = currarg;
                    }
                }
                Vector2f point1 = new Vector2f(point.x + 0.5f * endwidth * (float) Math.cos(Math.toRadians(nextNode.angle + 90f)), point.y + 0.5f * endwidth * (float) Math.sin(Math.toRadians(nextNode.angle + 90f)));
                Vector2f point2 = new Vector2f(point.x + 0.5f * endwidth * (float) Math.cos(Math.toRadians(nextNode.angle - 90f)), point.y + 0.5f * endwidth * (float) Math.sin(Math.toRadians(nextNode.angle - 90f)));
                if (timer >= 1f) {
                    GL11.glTexCoord2f(5f, 0f);
                    GL11.glVertex2f(point1.x, point1.y);
                    GL11.glTexCoord2f(5f, 1f);
                    GL11.glVertex2f(point2.x, point2.y);
                }
                GL11.glEnd();
                GL11.glPopMatrix();
            }
        }
    }
}
