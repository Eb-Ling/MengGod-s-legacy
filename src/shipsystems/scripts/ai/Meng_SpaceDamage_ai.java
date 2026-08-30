package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import data.methods.Meng_MingGodSpaceCutPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;

public class Meng_SpaceDamage_ai implements ShipSystemAIScript {
	private static final float MIN_ACTIVATION_SCORE = 2f;
	private static final float MIN_SWAP_SCORE = 2f;
	private static final float SWAP_INTERVAL = 0.08f;
	private static final int SECTOR_COUNT = 8;
	private static final float SECTOR_ANGLE = (float) (Math.PI * 2d / SECTOR_COUNT);

	private ShipAPI ship;
	private ShipSystemAPI system;
	private CombatEngineAPI engine;
	private float swapTimer;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		this.engine = engine;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (engine.isPaused() || !ship.isAlive()) return;

		if (system.getState() == ShipSystemAPI.SystemState.IDLE) {
			Vector2f center = chooseActivationCenter(target);
			if (center == null || !system.canBeActivated()) return;

			ship.getMouseTarget().set(center);
			ship.useSystem();
			return;
		}

		if (!system.isActive()) return;

		Object pluginObject = ship.getCustomData().get(Meng_MingGodSpaceCutPlugin.ACTIVE_PLUGIN_KEY);
		if (!(pluginObject instanceof Meng_MingGodSpaceCutPlugin)) return;

		swapTimer += amount;
		if (swapTimer < SWAP_INTERVAL) return;
		swapTimer = 0f;

		Meng_MingGodSpaceCutPlugin plugin = (Meng_MingGodSpaceCutPlugin) pluginObject;
		int[] swap = chooseBestSwap(ship.getMouseTarget(), plugin.getSectorMapSnapshot());
		if (swap == null) return;

