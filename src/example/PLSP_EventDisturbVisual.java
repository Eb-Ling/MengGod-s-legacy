package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.plsp.util.PLSP_BoxBasedUtils;
import data.scripts.plsp.util.PLSP_ColorData;
import data.scripts.plsp.util.PLSP_Utils;
import org.boxutil.manager.ShaderCore;
import org.boxutil.base.api.InstanceDataAPI;
import org.boxutil.units.standard.ShaderProgram;
import org.boxutil.units.standard.entity.CurveEntity;
import org.boxutil.util.ShaderUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PLSP_EventDisturbVisual extends BaseCombatLayeredRenderingPlugin {

	public static final Vector2f ZERO = new Vector2f();
	public static final Color CURVE_TO_TARGET_START = new Color(123, 255, 231);
	public static final Color CURVE_TO_TARGET_END = new Color(0, 128, 255);
	public static final Color REMOVER_GLOW = new Color(158, 255, 237, 150);

	public static final float DURATION_IN = 0.5f;
	public static final float DURATION_OUT = 0.5f;
	private static final int FEEDBACK_EVENT_FLOATS = 4;
	private static final int FEEDBACK_SSBO_BINDING = 1;
	private static final float EDGE_WIDTH_FACTOR = 4f;
	private static final Color FIELD_CORE = new Color(173, 252, 255);
	private static final Color FIELD_EDGE = new Color(137, 164, 255);

	private final ShipAPI source;
	private final ShipAPI anchor;
	private final float maxTime;

	private float elapsed;
	private final float elapsedShift;

	private CurveEntity curveToTarget = null;

	private CombatEngineAPI engine;
	private boolean valid = true;

	private boolean fadeOutSound = false;
	private float aliveLevel = 1f;
	private final List<FeedbackEvent> feedbackEvents = new ArrayList<>();

	public ShipAPI getAnchor() {
		return anchor;
	}

	public float getEffectLevel() {
		float effectLevel;
		if (elapsed < DURATION_IN) effectLevel = elapsed / DURATION_IN;
		else if (elapsed > maxTime - DURATION_OUT) effectLevel = (maxTime - elapsed) / (DURATION_OUT);
		else effectLevel = 1f;
		return Math.max(effectLevel, 0f);
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean isValid() {
		return valid;
	}

	public PLSP_EventDisturbVisual(ShipAPI source, ShipAPI anchor, float maxTime) {
		this.source = source;
		this.anchor = anchor;
		this.maxTime = maxTime + DURATION_IN + DURATION_OUT;
		this.elapsed = 0f;
		this.elapsedShift = MathUtils.getRandomNumberInRange(0f, 100f);

		this.anchor.setCustomData(PLSP_EventDisturbStats.DATA_KEY, true);

		if (source != anchor) {
			float widthStart = source.getCollisionRadius() * 4f;
			float widthEnd = anchor.getCollisionRadius() * 4f;
			float textureSpeed = 400f;
			curveToTarget = PLSP_BoxBasedUtils.createCurveLine(Global.getSettings().getSprite("misc", "PLSP_linkBand"), CURVE_TO_TARGET_START, CURVE_TO_TARGET_END, null, PLSP_ColorData.NONE, PLSP_ColorData.NONE, widthStart, widthEnd, textureSpeed, (byte)64, CombatEngineLayers.BELOW_SHIPS_LAYER);
			curveToTarget.setFillStartFactor(0.5f);
			curveToTarget.setFillEndFactor(0.05f);
			PLSP_BoxBasedUtils.startCurveLine(curveToTarget, 0.2f);
		}
	}

	@Override
	public float getRenderRadius() {
		return PLSP_EventDisturbStats.getRange(source) + getActiveRange(anchor) + 1000f;
	}

	public float getActiveRange(ShipAPI anchor) {
		return anchor.getCollisionRadius() + 500f;
	}

	@Override
	public void advance(float amount) {

		if (engine == null) return;
		if (engine.isPaused()) return;

		if (!engine.isEntityInPlay(anchor) || !engine.isEntityInPlay(source)) {
			valid = false;
			if (!fadeOutSound) {
				fadeOutSound = true;
				Global.getSoundPlayer().playSound("PLSP_event_disturb_deactive", 1f, 1f, source.getLocation(), ZERO);
			}
			return;
		}

		elapsed += amount;
		entity.getLocation().set(anchor.getLocation());
		advanceFeedbackEvents(amount);

		if (!anchor.isAlive() || !source.isAlive()) {
			aliveLevel -= amount * 3f;
			if (aliveLevel <= 0f) {
				aliveLevel = 0f;
				valid = false;
			}

			if (!fadeOutSound) {
				fadeOutSound = true;
				Global.getSoundPlayer().playSound("PLSP_event_disturb_deactive", 1f, 1f, source.getLocation(), ZERO);
			}

			return;
		}

		float effectLevel = getEffectLevel();
		if (effectLevel <= 0f) {
			valid = false;
			if (!fadeOutSound) {
				fadeOutSound = true;
				Global.getSoundPlayer().playSound("PLSP_event_disturb_deactive", 1f, 1f, source.getLocation(), ZERO);
			}
			return;
		}

		if (elapsed > maxTime - DURATION_OUT) {
			if (!fadeOutSound) {
				fadeOutSound = true;
				Global.getSoundPlayer().playSound("PLSP_event_disturb_deactive", 1f, 1f, source.getLocation(), ZERO);
			}
		}

		Global.getSoundPlayer().playLoop("PLSP_event_disturb_loop", source, 1f, 1f, anchor.getLocation(), ZERO);

		if (source != anchor) {
			source.setJitterUnder(this, CURVE_TO_TARGET_START, effectLevel, 5, 0f, 14f);
		}

		if (curveToTarget != null) {
			PLSP_BoxBasedUtils.updateCurveLine(curveToTarget, source.getLocation(), anchor.getLocation());

			float alphaMult = aliveLevel * effectLevel;
			curveToTarget.getMaterialData().setColorAlpha(alphaMult);
		}

		if (effectLevel >= 1f) {

			for (DamagingProjectileAPI proj : PLSP_Utils.getEnemyProjectilesAndMissilesWithinRange(anchor.getLocation(), getActiveRange(anchor), anchor.getOwner())) {

				if (PLSP_Utils.willProjectileHitShipWithInSec(proj, anchor, 3)) {

					// visual
					if (PLSP_Utils.shouldApplyVisual(proj)) {

						float adjustedStrength = PLSP_Utils.getProjectileStrength(proj) / 400f;
						float clampStrength = Math.max(0.1f, Math.min(1f, adjustedStrength));
						float duration = 0.5f + clampStrength * 0.5f;

						float visualSize = proj.getCollisionRadius() + 20f;

						float angleDiff = VectorUtils.getAngle(anchor.getLocation(), proj.getLocation());
						float[] angle = new float[]{angleDiff - 10f, angleDiff + 10f};
						float[] speed = new float[]{30f, 70f};
						float[] size = new float[]{visualSize, visualSize * 2f};
						float time = duration * 0.5f;
						List<InstanceDataAPI> list = PLSP_BoxBasedUtils.createBurstOutInstanceData(3, null, null, angle, speed, size, null, null, 0f, time);
						PLSP_BoxBasedUtils.addSmoothParticle(list, proj.getLocation(), 1f, 1f, 0f, time, REMOVER_GLOW);

						if (!MathUtils.isWithinRange(proj, anchor.getLocation(), anchor.getCollisionRadius())) {
							float innerHitDistance = getActiveRange(anchor) - MathUtils.getDistance(anchor.getLocation(), proj.getLocation());
							addFeedbackEvent(angleDiff, clampStrength, duration, innerHitDistance);
						}

						float volume = Math.min(1f, adjustedStrength);
						Global.getSoundPlayer().playSound("PLSP_event_disturb_trigger", 1f, volume, proj.getLocation(), ZERO);
					}

					engine.removeEntity(proj);
				}
			}
		}
	}

	@Override
	public void init(CombatEntityAPI entity) {
		super.init(entity);

		engine = Global.getCombatEngine();
		layer = CombatEngineLayers.UNDER_SHIPS_LAYER;
		advance(0f);

		int programId = ShaderUtil.createShaderVFFormPath(this.getClass().getName(),
				"data/shaders/PLSP_event_disturb_field.vert",
				"data/shaders/PLSP_event_disturb_field.frag");
		program = new ShaderProgram(programId);
		program.location = new int[] {
				program.getUniformIndex("modelMatrix"),
				program.getUniformIndex("size"),
				program.getUniformIndex("time"),
				program.getUniformIndex("edgeWidth"),
				program.getUniformIndex("coreColor"),
				program.getUniformIndex("edgeColor"),
				program.getUniformIndex("feedbackEventCount")
		};
		program.uboLocation = new int[] {
				program.getUBOIndex("BUtilGlobalData", ShaderCore.getMatrixUBOBinding())
		};

		int[] idV = PLSP_BoxBasedUtils.createUniversalRectVAO();
		vaoId = idV[0];
		vboId = idV[1];
		feedbackSsboId = GL15.glGenBuffers();
	}

	@Override
	public void cleanup() {
		anchor.removeCustomData(PLSP_EventDisturbStats.DATA_KEY);

		if (curveToTarget != null && !curveToTarget.hasDelete()) {
			curveToTarget.delete();
		}
		if (vaoId > 0) {
			GL30.glDeleteVertexArrays(vaoId);
			vaoId = 0;
		}
		if (vboId > 0) {
			GL15.glDeleteBuffers(vboId);
			vboId = 0;
		}
		if (program != null) {
			program.delete();
			program = null;
		}
		if (feedbackSsboId > 0) {
			GL15.glDeleteBuffers(feedbackSsboId);
			feedbackSsboId = 0;
		}
	}

	@Override
	public boolean isExpired() {
		return !valid;
	}

	@Override
	public void render(CombatEngineLayers layer, ViewportAPI viewport) {

		float effectLevel = getEffectLevel();
		float alphaMult = viewport.getAlphaMult() * anchor.getAlphaMult() * aliveLevel * effectLevel;
		if (alphaMult <= 0f) return;

		float radius = getActiveRange(anchor) * 2f;
		renderField(radius * 1.25f, alphaMult);
	}

	private int vaoId = -99;
	private int vboId = -99;
	private int feedbackSsboId = -99;
	private int feedbackSsboCapacity = 0;
	private int feedbackUploadBufferCapacity = 0;
	private FloatBuffer feedbackUploadBuffer = null;
	private ShaderProgram program = null;
	private FloatBuffer matrixBuffer = null;

	private void addFeedbackEvent(float angle, float strength, float duration, float innerHitDistance) {
		feedbackEvents.add(new FeedbackEvent((float)Math.toRadians(MathUtils.clampAngle(angle)), strength, duration, innerHitDistance));
	}

	private void advanceFeedbackEvents(float amount) {
		Iterator<FeedbackEvent> iter = feedbackEvents.iterator();
		while (iter.hasNext()) {
			FeedbackEvent event = iter.next();
			event.age += amount;
			if (event.age >= event.duration) {
				iter.remove();
			}
		}
	}

	private void renderField(float radius, float alphaMult) {

		if (program == null) return;
		if (vaoId <= 0) return;
		if (vboId <= 0) return;
		if (feedbackSsboId <= 0) return;

		if (radius <= 0f) return;

		uploadFeedbackEvents();

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

		GL30.glBindVertexArray(vaoId);
		program.active();

		matrixBuffer = PLSP_BoxBasedUtils.maintainAnchoredFloatBuffer(anchor.getLocation(), 0f, matrixBuffer);

		GL20.glUniformMatrix4(program.location[0], false, matrixBuffer);
		GL20.glUniform2f(program.location[1], radius, radius);
		GL20.glUniform1f(program.location[2], elapsed + elapsedShift);
		GL20.glUniform1f(program.location[3], EDGE_WIDTH_FACTOR);
		GL20.glUniform4f(program.location[4], FIELD_CORE.getRed() / 255f, FIELD_CORE.getGreen() / 255f, FIELD_CORE.getBlue() / 255f, alphaMult * FIELD_CORE.getAlpha() / 255f);
		GL20.glUniform4f(program.location[5], FIELD_EDGE.getRed() / 255f, FIELD_EDGE.getGreen() / 255f, FIELD_EDGE.getBlue() / 255f, alphaMult * FIELD_EDGE.getAlpha() / 255f);
		GL20.glUniform1i(program.location[6], feedbackEvents.size());

		GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

		program.close();
		GL30.glBindVertexArray(0);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, FEEDBACK_SSBO_BINDING, 0);
	}

	private void uploadFeedbackEvents() {
		int eventCount = feedbackEvents.size();
		int floatCount = Math.max(FEEDBACK_EVENT_FLOATS, eventCount * FEEDBACK_EVENT_FLOATS);
		if (feedbackUploadBuffer == null || feedbackUploadBufferCapacity < floatCount) {
			feedbackUploadBuffer = BufferUtils.createFloatBuffer(floatCount);
			feedbackUploadBufferCapacity = floatCount;
		}

		feedbackUploadBuffer.clear();
		for (FeedbackEvent event : feedbackEvents) {
			feedbackUploadBuffer.put(event.angle);
			feedbackUploadBuffer.put(event.age / event.duration);
			feedbackUploadBuffer.put(event.strength);
			feedbackUploadBuffer.put(event.innerHitDistance);
		}
		if (eventCount <= 0) {
			feedbackUploadBuffer.put(0f);
			feedbackUploadBuffer.put(0f);
			feedbackUploadBuffer.put(0f);
			feedbackUploadBuffer.put(0f);
		}
		feedbackUploadBuffer.flip();

		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, feedbackSsboId);
		if (feedbackSsboCapacity < floatCount) {
			GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, feedbackUploadBuffer, GL15.GL_STREAM_DRAW);
			feedbackSsboCapacity = floatCount;
		} else {
			GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, feedbackUploadBuffer);
		}
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, FEEDBACK_SSBO_BINDING, feedbackSsboId);
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
	}

	private static class FeedbackEvent {
		private final float angle;
		private final float strength;
		private final float duration;
		private final float innerHitDistance;
		private float age = 0f;

		private FeedbackEvent(float angle, float strength, float duration, float innerHitDistance) {
			this.angle = angle;
			this.strength = strength;
			this.duration = duration;
			this.innerHitDistance = innerHitDistance;
		}
	}
}
