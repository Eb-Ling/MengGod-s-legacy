package data.methods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.util.EnumSet;

public class Meng_Noname_afterplugin implements CombatLayeredRenderingPlugin {
    private WeaponAPI sourcewea;
    private ShipAPI ship;
    private boolean left;
    private boolean end=false;
    private Vector2f[] points1=new Vector2f[3];
    private Vector2f[] underpoints1=new Vector2f[3];
    private Vector2f[] dpoints1=new Vector2f[3];
    private Vector2f[] underdpoints1=new Vector2f[3];
    
    private Vector2f[] pointsVelocity = new Vector2f[3];
    private Vector2f[] underpointsVelocity = new Vector2f[3];
    private Vector2f[] dpointsVelocity = new Vector2f[3];
    private Vector2f[] underdpointsVelocity = new Vector2f[3];
    
    private float vexelasped=0f;
    private float lastShipFacing = 0f;
    private Vector2f lastShipVelocity = new Vector2f();

    public Meng_Noname_afterplugin(WeaponAPI source,ShipAPI thisship,boolean renderleft) {
        sourcewea=source;
        ship=thisship;
        left=renderleft;
    }

    public void init(CombatEntityAPI entity) {
        Vector2f sourceloc=sourcewea.getLocation();
        float facing = ship.getFacing();
        lastShipFacing = facing;
        
        float angleMultiplier = left ? 1f : -1f;
        
        initializeRibbonPoints(sourceloc, facing, angleMultiplier);

        for (int i = 0; i < 3; i++) {
            pointsVelocity[i] = new Vector2f(0f, 0f);
            underpointsVelocity[i] = new Vector2f(0f, 0f);
            dpointsVelocity[i] = new Vector2f(0f, 0f);
            underdpointsVelocity[i] = new Vector2f(0f, 0f);
        }
    }
    
    private void initializeRibbonPoints(Vector2f sourceloc, float facing, float angleMultiplier) {
        float point1arg = 110f * angleMultiplier + facing;
        float point2arg = 160f * angleMultiplier + facing;
        float point3arg = 135f * angleMultiplier + facing;
        float dpoint1arg = 120f * angleMultiplier + facing;
        float dpoint2arg = 180f * angleMultiplier + facing;
        float dpoint3arg = 160f * angleMultiplier + facing;
        
        float offset1 = 10f * angleMultiplier;
        
        points1[0] = createPoint(sourceloc, point1arg, 140f);
        points1[1] = createPoint(sourceloc, point2arg, 220f);
        points1[2] = createPoint(sourceloc, point3arg, 350f);
        
        underpoints1[0] = createPoint(sourceloc, point1arg + offset1, 140f);
        underpoints1[1] = createPoint(sourceloc, point2arg + offset1, 220f);
        underpoints1[2] = createPoint(sourceloc, point3arg + offset1, 350f);
        
        dpoints1[0] = createPoint(sourceloc, dpoint1arg, 140f);
        dpoints1[1] = createPoint(sourceloc, dpoint2arg, 220f);
        dpoints1[2] = createPoint(sourceloc, dpoint3arg, 350f);
        
        underdpoints1[0] = createPoint(sourceloc, dpoint1arg + offset1, 140f);
        underdpoints1[1] = createPoint(sourceloc, dpoint2arg + offset1, 220f);
        underdpoints1[2] = createPoint(sourceloc, dpoint3arg + offset1, 350f);
    }
    
    private Vector2f createPoint(Vector2f source, float angleDeg, float distance) {
        return new Vector2f(
            source.x + (float) Math.cos(Math.toRadians(angleDeg)) * distance,
            source.y + (float) Math.sin(Math.toRadians(angleDeg)) * distance
        );
    }

    @Override
    public void cleanup() {
    }

    @Override
    public boolean isExpired() {
        return end;
    }

