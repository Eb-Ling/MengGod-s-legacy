package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Noise;
import data.methods.Meng_V2arcfind;
import data.methods.Meng_arcfind;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

public class Meng_MingGodCenter extends BaseHullMod {
    public static final String KEY = "Meng_MingGodheartlistener";
    public static final String id = "Meng_MingGodheartsign";
    public final float range = 80f;

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 225, 171, 255);
        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);
        LabelAPI label2 = tooltip.addPara(
                "#舰船每隔 %s 秒将会重新装填所有导弹武器的弹药。\n#舰船投射的弹体将经由差分空间潮汐加速 %s%% 。\n#所有非导弹武器射程增加 %s%% 。",
                opad, highlight, "10", "100", "80"
        );
        label2.setHighlight("10", "100%", "80%");
        label2.setHighlightColors(highlight, highlight, highlight);

        tooltip.addSectionHeading("冥", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "这艘舰船的能量来源于名为 \"冥\" 的差分空间锁，舰船朝向范围内60°的且距离在3000su以内的舰船将会被 \"冥\"注视。\n\n?当注视完全成型，舰船可通过战术系统对所有被注视的目标投射 空间裂缝。\n?冥河摧毁的目标将会被\"冥\"注视，在原地张开 虚无空间，形成标记，虚无空间将随时间逐渐活化，并会持续吸收周围弹体加速活化进度，当活化达到一定程度将会发生爆炸，造成至多 5000点能量伤害。",
                opad, highlight, "20"

        );
        label.setHighlight("\"冥\"", "空间裂缝", "虚无空间", "次元裂隙", "5000");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);


        LabelAPI label1 = tooltip.addPara(
                "“冥域注视之人，亦无容身之地。”",
                opad, new Color(106, 255, 255, 218), "“冥域注视之人，亦无容身之地。”"

        );
        label1.setHighlight(0, 100);
        LabelAPI para = tooltip.addSectionHeading("阶梯计划", Alignment.MID, opad);
        para.setHighlightColors(new Color(0, 255, 221, 218));
        para.setHighlight(0, 100);

        label = tooltip.addPara(
                "阶梯计划在创立之初便做好了失控的应对，当舰队中同时出现一艘以上的阶梯计划舰船时，所有的阶梯计划舰船的系统都会全部下线。",
                opad, highlight);
        label.italicize();
        label.setHighlightColors(new Color(250, 0, 0, 255));
        label.setHighlight(0, 100);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, range);
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, range);
        stats.getProjectileSpeedMult().modifyPercent(id, 100f);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;

        if (isInPlayerFleet(ship)) {
            PersonAPI fc = null;
            FleetMemberAPI member = ship.getFleetMember();
            if (member != null) {
                for (FleetMemberAPI mem : ship.getFleetMember().getFleetData().getMembersListCopy()) {
                    if (mem.getHullSpec() != member.getHullSpec() && mem.getHullSpec().getTags().contains("Meng_embers") && !mem.isMothballed()) {
                        ship.setCurrentCR(0f);
                    }
                }
            }
        }


        for (ShipAPI target : Global.getCombatEngine().getShips()) {
            if (target != null) {
                if (target.getOwner() != ship.getOwner()) {
                    if (ship.isAlive()) {
                        //筛选全场符合要求的舰船。
                        if (!target.hasListenerOfClass(MyDamageListener1.class)) {
                            target.addListener(new MyDamageListener1(ship, target));
                        }
                    }
                }
            }
        }
        if (!ship.getCustomData().containsKey(KEY)) {
            DataContainer data = new DataContainer();
            ship.setCustomData(KEY, data);
        }
        DataContainer data = (DataContainer) ship.getCustomData().get(KEY);
        data.interval.advance(amount);
        if (data.interval.intervalElapsed()) {
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getType() != WeaponAPI.WeaponType.MISSILE) continue;

                if (w.usesAmmo() && w.getAmmo() < w.getMaxAmmo()) {
                    w.setAmmo(w.getMaxAmmo());
                }
            }
        }
        //为每一艘目标舰船添加监听。

        if (Objects.equals(ship.getHullSpec().getHullId(), "Meng_MingGod") && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.NONE && ship.getShield() != null && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.PHASE) {
            ship.getShield().setRingColor(new Color(250, 27, 27, 255));
        }
    }

    public static class DataContainer {
        IntervalUtil interval = new IntervalUtil(10f, 10f);
    }

    private static class MyDamageListener1 implements DamageListener {
        public ShipAPI target;
        public ShipAPI ships;
        private boolean init = false;

        public MyDamageListener1(ShipAPI ship, ShipAPI target) {
            this.target = target;
            ships = ship;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (source instanceof ShipAPI && !this.target.isAlive() && ships != null) {
                ShipAPI targets = (ShipAPI) source;
                if (targets == ships && result.getDamageToHull() > this.target.getHitpoints()) {
                    if (!init) {
                        init = true;
                        Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_MingGodSignPlugin(this.target, ships));
                    }
                }
            }
        }
    }

    public static class Meng_MingGodSignPlugin implements CombatLayeredRenderingPlugin {
        private final ShipAPI target;
        private final ShipAPI ship;
        private float chargelevel = 0f;
        private float energy = 0f;
        private ArrayList<DamagingProjectileAPI> targets;
        private float timer = 0f;
        private float f = 0f;
        private float spritecharger = 0f;
        private boolean end = false;
        private float size;
        private float[] lastnoise;
        private float[] ideanoise;
        private float[] noise;
        private Vector2f loc;
        private float arg;
        
        private int shaderProgram;
        private int vbo;
        private int ibo;
        private int uRadiusLoc;
        private int uTimeLoc;
        private int uDurationLoc;
        private int uProgressLoc;

        public Meng_MingGodSignPlugin(ShipAPI targets, ShipAPI source) {
            target = targets;
            ship = source;
        }

        public void init(CombatEntityAPI entity) {
            loc = new Vector2f(target.getLocation().x, target.getLocation().y);
            size = 0f;
            lastnoise = Noise.genNoise(360, 1f);
            ideanoise = Noise.genNoise(360, 1f);
            noise = Arrays.copyOf(lastnoise, 360);
            arg = (float) Math.random() * 360f;
            
            createShaderProgram();
        }
        
        private void createShaderProgram() {

            try {
                String vertexSource = 
                    "#version 110\n" +
                    "attribute vec2 a_position;\n" +
                    "attribute vec2 a_texCoord;\n" +
                    "varying vec2 v_uv;\n" +
                    "uniform float u_radius;\n" +
                    "void main() {\n" +
                    "    v_uv = a_texCoord;\n" +
                    "    vec2 scaledPos = a_position * (u_radius / 400.0);\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * vec4(scaledPos, 0.0, 1.0);\n" +
                    "}\n";
                
                String fragmentSource = 
                    "#version 110\n" +
                    "varying vec2 v_uv;\n" +
                    "uniform float u_time;\n" +
                    "uniform float u_duration;\n" +
                    "uniform float u_progress;\n" +
                    "\n" +
                    "vec4 permute(vec4 x) {\n" +
                    "    return mod(((x * 34.0) + 1.0) * x, 289.0);\n" +
                    "}\n" +
                    "\n" +
                    "float snoise(vec3 v) {\n" +
                    "    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);\n" +
                    "    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);\n" +
                    "\n" +
                    "    vec3 i  = floor(v + dot(v, C.yyy));\n" +
                    "    vec3 x0 = v - i + dot(i, C.xxx);\n" +
                    "\n" +
                    "    vec3 g = step(x0.yzx, x0.xyz);\n" +
                    "    vec3 l = 1.0 - g;\n" +
                    "    vec3 i1 = min(g.xyz, l.zxy);\n" +
                    "    vec3 i2 = max(g.xyz, l.zxy);\n" +
                    "\n" +
                    "    vec3 x1 = x0 - i1 + C.xxx;\n" +
                    "    vec3 x2 = x0 - i2 + C.yyy;\n" +
                    "    vec3 x3 = x0 - D.yyy;\n" +
                    "\n" +
                    "    i = mod(i, 289.0);\n" +
                    "    vec4 p = permute(permute(permute(\n" +
                    "             i.z + vec4(0.0, i1.z, i2.z, 1.0))\n" +
                    "           + i.y + vec4(0.0, i1.y, i2.y, 1.0))\n" +
                    "           + i.x + vec4(0.0, i1.x, i2.x, 1.0));\n" +
                    "\n" +
                    "    float n_ = 0.142857142857;\n" +
                    "    vec3 ns = n_ * D.wyz - D.xzx;\n" +
                    "\n" +
                    "    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);\n" +
                    "\n" +
                    "    vec4 x_ = floor(j * ns.z);\n" +
                    "    vec4 y_ = floor(j - 7.0 * x_);\n" +
                    "\n" +
                    "    vec4 x = x_ * ns.x + ns.yyyy;\n" +
                    "    vec4 y = y_ * ns.x + ns.yyyy;\n" +
                    "    vec4 h = 1.0 - abs(x) - abs(y);\n" +
                    "\n" +
                    "    vec4 b0 = vec4(x.xy, y.xy);\n" +
                    "    vec4 b1 = vec4(x.zw, y.zw);\n" +
                    "\n" +
                    "    vec4 s0 = floor(b0) * 2.0 + 1.0;\n" +
                    "    vec4 s1 = floor(b1) * 2.0 + 1.0;\n" +
                    "    vec4 sh = -step(h, vec4(0.0));\n" +
                    "\n" +
                    "    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;\n" +
                    "    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;\n" +
                    "\n" +
                    "    vec3 p0 = vec3(a0.xy, h.x);\n" +
                    "    vec3 p1 = vec3(a0.zw, h.y);\n" +
                    "    vec3 p2 = vec3(a1.xy, h.z);\n" +
                    "    vec3 p3 = vec3(a1.zw, h.w);\n" +
                    "\n" +
                    "    vec4 norm = 1.0 / vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3));\n" +
                    "    p0 *= norm.x;\n" +
                    "    p1 *= norm.y;\n" +
                    "    p2 *= norm.z;\n" +
                    "    p3 *= norm.w;\n" +
                    "\n" +
                    "    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);\n" +
                    "    m = m * m;\n" +
                    "    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));\n" +
                    "}\n" +
                    "\n" +
                    "float turbulence(vec3 p) {\n" +
                    "    float value = 0.0;\n" +
                    "    float amplitude = 1.0;\n" +
                    "    float frequency = 1.0;\n" +
                    "\n" +
                    "    for (int i = 0; i < 6; i++) {\n" +
                    "        value += amplitude * abs(snoise(p * frequency));\n" +
                    "        amplitude *= 0.5;\n" +
                    "        frequency *= 2.0;\n" +
                    "    }\n" +
                    "\n" +
                    "    return value * 0.5;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 center = vec2(0.5, 0.5);\n" +
                    "    vec2 toCenter = 2.0*(v_uv - center);\n" +
                    "    float dist = length(toCenter);\n" +
                    "\n" +
                    "    float angle = atan(toCenter.y, toCenter.x);\n" +
                    "    float normalizedAngle = angle / (2.0 * 3.14159);\n" +
                    "    \n" +
                    "    float radialGradient = dist;\n" +
                    "    \n" +
                    "    float rotationOffset = u_time * 0.1;\n" +
                    "    float contractionOffset = radialGradient + u_time* 0.2;\n" +
                    "    \n" +
                    "    float combined = normalizedAngle + pow(radialGradient,0.36) - rotationOffset;\n" +
                    "    \n" +
                    "    float spiralValue = cos(combined * 3.14159 * 2.0);\n" +
                    "    \n" +
                    "    vec3 spiralCoord = vec3(spiralValue, contractionOffset, u_time * 0.1);\n" +
                    "    float fogNoise = turbulence(spiralCoord);\n" +
                    "     fogNoise = pow(fogNoise,0.5);\n" +
                    "    \n" +
                    "    float radialFade =smoothstep(1.0, 0.5, dist);\n" +
                    "    float innerFade =smoothstep(0.25+0.025*sin(u_time*3.0),0.5+0.025*sin(u_time*3.0),dist);\n" +
                    "    vec3 color = vec3(1.0, 0.65, 0.75) * fogNoise * radialFade * innerFade;\n" +
                    "    \n" +
                    "    gl_FragColor = vec4(color,  max((1.0-innerFade),fogNoise*radialFade * 0.8));\n" +
                    "}\n";
                
                int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
                GL20.glShaderSource(vert, vertexSource);
                GL20.glCompileShader(vert);
                if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                    System.err.println("MingGodSign vert compile failed: " + GL20.glGetShaderInfoLog(vert, 1024));
                    return;
                }

                int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
                GL20.glShaderSource(frag, fragmentSource);
                GL20.glCompileShader(frag);
                if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                    System.err.println("MingGodSign frag compile failed: " + GL20.glGetShaderInfoLog(frag, 1024));
                    return;
                }

                shaderProgram = GL20.glCreateProgram();
                GL20.glAttachShader(shaderProgram, vert);
                GL20.glAttachShader(shaderProgram, frag);
                GL20.glBindAttribLocation(shaderProgram, 0, "a_position");
                GL20.glBindAttribLocation(shaderProgram, 1, "a_texCoord");
                GL20.glLinkProgram(shaderProgram);

                if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                    System.err.println("MingGodSign link failed: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
                    return;
                }

                GL20.glDeleteShader(vert);
                GL20.glDeleteShader(frag);
                
                uRadiusLoc = GL20.glGetUniformLocation(shaderProgram, "u_radius");
                uTimeLoc = GL20.glGetUniformLocation(shaderProgram, "u_time");
                uDurationLoc = GL20.glGetUniformLocation(shaderProgram, "u_duration");
                uProgressLoc = GL20.glGetUniformLocation(shaderProgram, "u_progress");

                createBuffers();
            } catch (Exception e) {
                System.err.println("MingGodSign shader error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        private void createBuffers() {
            float halfSize = 400f;

            FloatBuffer verts = org.lwjgl.BufferUtils.createFloatBuffer(16);
            verts.put(new float[]{
                -halfSize, -halfSize,  0f, 0f,
                 halfSize, -halfSize,  1f, 0f,
                 halfSize,  halfSize,  1f, 1f,
                -halfSize,  halfSize,  0f, 1f,
            });
            verts.flip();

            IntBuffer indices = org.lwjgl.BufferUtils.createIntBuffer(6);
            indices.put(new int[]{0, 1, 2, 0, 2, 3});
            indices.flip();

            vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verts, GL15.GL_STATIC_DRAW);

            ibo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        }

        @Override
        public void cleanup() {
            if (shaderProgram > 0) {
                GL20.glDeleteProgram(shaderProgram);
                shaderProgram = 0;
            }
            if (vbo != 0) {
                GL15.glDeleteBuffers(vbo);
                vbo = 0;
            }
            if (ibo != 0) {
                GL15.glDeleteBuffers(ibo);
                ibo = 0;
            }
        }

        @Override
        public boolean isExpired() {
            return end;
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            timer += amount * (1 + chargelevel);
            f += amount;
            if (f > 1f) {
                f = 0f;
                lastnoise = Arrays.copyOf(ideanoise, 360);
                ideanoise = Noise.genNoise(360, 1f);
            }
            for (int i = 0; i < 360; i++) {
                noise[i] = lastnoise[i] + (ideanoise[i] - lastnoise[i]) * f;
            }
            if (targets == null) {
                targets = new ArrayList<>();
            }
            if (chargelevel <= 0f) {
                spritecharger += amount;
                size = target.getShieldRadiusEvenIfNoShield() * 2f * spritecharger;
            }
            if (spritecharger >= 1f) {
                float range = size*1.5f;
                for (ShipAPI target : engine.getShips()) {
                    float nowrange = MathUtils.getDistance(loc, target.getLocation());
                    if (nowrange <= range && target.isHulk() && target.getOwner() != ship.getOwner()) {
                        Vector2f vel = new Vector2f(nowrange * (loc.getX() - target.getLocation().getX()), nowrange * (loc.getY() - target.getLocation().getY()));
                        target.getVelocity().set(vel);
                    }
                }
                for (DamagingProjectileAPI target : engine.getProjectiles()) {
                    float nowrange = MathUtils.getDistance(loc, target.getLocation());
                    if (nowrange <= range) {
                        if (!targets.contains(target)) {
                            targets.add(target);
                            energy += target.getDamageAmount() / size / 10f;
                        }

                        Vector2f force = new Vector2f(500f * (float) Math.cos(Math.toRadians(Meng_arcfind.Findarc(target.getLocation(), loc))), 500f * (float) Math.sin(Math.toRadians(Meng_arcfind.Findarc(target.getLocation(), loc))));
                        Vector2f vel = new Vector2f(target.getVelocity().x, target.getVelocity().y);
                        Vector2f.add(vel, force, vel);

                        target.getVelocity().set(vel);
                        target.setFacing(Meng_V2arcfind.Findarc(vel));
                        if (nowrange < range * 0.3f) {
                            engine.removeEntity(target);
                        }

                    }
                }
                chargelevel += 300f / size * amount * (0.05f + energy * 0.05f);
            }
            Color color1 = new Color(135, 15, 200, 120);
            Color color2 = new Color(180, 60, 250, 150);
            if (chargelevel >= 1f) {
                DamagingExplosionSpec spc = new DamagingExplosionSpec(
                        size * 0.25f,
                        size * 6f,
                        size * 3f,
                        size * 40f,
                        size * 4f,
                        CollisionClass.PROJECTILE_NO_FF,
                        CollisionClass.PROJECTILE_NO_FF,
                        size * 0.06f,
                        size * 0.01f,
                        size * 0.015f,
                        Math.round(size * 0.015f),
                        color1, color2);
                spc.setDamageType(DamageType.ENERGY);
                spc.setShowGraphic(true);
                engine.spawnDamagingExplosion(spc, ship, loc);
                end = true;
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_PARTICLES);
        }

        @Override
        public float getRenderRadius() {
            return 10000000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer == CombatEngineLayers.ABOVE_PARTICLES && shaderProgram > 0 && size > 0f) {
                float progress = Math.min(chargelevel, 1.0f);
                float duration = 100.0f;
                float elapsed = timer;
                
                float currentRadius = size*1.8f * Math.min(spritecharger, 1.0f);

                GL11.glPushMatrix();
                GL11.glTranslatef(loc.x, loc.y, 0f);
                GL11.glRotatef(arg, 0f, 0f, 1f);

                GL20.glUseProgram(shaderProgram);

                GL20.glUniform1f(uRadiusLoc, currentRadius);
                GL20.glUniform1f(uTimeLoc, elapsed);
                GL20.glUniform1f(uDurationLoc, duration);
                GL20.glUniform1f(uProgressLoc, progress);

                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
                GL20.glEnableVertexAttribArray(1);
                GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);

                GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);

                GL20.glDisableVertexAttribArray(0);
                GL20.glDisableVertexAttribArray(1);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

                GL20.glUseProgram(0);

                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glPopMatrix();
            }
        }

    }
}