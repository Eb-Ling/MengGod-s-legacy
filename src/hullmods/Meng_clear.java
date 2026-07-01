package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class Meng_clear extends BaseHullMod {
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);

        ship.setDHullOverlay("graphics/Meng/hullmods/meng_cover.png");//将D插的破损覆盖贴图替换为一个空白贴图即可去掉D插的不美观因素。
        ship.clearDamageDecals();//将舰船受伤的外观破损清除。

    }
}