    @Override
    public void advance(float amount) {
        if(ship==null||!ship.isAlive()) end=true;
        vexelasped+=amount*0.75f;
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

        if (layer == CombatEngineLayers.ABOVE_PARTICLES ) {
            CombatEngineAPI engine=Global.getCombatEngine();
            float amount=engine.getElapsedInLastFrame();
            
            SpriteAPI sprite = Global.getSettings().getSprite("Meng_Noname", "Meng_Noname_afterpicture");
            Vector2f sourceloc = sourcewea.getLocation();
            float facing = ship.getFacing();
            float angleMultiplier = left ? 1f : -1f;
            
            float point1arg = 110f * angleMultiplier + facing;
            float point2arg = 160f * angleMultiplier + facing;
            float point3arg = 135f * angleMultiplier + facing;
            float dpoint1arg = 120f * angleMultiplier + facing;
            float dpoint2arg = 180f * angleMultiplier + facing;
            float dpoint3arg = 160f * angleMultiplier + facing;
            
            float offset1 = 10f * angleMultiplier;
            
            Vector2f ideapoint1 = createPoint(sourceloc, point1arg, 140f);
            Vector2f ideapoint2 = createPoint(sourceloc, point2arg, 220f);
            Vector2f ideapoint3 = createPoint(sourceloc, point3arg, 350f);
            
            Vector2f ideaunderpoint1 = createPoint(sourceloc, point1arg + offset1, 140f);
            Vector2f ideaunderpoint2 = createPoint(sourceloc, point2arg + offset1, 220f);
            Vector2f ideaunderpoint3 = createPoint(sourceloc, point3arg + offset1, 350f);
            
            Vector2f ideadpoint1 = createPoint(sourceloc, dpoint1arg, 140f);
            Vector2f ideadpoint2 = createPoint(sourceloc, dpoint2arg, 220f);
            Vector2f ideadpoint3 = createPoint(sourceloc, dpoint3arg, 350f);
            
            Vector2f ideaunderdpoint1 = createPoint(sourceloc, dpoint1arg + offset1, 140f);
            Vector2f ideaunderdpoint2 = createPoint(sourceloc, dpoint2arg + offset1, 220f);
            Vector2f ideaunderdpoint3 = createPoint(sourceloc, dpoint3arg + offset1, 350f);
            
            Vector2f[] idealTargets = {ideapoint1, ideapoint2, ideapoint3};
            Vector2f[] underIdealTargets = {ideaunderpoint1, ideaunderpoint2, ideaunderpoint3};
            Vector2f[] dIdealTargets = {ideadpoint1, ideadpoint2, ideadpoint3};
            Vector2f[] underDIdealTargets = {ideaunderdpoint1, ideaunderdpoint2, ideaunderdpoint3};
            
            if(!engine.isPaused()) {
                updateRibbonPhysics(points1, pointsVelocity, idealTargets, amount, 0, facing);
                updateRibbonPhysics(underpoints1, underpointsVelocity, underIdealTargets, amount, 1, facing);
                updateRibbonPhysics(dpoints1, dpointsVelocity, dIdealTargets, amount, 2, facing);
                updateRibbonPhysics(underdpoints1, underdpointsVelocity, underDIdealTargets, amount, 3, facing);
                
                lastShipFacing = facing;
                lastShipVelocity.set(ship.getVelocity());
            }
            
            Vector2f point1 = projectToRadius(sourceloc, points1[0], 140f);
            Vector2f point2 = projectToRadius(sourceloc, points1[1], 220f);
            Vector2f point3 = projectToRadius(sourceloc, points1[2], 350f);

            Vector2f underpoint1 = projectToRadius(sourceloc, underpoints1[0], 140f);
            Vector2f underpoint2 = projectToRadius(sourceloc, underpoints1[1], 220f);
            Vector2f underpoint3 = projectToRadius(sourceloc, underpoints1[2], 350f);

            Vector2f dpoint1 = projectToRadius(sourceloc, dpoints1[0], 140f);
            Vector2f dpoint2 = projectToRadius(sourceloc, dpoints1[1], 220f);
            Vector2f dpoint3 = projectToRadius(sourceloc, dpoints1[2], 350f);

            Vector2f underdpoint1 = projectToRadius(sourceloc, underdpoints1[0], 140f);
            Vector2f underdpoint2 = projectToRadius(sourceloc, underdpoints1[1], 220f);
            Vector2f underdpoint3 = projectToRadius(sourceloc, underdpoints1[2], 350f);

            renderRibbon(sprite, sourceloc, point1, point2, point3, underpoint1, underpoint2, underpoint3);
            renderRibbon(sprite, sourceloc, dpoint1, dpoint2, dpoint3, underdpoint1, underdpoint2, underdpoint3);
        }
    }
    