		plugin.requestSwap(swap[0], swap[1]);
	}

	private Vector2f chooseActivationCenter(ShipAPI target) {
		ArrayList<Vector2f> candidates = new ArrayList<Vector2f>();
		if (target != null && target.isAlive()) {
			candidates.add(new Vector2f(target.getLocation()));
		}
		for (ShipAPI candidate : engine.getShips()) {
			if (!candidate.isAlive() || candidate.isFighter() || candidate.isDrone()) continue;
			if (candidate.getOwner() == ship.getOwner()) continue;
			candidates.add(new Vector2f(candidate.getLocation()));
		}

		Vector2f bestCenter = null;
		float bestScore = MIN_ACTIVATION_SCORE;
		for (Vector2f candidate : candidates) {
			Vector2f limitedCandidate = limitToSystemRange(candidate);
			float score = scoreActivationCenter(limitedCandidate);
			if (score <= bestScore) continue;
			bestScore = score;
			bestCenter = limitedCandidate;
		}
		return bestCenter;
	}

	private Vector2f limitToSystemRange(Vector2f candidate) {
		Vector2f offset = Vector2f.sub(candidate, ship.getLocation(), new Vector2f());
		if (offset.lengthSquared() <= Meng_MingGodSpaceCutPlugin.SPACE_RADIUS * Meng_MingGodSpaceCutPlugin.SPACE_RADIUS) {
			return candidate;
		}
		offset.normalise();
		return new Vector2f(ship.getLocation().x + offset.x * Meng_MingGodSpaceCutPlugin.SPACE_RADIUS,
				ship.getLocation().y + offset.y * Meng_MingGodSpaceCutPlugin.SPACE_RADIUS);
	}

	private float scoreActivationCenter(Vector2f center) {
		float enemyScore = 0f;
		float allyRisk = 0f;
		for (ShipAPI candidate : engine.getShips()) {
			if (!candidate.isAlive()) continue;
			if (distanceSquared(center, candidate.getLocation()) > Meng_MingGodSpaceCutPlugin.SPACE_RADIUS * Meng_MingGodSpaceCutPlugin.SPACE_RADIUS) continue;

			float weight = shipWeight(candidate);
			if (candidate.getOwner() == ship.getOwner()) {
				allyRisk += weight;
			} else {
				enemyScore += weight * enemyVulnerability(candidate);
			}
		}
		if (distanceSquared(center, ship.getLocation()) <= Meng_MingGodSpaceCutPlugin.SPACE_RADIUS * Meng_MingGodSpaceCutPlugin.SPACE_RADIUS) {
			allyRisk += 1f;
		}
		return enemyScore - allyRisk * 0.8f;
	}

	private int[] chooseBestSwap(Vector2f center, int[] map) {
		float[] enemyValue = new float[SECTOR_COUNT];
		float[] allyRisk = new float[SECTOR_COUNT];
		buildSectorValues(center, enemyValue, allyRisk);

		float currentScore = scoreArrangement(center, map, enemyValue, allyRisk);
		float bestScore = currentScore + MIN_SWAP_SCORE;
		int[] bestSwap = null;
		for (int first = 0; first < SECTOR_COUNT - 1; first++) {
			for (int second = first + 1; second < SECTOR_COUNT; second++) {
				int[] candidate = map.clone();
				int value = candidate[first];
				candidate[first] = candidate[second];
				candidate[second] = value;
				float score = scoreArrangement(center, candidate, enemyValue, allyRisk);
				if (score <= bestScore) continue;
				bestScore = score;
				bestSwap = new int[]{first, second};
			}
		}
		return bestSwap;
	}

	private void buildSectorValues(Vector2f center, float[] enemyValue, float[] allyRisk) {
		for (ShipAPI candidate : engine.getShips()) {
			if (!candidate.isAlive()) continue;
			Vector2f offset = Vector2f.sub(candidate.getLocation(), center, new Vector2f());
			if (offset.lengthSquared() > Meng_MingGodSpaceCutPlugin.SPACE_RADIUS * Meng_MingGodSpaceCutPlugin.SPACE_RADIUS) continue;

			int sector = getSector(offset);
			float weight = shipWeight(candidate);
			if (candidate.getOwner() == ship.getOwner()) {
				allyRisk[sector] += weight;
			} else {
				enemyValue[sector] += weight * enemyVulnerability(candidate);
			}
		}
	}

	private float scoreArrangement(Vector2f center, int[] map, float[] enemyValue, float[] allyRisk) {
		float score = 0f;
		for (int sector = 0; sector < SECTOR_COUNT; sector++) {
			int previous = (sector + SECTOR_COUNT - 1) % SECTOR_COUNT;
			int sourceSector = map[sector];
			int previousSourceSector = map[previous];
			if (sourceSector == (previousSourceSector + 1) % SECTOR_COUNT) continue;

			float enemyAtBoundary = enemyValue[sourceSector] + enemyValue[previousSourceSector];
			score += enemyAtBoundary * 2.5f;
		}
		for (int destination = 0; destination < SECTOR_COUNT; destination++) {
			int sourceSector = map[destination];
			float enemyAtDestination = enemyValue[sourceSector];
			score += enemyAtDestination * forwardPressure(destination) * 0.45f;
		}
		return score + scoreProjectedPositions(center, map);
	}

	private float scoreProjectedPositions(Vector2f center, int[] map) {
		ArrayList<ShipAPI> ships = new ArrayList<ShipAPI>();
		for (ShipAPI candidate : engine.getShips()) {
			if (!candidate.isAlive()) continue;
			if (candidate.isFighter() || candidate.isDrone()) continue;
			ships.add(candidate);
		}

		Vector2f projectedSource = projectLocation(ship.getLocation(), center, map);
		float score = 0f;
		for (ShipAPI candidate : ships) {
			Vector2f projected = projectLocation(candidate.getLocation(), center, map);
			if (candidate.getOwner() != ship.getOwner()) {
				float distance = (float) Math.sqrt(distanceSquared(projected, projectedSource));
				float facingScore = forwardPressure(projected, projectedSource);
				score += shipWeight(candidate) * enemyVulnerability(candidate) * (3.5f - Math.min(3f, distance / 1000f));
				score += shipWeight(candidate) * facingScore * 1.2f;
				continue;
			}
			if (candidate == ship) continue;

			float nearestEnemyDistance = 6000f;
			for (ShipAPI enemy : ships) {
				if (enemy.getOwner() == ship.getOwner()) continue;
				Vector2f projectedEnemy = projectLocation(enemy.getLocation(), center, map);
				nearestEnemyDistance = Math.min(nearestEnemyDistance,
						(float) Math.sqrt(distanceSquared(projected, projectedEnemy)));
			}
			score += shipWeight(candidate) * Math.min(6f, nearestEnemyDistance / 1000f) * 1.6f;
		}
		return score;
	}

	private Vector2f projectLocation(Vector2f position, Vector2f center, int[] map) {
		Vector2f offset = Vector2f.sub(position, center, new Vector2f());
		if (offset.lengthSquared() > Meng_MingGodSpaceCutPlugin.SPACE_RADIUS * Meng_MingGodSpaceCutPlugin.SPACE_RADIUS) {
			return new Vector2f(position);
		}
		int sourceSector = getSector(offset);
		int destinationSector = getDestinationSector(sourceSector, map);
		float rotation = (destinationSector - sourceSector) * SECTOR_ANGLE;
		float cosine = (float) Math.cos(rotation);
		float sine = (float) Math.sin(rotation);
		return new Vector2f(center.x + offset.x * cosine - offset.y * sine,
				center.y + offset.x * sine + offset.y * cosine);
	}

	private int getDestinationSector(int sourceSector, int[] map) {
		for (int destination = 0; destination < SECTOR_COUNT; destination++) {
			if (map[destination] == sourceSector) return destination;
		}
		return sourceSector;
	}

	private float forwardPressure(int destinationSector) {
		float angle = destinationSector * SECTOR_ANGLE + SECTOR_ANGLE * 0.5f;
		float facing = (float) Math.toRadians(ship.getFacing());
		return Math.max(0f, (float) Math.cos(angle - facing));
	}

	private float forwardPressure(Vector2f position, Vector2f sourcePosition) {
		float angle = (float) Math.atan2(position.y - sourcePosition.y, position.x - sourcePosition.x);
		float facing = (float) Math.toRadians(ship.getFacing());
		return Math.max(0f, (float) Math.cos(angle - facing));
	}

	private int getSector(Vector2f offset) {
		float angle = (float) Math.atan2(offset.y, offset.x);
		if (angle < 0f) angle += (float) (Math.PI * 2d);
		return Math.min(SECTOR_COUNT - 1, (int) (angle / SECTOR_ANGLE));
	}

	private float shipWeight(ShipAPI candidate) {
		if (candidate.isFighter() || candidate.isDrone()) return 0.15f;
		if (candidate.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) return 8f;
		if (candidate.getHullSize() == ShipAPI.HullSize.CRUISER) return 5f;
		if (candidate.getHullSize() == ShipAPI.HullSize.DESTROYER) return 2.5f;
		if (candidate.getHullSize() == ShipAPI.HullSize.FRIGATE) return 1f;
		return 0.25f;
	}

	private float enemyVulnerability(ShipAPI candidate) {
		float hullPressure = 1f + (1f - candidate.getHullLevel()) * 1.5f;
		return hullPressure + candidate.getFluxTracker().getFluxLevel() * 0.75f;
	}

	private float distanceSquared(Vector2f first, Vector2f second) {
		float x = first.x - second.x;
		float y = first.y - second.y;
		return x * x + y * y;
	}
}
