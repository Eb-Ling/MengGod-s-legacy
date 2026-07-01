package data.methods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.util.EnumSet;

public class Meng_MingGodPlugin implements CombatLayeredRenderingPlugin {
    private final ShipAPI target;
    private final ShipAPI ship;
    private final IntervalUtil interval=new IntervalUtil(0.02f,0.02f);
    private float chargelevel;
    private boolean end = false;
    private float size;
    private float eyearg;
    private Vector2f loc;

    public Meng_MingGodPlugin(ShipAPI targets,ShipAPI source) {
        target = targets;
        ship=source;
    }

    public void init(CombatEntityAPI entity) {
        loc = new Vector2f(target.getLocation().x, target.getLocation().y);
        size = target.getShieldRadiusEvenIfNoShield() * 3f;
        eyearg = (float) Math.random() * 360f;
    }

    @Override
    public void cleanup() {
    }

    @Override
    public boolean isExpired() {
        return end;//返回值为true时，Plugin删除。
    }

    @Override
    public void advance(float amount) {
        if(chargelevel<=0.1f) chargelevel+=amount*0.5*300f/size;
        else chargelevel+=amount*0.5f;
        float relchargelevel = chargelevel*10f;
        if(relchargelevel<=1f) {
            interval.advance(amount);
            if(interval.intervalElapsed()) {
                Vector2f damageloc = new Vector2f(loc.getX() + (float) Math.cos(Math.toRadians(eyearg)) * (relchargelevel * size - 0.5f * size), loc.getY() + (float) Math.sin(Math.toRadians(eyearg)) * (relchargelevel * size - 0.5f * size));
                Global.getCombatEngine().applyDamage(damageloc, target, damageloc, 500f, DamageType.ENERGY, 0f, false, false, ship, true);
                Global.getCombatEngine().applyDamage(damageloc, target, damageloc, 0f, DamageType.ENERGY, 200f, true, false, ship, true);
            }
        }
        if(chargelevel>1f){
            end=true;
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
        float relchargelevel = chargelevel*10f;
        if (layer == CombatEngineLayers.ABOVE_PARTICLES ) {
            SpriteAPI sprite = Global.getSettings().getSprite("Meng_MingGod", "Meng_MingGod_Eyelight");

            sprite.setSize(size, size * 10f / 326f);
            float hight = sprite.getTextureHeight();
            float width = sprite.getTextureWidth();
            Vector2f uv = new Vector2f(Math.min(1f,relchargelevel) * width, hight);
            Vector2f location = loc;

            Vector2f sizes = new Vector2f(Math.min(1f,relchargelevel) * size - 0.5f * size, size * 10f / 326f / 2f);
            float facing = eyearg;

            GL11.glPushMatrix();

            GL11.glTranslatef(location.x, location.y, 0.0f);

            GL11.glRotatef(facing, 0f, 0f, 1f);

            GL11.glEnable(GL11.GL_TEXTURE_2D);

            sprite.bindTexture();

            GL11.glEnable(GL11.GL_BLEND);

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            if(relchargelevel>=1f){
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1f-(chargelevel-0.1f)/0.9f);
            }
            else {
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            }

            GL11.glBegin(GL11.GL_QUADS);

            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex2f(-size / 2f, -sizes.y);

            GL11.glTexCoord2f(0.0f, uv.y);
            GL11.glVertex2f(-size / 2f, sizes.y);

            GL11.glTexCoord2f(uv.x, uv.y);
            GL11.glVertex2f(sizes.x, sizes.y);

            GL11.glTexCoord2f(uv.x, 0.0f);
            GL11.glVertex2f(sizes.x, -sizes.y);

            GL11.glEnd();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            GL11.glPopMatrix();

        }
    }

}