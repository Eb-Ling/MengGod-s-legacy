package data.methods;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/**
 * 飞船移动控制工具类（data.methods.ShipMoveUtil）。
 * <p>
 * 复刻原版 {@code BasicEngineAI.establishHeading()} 的移动控制逻辑，
 * 仅使用公开 API（{@link ShipCommand} + {@link ShipAPI#giveCommand}）驱动飞船，
 * 使飞船以与原版 AI 相同的方式飞向指定位置。
 * <p>
 * 核心流程：
 * <ol>
 *   <li>计算当前速度方向与目标方向的偏差</li>
 *   <li>计算船头朝向与目标方向的偏差</li>
 *   <li>根据偏差分别发出 转向 / 平移 / 加速 / 减速 指令</li>
 * </ol>
 * <p>
 * 调用方式：在每帧的 advance 中调用 {@link #moveToLocation(ShipAPI, Vector2f, float)}。
 */
public class ShipMoveUtil {

    // ======================== 常量 ========================

    /**
     * 到达判定距离（SU），小于此距离视为到达目标
     */
    private static final float ARRIVAL_DISTANCE = 18f;

    /**
     * 速度方向偏差阈值（度），超过此值时执行平移修正
     */
    private static final float VELOCITY_DIFF_THRESHOLD = 1f;

    /**
     * 侧向平移触发角度（度），目标在船侧方 55° 以内时使用平移
     */
    private static final float STRAFE_ANGLE_THRESHOLD = 55f;

    /**
     * 倒车触发角度（度），目标在船尾 135°~180° 范围时使用倒车加速
     */
    private static final float REVERSE_ANGLE = 135f;

    /**
     * 前向加速触发角度（度），目标在船头 45°~135° 范围且避障模式时使用前向加速
     */
    private static final float FORWARD_ACCEL_ANGLE = 45f;

    // ======================== 核心方法 ========================

    /**
     * 驱动飞船飞向指定位置（复刻原版 BasicEngineAI 逻辑）。
     * <p>
     * 每帧调用一次。飞船会自动转向、加速、平移修正，
     * 到达目标附近后减速停止。
     *
     * @param ship     目标飞船
     * @param target   目标位置（世界坐标）
     * @param speedFraction 期望速度比例（0~1），1 为全速
     */
    public static void moveToLocation(ShipAPI ship, Vector2f target, float speedFraction) {
        if (ship == null || target == null) return;

        float distance = Misc.getDistance(ship.getLocation(), target);
        if (distance <= ARRIVAL_DISTANCE) {
            ship.giveCommand(ShipCommand.DECELERATE, null, 0);
            return;
        }

        float desiredHeading = Misc.getAngleInDegrees(ship.getLocation(), target);
        establishHeading(ship, desiredHeading, speedFraction);
    }

    /**
     * 驱动飞船朝指定角度方向移动（复刻原版 BasicEngineAI 逻辑）。
     * <p>
     * 与 {@link #moveToLocation} 不同，此方法直接指定移动方向角度而非目标坐标。
     *
     * @param ship          目标飞船
     * @param heading       期望移动方向（度，0=正右，逆时针递增）
     * @param speedFraction 期望速度比例（0~1）
     */
    public static void moveInHeading(ShipAPI ship, float heading, float speedFraction) {
        if (ship == null) return;
        establishHeading(ship, heading, speedFraction);
    }

    /**
     * 原地转向指定角度（不驱动移动，仅转向）。
     *
     * @param ship    目标飞船
     * @param facing  期望朝向（度）
     */
    public static void turnToFacing(ShipAPI ship, float facing) {
        if (ship == null) return;
        turnTowards(ship, facing);
    }

    // ======================== 内部实现（复刻原版 establishHeading） ========================

