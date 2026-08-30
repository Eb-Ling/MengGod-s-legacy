package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.methods.Meng_MingGodSpaceCutPlugin;
import org.lwjgl.util.vector.Vector2f;
/** Activates the visual-only first stage of MingGod's spatial exchange system. */
public class Meng_MingGod_Spacedamage extends BaseShipSystemScript {
	private static final float PLAYER_GLOBAL_TIME_MULT = 1f / 20f;
	private static final float NPC_GLOBAL_TIME_MULT = 1f / 5f;
	private static final float EFFECT_DURATION = 10f;
	private static final float SYSTEM_RANGE = 3000f;
	private boolean triggered;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (!(stats.getEntity() instanceof ShipAPI)) return;

		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (triggered) return;
		if (state == State.IN) return;

		float globalTimeMult = ship == Global.getCombatEngine().getPlayerShip() ? PLAYER_GLOBAL_TIME_MULT : NPC_GLOBAL_TIME_MULT;
		Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_MingGodSpaceCutPlugin(ship.getMouseTarget(), ship, globalTimeMult));
		triggered = true;
	}

	@Override
	public boolean isUsable(com.fs.starfarer.api.combat.ShipSystemAPI system, ShipAPI ship) {
		if (ship != Global.getCombatEngine().getPlayerShip()) return true;

		Vector2f offset = Vector2f.sub(ship.getMouseTarget(), ship.getLocation(), new Vector2f());
		return offset.lengthSquared() <= SYSTEM_RANGE * SYSTEM_RANGE;
	}

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
		if (!(stats.getEntity() instanceof ShipAPI)) return;

		triggered = false;
	}

	@Override
	public float getInOverride(ShipAPI ship) {
		return 0f;
	}

	@Override
	public float getActiveOverride(ShipAPI ship) {
		float globalTimeMult = ship == Global.getCombatEngine().getPlayerShip() ? PLAYER_GLOBAL_TIME_MULT : NPC_GLOBAL_TIME_MULT;
		return EFFECT_DURATION * globalTimeMult;
	}

	@Override
	public float getOutOverride(ShipAPI ship) {
		return 0f;
	}
}
