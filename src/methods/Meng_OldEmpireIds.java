package data.methods;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public final class Meng_OldEmpireIds {
    private Meng_OldEmpireIds() {}

    public static final String CHAIN = "Meng_OldEmpire_pulse_chain_gun";
    public static final String DUST = "Meng_OldEmpire_spirit_dust_missile";
    public static final String BEAM = "Meng_OldEmpire_continuous_flux_beam";
    public static final String TRACER = "Meng_OldEmpire_tracer_pursuit_beam";

    public static final String A_MULTI = "Meng_OldEmpire_alpha_multishot";
    public static final String A_HEAVY = "Meng_OldEmpire_alpha_heavy_caliber";
    public static final String A_SPLIT = "Meng_OldEmpire_alpha_warhead_split";
    public static final String B_BURST = "Meng_OldEmpire_beta_burst_rifling";
    public static final String B_RHYTHM = "Meng_OldEmpire_beta_deadly_rhythm";
    public static final String B_PRECISION = "Meng_OldEmpire_beta_precision_kinetics";
    public static final String G_VOLATILE = "Meng_OldEmpire_gamma_volatile_load";
    public static final String G_CHARGED = "Meng_OldEmpire_gamma_charged_breech";
    public static final String G_CRIMSON = "Meng_OldEmpire_gamma_crimson_taint";

    public static final String[] ALPHA = {A_MULTI, A_HEAVY, A_SPLIT};
    public static final String[] BETA = {B_BURST, B_RHYTHM, B_PRECISION};
    public static final String[] GAMMA = {G_VOLATILE, G_CHARGED, G_CRIMSON};

    public static boolean has(ShipAPI ship, String id) {
        return ship != null && ship.getVariant() != null && ship.getVariant().hasHullMod(id);
    }

    public static boolean isKrgWeapon(WeaponAPI weapon) {
        if (weapon == null) return false;
        String id = weapon.getId();
        return CHAIN.equals(id) || DUST.equals(id) || BEAM.equals(id) || TRACER.equals(id);
    }
}
