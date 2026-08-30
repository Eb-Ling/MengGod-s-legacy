package data.methods;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;

public class Meng_OldEmpireRangeModifier implements WeaponBaseRangeModifier {
    public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
        if (!Meng_OldEmpireIds.isKrgWeapon(weapon)) return 0f;
        float result = 0f;
        if (Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.A_HEAVY)) result += 30f;
        if (Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.B_BURST)) result -= 25f;
        if (Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.B_PRECISION)) result += 35f;
        return result;
    }

    public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
        return 1f;
    }

    public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
        return 0f;
    }
}