    private void updateRibbonPhysics(Vector2f[] currentPoints, Vector2f[] velocities, 
                                     Vector2f[] targetPoints, float amount, int ribbonIndex, float facing) {
        float shipSpeed = ship.getVelocity().length();
        float shipAngularVel = Math.abs(ship.getAngularVelocity());
        
        float baseSpringStrength = 8.0f;
        float damping = 0.85f;
        float mass = 1.0f;
        
        float trailDrag = Math.min(shipSpeed / 200f, 1.0f);
        
        for (int i = 0; i < 3; i++) {
            float distanceRatio = (i + 1) / 3.0f;
            
            Vector2f toTarget = new Vector2f(targetPoints[i].x - currentPoints[i].x, 
                                              targetPoints[i].y - currentPoints[i].y);
            float distance = toTarget.length();
            
            float springStrength = baseSpringStrength * (1.0f + distanceRatio * 0.5f);
            
            Vector2f springForce = new Vector2f(toTarget.x * springStrength, 
                                                 toTarget.y * springStrength);
            
            float dragCoeff = 0.3f + trailDrag * 0.7f * distanceRatio;
            Vector2f dragForce = new Vector2f(
                -lastShipVelocity.x * dragCoeff * 60f * amount,
                -lastShipVelocity.y * dragCoeff * 60f * amount
            );
            
            float rotationDrag = shipAngularVel * distanceRatio * 50f;
            float facingDiff = facing - lastShipFacing;
            Vector2f rotationForce = new Vector2f(
                (float)Math.cos(Math.toRadians(facing + 90f)) * rotationDrag * Math.signum(facingDiff),
                (float)Math.sin(Math.toRadians(facing + 90f)) * rotationDrag * Math.signum(facingDiff)
            );
            
            Vector2f totalForce = new Vector2f(
                springForce.x + dragForce.x + rotationForce.x,
                springForce.y + dragForce.y + rotationForce.y
            );
            
            Vector2f acceleration = new Vector2f(totalForce.x / mass, totalForce.y / mass);
            
            velocities[i].x += acceleration.x * amount;
            velocities[i].y += acceleration.y * amount;
            
            velocities[i].x *= damping;
            velocities[i].y *= damping;
            
            float maxVelocity = 800f * (1.0f + distanceRatio);
            float velLength = velocities[i].length();
            if (velLength > maxVelocity) {
                velocities[i].scale(maxVelocity / velLength);
            }
            
            currentPoints[i].x += velocities[i].x * amount;
            currentPoints[i].y += velocities[i].y * amount;
        }
    }
    
    private Vector2f projectToRadius(Vector2f source, Vector2f point, float radius) {
        float angle = Meng_arcfind.Findarc(source, point);
        return new Vector2f(
            source.x + (float) Math.cos(Math.toRadians(angle)) * radius,
            source.y + (float) Math.sin(Math.toRadians(angle)) * radius
        );
    }
    
    private void renderRibbon(SpriteAPI sprite, Vector2f sourceloc, 
                              Vector2f point1, Vector2f point2, Vector2f point3,
                              Vector2f underpoint1, Vector2f underpoint2, Vector2f underpoint3) {
        for (int i = 0; i < 500; i++) {
            float t = i / 500f;
            
            float widthFactor;
            if (t <= 0.3f) {
                float fastT = t / 0.3f;
                widthFactor = 0.3f + 0.7f * fastT * fastT;
            } else {
                float slowT = (t - 0.3f) / 0.7f;
                widthFactor = 1.0f + slowT * 0.5f;
            }
            
            Vector2f pointloc = Meng_findbezierpoint.findpoint(sourceloc, point1, point2, point3, t);
            Vector2f underpointloc = Meng_findbezierpoint.findpoint(sourceloc, underpoint1, underpoint2, underpoint3, t);
            
            Vector2f direction = new Vector2f(underpointloc.x - pointloc.x, underpointloc.y - pointloc.y);
            float currentWidth = direction.length();
            float targetWidth = currentWidth * widthFactor;
            
            if (currentWidth > 0.001f) {
                float scale = targetWidth / currentWidth;
                Vector2f midpoint = new Vector2f(
                    (pointloc.x + underpointloc.x) * 0.5f,
                    (pointloc.y + underpointloc.y) * 0.5f
                );
                
                pointloc.x = midpoint.x - direction.x * scale * 0.5f;
                pointloc.y = midpoint.y - direction.y * scale * 0.5f;
                underpointloc.x = midpoint.x + direction.x * scale * 0.5f;
                underpointloc.y = midpoint.y + direction.y * scale * 0.5f;
            }
            
            GL11.glPushMatrix();
            
            GL11.glTranslatef(0f, 0f, 0.0f);
            GL11.glRotatef(0f, 0f, 0f, 1f);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            sprite.bindTexture();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1f - t);
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            
            GL11.glTexCoord2f(0f, i / 500f - vexelasped / 3f);
            GL11.glVertex2f(pointloc.x, pointloc.y);
            GL11.glTexCoord2f(1f, i / 500f - vexelasped / 3f);
            GL11.glVertex2f(underpointloc.x, underpointloc.y);
        }
        GL11.glEnd();
        GL11.glPopMatrix();
    }


}