    /**
     * 复刻原版 {@code BasicEngineAI.establishHeading(heading, speedFraction, amount, true)} 逻辑。
     * <p>
     * 根据当前速度方向、船头朝向与目标方向的偏差，
     * 发出 ACCELERATE / DECELERATE / STRAFE_LEFT / STRAFE_RIGHT / TURN_LEFT / TURN_RIGHT 指令。
     *
     * @param ship          目标飞船
     * @param desiredHeading 期望移动方向（度）
     * @param speedFraction  期望速度比例（0~1）
     */
    private static void establishHeading(ShipAPI ship, float desiredHeading, float speedFraction) {
        // 1. 当前速度方向
        Vector2f vel = ship.getVelocity();
        float velAngle = getVectorAngle(vel);

        // 2. 速度方向与目标方向的偏差
        float velDiff = Misc.getAngleDiff(velAngle, desiredHeading);

        // 3. 是否已停止
        boolean isStopped = vel.length() <= 0f;

        // 4. 船头朝向与目标方向的偏差
        float facingDiff = Misc.getAngleDiff(ship.getFacing(), desiredHeading);

        // 5. 速度变化方向：1=需加速, -1=需减速, 0=无需变化
        float speedChangeDir = getSpeedChangeDir(ship, speedFraction);

        // 如果已停止或速度方向偏差很小且无需变速，则只做转向
        if (!isStopped && velDiff > VELOCITY_DIFF_THRESHOLD && speedChangeDir != 0f) {
            boolean backing = false;

            // 避障场景：目标在侧方 45°~135° 时，向前加速
            if (facingDiff > FORWARD_ACCEL_ANGLE && facingDiff < REVERSE_ANGLE) {
                ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
            } else if (facingDiff > REVERSE_ANGLE) {
                // 目标在船尾方向：倒车加速
                ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
                backing = true;
            }

            // 计算实际用于转向的目标角度
            float turnTarget = desiredHeading;
            if (backing) {
                turnTarget += 180f;
            }

            // 侧向平移修正：目标在船侧方时平移
            float rightDiff = Misc.getAngleDiff(desiredHeading, ship.getFacing() + 90f);
            if (rightDiff <= STRAFE_ANGLE_THRESHOLD) {
                ship.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
                if (backing) {
                    turnTarget -= 90f;
                }
            } else {
                float leftDiff = Misc.getAngleDiff(desiredHeading, ship.getFacing() - 90f);
                if (leftDiff <= STRAFE_ANGLE_THRESHOLD) {
                    ship.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
                    if (backing) {
                        turnTarget += 90f;
                    }
                }
            }

            turnTowards(ship, turnTarget);
        } else {
            // 速度方向已对齐或已停止，只需转向目标方向
            turnTowards(ship, desiredHeading);
        }
    }

    /**
     * 复刻原版 {@code BasicEngineAI.turnTowards(heading, amount)} 逻辑。
     * <p>
     * 根据船头朝向与目标角度的偏差，发出 TURN_LEFT 或 TURN_RIGHT 指令。
     *
     * @param ship          目标飞船
     * @param targetFacing  目标朝向（度）
     */
    private static void turnTowards(ShipAPI ship, float targetFacing) {
        float angularVel = ship.getAngularVelocity();
        float facingDiff = Misc.getAngleDiff(ship.getFacing(), targetFacing);

        // 考虑角速度惯性：如果已经在转向且方向正确，不需要额外指令
        if (Math.abs(angularVel) > 0f) {
            float turnDir = Math.signum(angularVel);
            float diffSign = Math.signum(facingDiff);
            // 角速度方向与需要的转向方向一致，跳过
            if (turnDir == diffSign && Math.abs(facingDiff) < 1f) {
                return;
            }
        }

        // 发出转向指令
        if (facingDiff > 0f) {
            ship.giveCommand(ShipCommand.TURN_LEFT, null, 0);
        } else if (facingDiff < 0f) {
            ship.giveCommand(ShipCommand.TURN_RIGHT, null, 0);
        }
    }

    /**
     * 复刻原版 {@code BasicEngineAI.getSpeedChangeDir(speedFraction)} 逻辑。
     * <p>
     * 根据当前速度与期望速度比例的差值，返回速度变化方向。
     *
     * @param ship          目标飞船
     * @param speedFraction 期望速度比例（0~1）
     * @return 1=需加速, -1=需减速, 0=速度已匹配
     */
    private static float getSpeedChangeDir(ShipAPI ship, float speedFraction) {
        float currentSpeedRatio = ship.getVelocity().length() / ship.getMaxSpeed();
        if (currentSpeedRatio - 0.01f > speedFraction) {
            return -1f; // 当前速度超过期望，需减速
        }
        if (currentSpeedRatio + 0.01f < speedFraction) {
            return 1f;  // 当前速度低于期望，需加速
        }
        return 0f;      // 速度已匹配
    }

    /**
     * 获取向量的角度（度，Starsector 坐标系：0=正右，逆时针递增）。
     *
     * @param v 向量
     * @return 角度（度）
     */
    private static float getVectorAngle(Vector2f v) {
        if (v == null || (v.x == 0f && v.y == 0f)) return 0f;
        return (float) Math.toDegrees(Math.atan2(v.y, v.x));
    }
}
