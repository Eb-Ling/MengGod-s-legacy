package data.methods;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * CatmullRomSpline - Catmull-Rom 样条曲线插值工具。
 *
 * <h3>功能概述</h3>
 * 提供基于 Catmull-Rom 基函数的平滑曲线插值：
 * <ul>
 *   <li>通过 {@link #insert(Vector2f)} 或 {@link #insert(Vector3f)} 动态添加控制点</li>
 *   <li>通过 {@link #evaluate(float)} 获取曲线上任意参数 t 处的插值点</li>
 *   <li>t=0 对应第一个控制点，t=1 对应最后一个控制点</li>
 *   <li>曲线经过所有控制点（非逼近，是精确插值）</li>
 *   <li>端点处使用 clamped 模式（虚拟控制点 = 最近端点）</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 *   CatmullRomSpline spline = new CatmullRomSpline();
 *   spline.insert(new Vector2f(0f, 0f));
 *   spline.insert(new Vector2f(100f, 50f));
 *   spline.insert(new Vector2f(200f, 0f));
 *
 *   // 沿曲线均匀采样 20 个点
 *   for (int i = 0; i &lt; 20; i++) {
 *       float t = (float) i / 19f;
 *       Vector2f p = spline.evaluate(t, null);
 *   }
 * </pre>
 */
public class CatmullRomSpline {

    /** 2D 控制点列表 */
    private final List<Vector2f> points2D = new ArrayList<>();
    /** 3D 控制点列表 */
    private final List<Vector3f> points3D = new ArrayList<>();

    // ==================== 插入控制点 ====================

    /**
     * 在末尾追加一个 2D 控制点。
     * 至少需要 2 个控制点才能进行插值。
     *
     * @param point 控制点坐标
     */
    public void insert(Vector2f point) {
        points2D.add(new Vector2f(point));
    }

    /**
     * 在末尾追加一个 3D 控制点。
     * 至少需要 2 个控制点才能进行插值。
     *
     * @param point 控制点坐标
     */
    public void insert(Vector3f point) {
        points3D.add(new Vector3f(point));
    }

    /** 清空所有控制点 */
    public void clear() {
        points2D.clear();
        points3D.clear();
    }

    /** 获取 2D 控制点数量 */
    public int size2D() { return points2D.size(); }

    /** 获取 3D 控制点数量 */
    public int size3D() { return points3D.size(); }

    // ==================== 2D 插值 ====================

    /**
     * 在 Catmull-Rom 曲线上对 2D 控制点进行插值。
     * t=0 返回第一个控制点，t=1 返回最后一个控制点，中间值平滑经过所有控制点。
     *
     * @param t    曲线参数，通常 [0, 1]，超出范围时 clamped 到端点
     * @param out  输出向量（可复用以减少GC），null 时自动创建
     * @return 插值结果
     */
    public Vector2f evaluate(float t, Vector2f out) {
        if (out == null) out = new Vector2f();
        int n = points2D.size();
        if (n == 0) { out.set(0f, 0f); return out; }
        if (n == 1) { out.set(points2D.get(0)); return out; }

        t = Math.max(0f, Math.min(1f, t));
        float scaledT = t * (n - 1);
        int seg = Math.min((int) scaledT, n - 2);
        float localT = scaledT - seg;

        Vector2f p0 = clamped2D(seg - 1, n);
        Vector2f p1 = clamped2D(seg, n);
        Vector2f p2 = clamped2D(seg + 1, n);
        Vector2f p3 = clamped2D(seg + 2, n);

        out.x = catmullRom1D(localT, p0.x, p1.x, p2.x, p3.x);
        out.y = catmullRom1D(localT, p0.y, p1.y, p2.y, p3.y);
        return out;
    }

    // ==================== 3D 插值 ====================

    /**
     * 在 Catmull-Rom 曲线上对 3D 控制点进行插值。
     * t=0 返回第一个控制点，t=1 返回最后一个控制点，中间值平滑经过所有控制点。
     *
     * @param t    曲线参数，通常 [0, 1]，超出范围时 clamped 到端点
     * @param out  输出向量（可复用以减少GC），null 时自动创建
     * @return 插值结果
     */
    public Vector3f evaluate(float t, Vector3f out) {
        if (out == null) out = new Vector3f();
        int n = points3D.size();
        if (n == 0) { out.set(0f, 0f, 0f); return out; }
        if (n == 1) { out.set(points3D.get(0)); return out; }

        t = Math.max(0f, Math.min(1f, t));
        float scaledT = t * (n - 1);
        int seg = Math.min((int) scaledT, n - 2);
        float localT = scaledT - seg;

        Vector3f p0 = clamped3D(seg - 1, n);
        Vector3f p1 = clamped3D(seg, n);
        Vector3f p2 = clamped3D(seg + 1, n);
        Vector3f p3 = clamped3D(seg + 2, n);

        out.x = catmullRom1D(localT, p0.x, p1.x, p2.x, p3.x);
        out.y = catmullRom1D(localT, p0.y, p1.y, p2.y, p3.y);
        out.z = catmullRom1D(localT, p0.z, p1.z, p2.z, p3.z);
        return out;
    }

    // ==================== 内部方法 ====================

    /** Clamped 索引：越界时返回最近端点 */
    private Vector2f clamped2D(int index, int n) {
        return points2D.get(Math.max(0, Math.min(n - 1, index)));
    }

    private Vector3f clamped3D(int index, int n) {
        return points3D.get(Math.max(0, Math.min(n - 1, index)));
    }

    /**
     * 单变量 Catmull-Rom 插值（标准基函数）。
     * q(t) = 0.5 * [(2*P1) + (-P0+P2)*t + (2*P0-5*P1+4*P2-P3)*t^2 + (-P0+3*P1-3*P2+P3)*t^3]
     */
    private static float catmullRom1D(float t, float p0, float p1, float p2, float p3) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5f * (
                (2f * p1) +
                (-p0 + p2) * t +
                (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
                (-p0 + 3f * p1 - 3f * p2 + p3) * t3
        );
    }
}
