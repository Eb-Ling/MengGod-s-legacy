package data;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector2f;

import java.nio.ByteBuffer;

public class KabbalahLifeTreeEffect implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect, MyRenderTool.MouseAwareEffect {
    private final Vector2f position = new Vector2f(0f, 0f);
    private final TreeNode[] nodes;

    private float elapsed;
    private boolean expired;
    private float canvasWidth = 900f;
    private float canvasHeight = 560f;
    private float mouseX = Float.NaN;
    private float mouseY = Float.NaN;
    private float hoverBurst;
    private int hoveredNode = -1;

    private int fboId;
    private int fboTexId;
    private int fboW;
    private int fboH;
    private int branchShader;
    private int uTimeLoc;
    private int uCanvasLoc;

    private static final String VERT_SRC =
            "#version 110\n" +
            "varying vec2 v_uv;\n" +
            "void main() {\n" +
            "    v_uv = gl_MultiTexCoord0.xy;\n" +
            "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
            "}\n";

    private static final String FRAG_SRC =
            "#version 110\n" +
            "varying vec2 v_uv;\n" +
            "uniform float u_time;\n" +
            "uniform vec2 u_canvas;\n" +
            "\n" +
            "vec2 cubic(vec2 p0, vec2 p1, vec2 p2, vec2 p3, float t) {\n" +
            "    float u = 1.0 - t;\n" +
            "    return u*u*u*p0 + 3.0*u*u*t*p1 + 3.0*u*t*t*p2 + t*t*t*p3;\n" +
            "}\n" +
            "\n" +
            "float segDist(vec2 p, vec2 a, vec2 b) {\n" +
            "    vec2 pa = p - a;\n" +
            "    vec2 ba = b - a;\n" +
            "    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001), 0.0, 1.0);\n" +
            "    return length(pa - ba * h);\n" +
            "}\n" +
            "\n" +
            "void nearestBranch(vec2 p, vec2 p0, vec2 p1, vec2 p2, vec2 p3, float startTime, float endTime, float globalStart, float globalEnd, inout float nearest, inout float nearestWidth) {\n" +
            "    float grow = smoothstep(0.0, 1.0, clamp((u_time - startTime) / (endTime - startTime), 0.0, 1.0));\n" +
            "    if (grow <= 0.0) return;\n" +
            "    vec2 prev = p0;\n" +
            "    for (int i = 1; i <= 72; i++) {\n" +
            "        float t = grow * float(i) / 72.0;\n" +
            "        vec2 cur = cubic(p0, p1, p2, p3, t);\n" +
            "        float d = segDist(p, prev, cur);\n" +
            "        if (d < nearest) {\n" +
            "            float globalT = mix(globalStart, globalEnd, t);\n" +
            "            nearest = d;\n" +
            "            nearestWidth = mix(18.0, 3.4, globalT);\n" +
            "        }\n" +
            "        prev = cur;\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "void nearestSplitBranch(vec2 p, vec2 parentPoint, vec2 p0, vec2 p3, float startTime, float endTime, float globalStart, float globalEnd, inout float nearest, inout float nearestWidth) {\n" +
            "    vec2 dir = normalize(p0 - parentPoint);\n" +
            "    float extension = clamp(length(p3 - p0) * 0.26, 42.0, 78.0);\n" +
            "    vec2 p1 = p0 + dir * extension;\n" +
            "    vec2 p2 = mix(p1, p3, 0.62);\n" +
            "    nearestBranch(p, p0, p1, p2, p3, startTime, endTime, globalStart, globalEnd, nearest, nearestWidth);\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 p = vec2((v_uv.x - 0.5) * u_canvas.x, (v_uv.y - 0.5) * u_canvas.y);\n" +
            "    float nearest = 99999.0;\n" +
            "    float width = 0.0;\n" +
            "    nearestBranch(p, vec2(0.0, -245.0), vec2(0.0, -205.0), vec2(0.0, -152.0), vec2(0.0, -110.0), 0.0, 1.45, 0.00, 0.28, nearest, width);\n" +
            "    nearestSplitBranch(p, vec2(0.0, -245.0), vec2(0.0, -110.0), vec2(-276.0, 18.0), 1.45, 3.55, 0.28, 0.64, nearest, width);\n" +
            "    nearestSplitBranch(p, vec2(0.0, -245.0), vec2(0.0, -110.0), vec2(18.0, 82.0), 1.45, 3.65, 0.28, 0.64, nearest, width);\n" +
            "    nearestSplitBranch(p, vec2(0.0, -245.0), vec2(0.0, -110.0), vec2(232.0, 48.0), 1.45, 3.55, 0.28, 0.64, nearest, width);\n" +
            "    nearestSplitBranch(p, vec2(0.0, -110.0), vec2(-276.0, 18.0), vec2(-386.0, 226.0), 3.55, 6.0, 0.64, 1.00, nearest, width);\n" +
            "    nearestSplitBranch(p, vec2(0.0, -110.0), vec2(-276.0, 18.0), vec2(-224.0, 244.0), 3.55, 5.85, 0.64, 1.00, nearest, width);\n" +
            "    nearestSplitBranch(p, vec2(0.0, -110.0), vec2(18.0, 82.0), vec2(-98.0, 258.0), 3.65, 6.05, 0.64, 1.00, nearest, width);\n" +
            "    nearestSplitBranch(p, vec2(0.0, -110.0), vec2(18.0, 82.0), vec2(42.0, 266.0), 3.65, 6.05, 0.64, 1.00, nearest, width);\n" +
            "    nearestSplitBranch(p, vec2(0.0, -110.0), vec2(232.0, 48.0), vec2(214.0, 246.0), 3.55, 5.85, 0.64, 1.00, nearest, width);\n" +
            "    nearestSplitBranch(p, vec2(0.0, -110.0), vec2(232.0, 48.0), vec2(344.0, 232.0), 3.55, 6.0, 0.64, 1.00, nearest, width);\n" +
            "    if (nearest >= width || width <= 0.0) discard;\n" +
            "    float edge = clamp(nearest / width, 0.0, 1.0);\n" +
            "    float alpha = 1.0 - smoothstep(0.0, 1.0, edge);\n" +
            "    vec3 color = mix(vec3(0.18, 0.48, 0.95), vec3(0.92, 0.96, 1.0), 1.0 - smoothstep(0.0, 0.42, edge));\n" +
            "    gl_FragColor = vec4(color, alpha);\n" +
            "}\n";

    public KabbalahLifeTreeEffect() {
        nodes = createNodes();
        createShader();
    }

    @Override
    public void initialize(float x, float y) {
        position.x = x;
        position.y = y;
        elapsed = 0f;
        expired = false;
        hoverBurst = 0f;
        hoveredNode = -1;
        mouseX = Float.NaN;
        mouseY = Float.NaN;
        for (TreeNode node : nodes) node.wasHovered = false;
    }

    @Override
    public void setMousePosition(float x, float y) {
        mouseX = x;
        mouseY = y;
    }

    @Override
    public void advance(float amount) {
        elapsed += amount;
        updateHover(amount);
    }

    @Override
    public void render() {
        int tex = renderToFBO();
        if (tex == 0) return;

        float halfW = canvasWidth * 0.5f;
        float halfH = canvasHeight * 0.5f;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glTranslatef(position.x, position.y, 0f);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(-halfW, -halfH);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(halfW, -halfH);
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(halfW, halfH);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(-halfW, halfH);
        GL11.glEnd();

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void cleanup() {
        expired = true;
        destroyFBO();
        if (branchShader > 0) {
            GL20.glDeleteProgram(branchShader);
            branchShader = 0;
        }
    }

    private TreeNode[] createNodes() {
        return new TreeNode[]{
                new TreeNode(new Vector2f(0f, -110f), 1.45f, 0),
                new TreeNode(new Vector2f(-276f, 18f), 3.55f, 1),
                new TreeNode(new Vector2f(18f, 82f), 3.65f, 1),
                new TreeNode(new Vector2f(232f, 48f), 3.55f, 1),
                new TreeNode(new Vector2f(-386f, 226f), 6.0f, 2),
                new TreeNode(new Vector2f(-224f, 244f), 5.85f, 2),
                new TreeNode(new Vector2f(-98f, 258f), 6.05f, 2),
                new TreeNode(new Vector2f(42f, 266f), 6.05f, 2),
                new TreeNode(new Vector2f(214f, 246f), 5.85f, 2),
                new TreeNode(new Vector2f(344f, 232f), 6.0f, 2)
        };
    }

    private void createShader() {
        try {
            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, VERT_SRC);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Kabbalah tree vert compile failed: " + GL20.glGetShaderInfoLog(vert, 2048));
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, FRAG_SRC);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Kabbalah tree frag compile failed: " + GL20.glGetShaderInfoLog(frag, 4096));
                return;
            }

            branchShader = GL20.glCreateProgram();
            GL20.glAttachShader(branchShader, vert);
            GL20.glAttachShader(branchShader, frag);
            GL20.glLinkProgram(branchShader);
            if (GL20.glGetProgrami(branchShader, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("Kabbalah tree link failed: " + GL20.glGetProgramInfoLog(branchShader, 2048));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);
            uTimeLoc = GL20.glGetUniformLocation(branchShader, "u_time");
            uCanvasLoc = GL20.glGetUniformLocation(branchShader, "u_canvas");
        } catch (Exception e) {
            System.err.println("Kabbalah tree shader error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateHover(float amount) {
        hoveredNode = -1;
        if (!Float.isNaN(mouseX) && !Float.isNaN(mouseY)) {
            float localX = mouseX - position.x;
            float localY = mouseY - position.y;
            for (int i = 0; i < nodes.length; i++) {
                TreeNode node = nodes[i];
                if (elapsed < node.birthTime) continue;
                float dx = localX - node.position.x;
                float dy = localY - node.position.y;
                float radius = 30f + node.stage * 5f;
                boolean hit = dx * dx + dy * dy <= radius * radius;
                if (hit) {
                    hoveredNode = i;
                    if (!node.wasHovered) hoverBurst = 1f;
                    node.wasHovered = true;
                } else {
                    node.wasHovered = false;
                }
            }
        }
        hoverBurst = Math.max(0f, hoverBurst - amount * 1.7f);
    }

    private int renderToFBO() {
        ensureFBO((int) canvasWidth, (int) canvasHeight);
        if (fboId == 0) return 0;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL11.glViewport(0, 0, fboW, fboH);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(-canvasWidth * 0.5f, canvasWidth * 0.5f, canvasHeight * 0.5f, -canvasHeight * 0.5f, -1f, 1f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        renderBackground();
        renderBranchShader();
        // Temporarily keep nodes disabled while tuning the trunk shader.

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glPopAttrib();
        return fboTexId;
    }

    private void renderBackground() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float hw = canvasWidth * 0.5f;
        float hh = canvasHeight * 0.5f;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(0.004f, 0.012f, 0.032f, 1f);
        GL11.glVertex2f(-hw, -hh);
        GL11.glColor4f(0.014f, 0.04f, 0.085f, 1f);
        GL11.glVertex2f(hw, -hh);
        GL11.glColor4f(0.006f, 0.018f, 0.04f, 1f);
        GL11.glVertex2f(hw, hh);
        GL11.glColor4f(0.015f, 0.036f, 0.075f, 1f);
        GL11.glVertex2f(-hw, hh);
        GL11.glEnd();

        float pulse = 0.5f + 0.5f * (float) Math.sin(elapsed * 0.45f);
        drawFilledCircle(0f, 45f, 430f, 96, 0.04f, 0.14f, 0.28f, 0.07f + pulse * 0.02f);
        drawRectBorder(-hw + 8f, -hh + 8f, hw - 8f, hh - 8f, 0.18f, 0.42f, 0.62f, 0.18f);
    }

    private void renderBranchShader() {
        if (branchShader <= 0) return;
        float hw = canvasWidth * 0.5f;
        float hh = canvasHeight * 0.5f;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL20.glUseProgram(branchShader);
        GL20.glUniform1f(uTimeLoc, elapsed);
        GL20.glUniform2f(uCanvasLoc, canvasWidth, canvasHeight);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(-hw, -hh);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(hw, -hh);
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(hw, hh);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(-hw, hh);
        GL11.glEnd();

        GL20.glUseProgram(0);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void renderNodes() {
        for (int i = 0; i < nodes.length; i++) {
            TreeNode node = nodes[i];
            if (elapsed < node.birthTime) continue;
            float age = elapsed - node.birthTime;
            float fade = smooth(clamp(age / 0.7f));
            drawNode(i, node, age, fade, i == hoveredNode);
        }
    }

    private void drawNode(int index, TreeNode node, float age, float fade, boolean hovered) {
        float breath = 0.5f + 0.5f * (float) Math.sin(elapsed * 2.1f + index * 0.75f);
        float hover = hovered ? 1f : 0f;
        float burst = hovered ? hoverBurst : 0f;
        float baseRadius = 14f + node.stage * 2f;
        float r = baseRadius * (1f + breath * 0.08f + hover * 0.16f);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        drawNodeRays(node.position.x, node.position.y, node.stage, fade, hover, burst, age);
        drawFilledCircle(node.position.x, node.position.y, r * 3.8f, 64, 0.35f, 0.74f, 1f, fade * (0.06f + hover * 0.08f));
        drawRing(node.position.x, node.position.y, r * (1.75f + breath * 0.12f), 96, 3.1f, 0.6f, 0.9f, 1f, fade * (0.35f + hover * 0.28f));
        drawRing(node.position.x, node.position.y, r * (2.35f - breath * 0.08f), 96, 1.4f, 0.35f, 0.72f, 1f, fade * (0.18f + hover * 0.18f));
        drawFilledCircle(node.position.x, node.position.y, r * 1.28f, 48, 0.18f, 0.48f, 0.9f, fade * 0.42f);
        drawFilledCircle(node.position.x, node.position.y, r * 0.76f, 48, 0.72f, 0.93f, 1f, fade * 0.9f);
        drawFilledCircle(node.position.x - r * 0.18f, node.position.y - r * 0.22f, r * 0.27f, 24, 1f, 1f, 1f, fade * 0.85f);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawNodeRays(float x, float y, int stage, float fade, float hovered, float burst, float age) {
        float stageScale = stage == 0 ? 1.0f : stage == 1 ? 0.86f : 0.72f;
        float spin = elapsed * (24f + stage * 7f) + stage * 17f;
        float length = (72f + hovered * 42f + burst * 58f) * stageScale;
        float width = (7f + hovered * 5f + burst * 7f) * stageScale;
        for (int i = 0; i < 4; i++) {
            float angle = spin + i * 90f + (float) Math.sin(age * 1.4f + i) * 7f;
            drawRay(x, y, angle, length, width, fade * (0.22f + hovered * 0.24f + burst * 0.32f));
        }
    }

    private void drawRay(float x, float y, float degrees, float length, float width, float alpha) {
        double rad = Math.toRadians(degrees);
        float dx = (float) Math.cos(rad);
        float dy = (float) Math.sin(rad);
        float nx = -dy;
        float ny = dx;
        float inner = 16f;
        float outer = inner + length;

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(0.5f, 0.86f, 1f, alpha * 0.95f);
        GL11.glVertex2f(x + dx * inner + nx * width * 0.35f, y + dy * inner + ny * width * 0.35f);
        GL11.glVertex2f(x + dx * inner - nx * width * 0.35f, y + dy * inner - ny * width * 0.35f);
        GL11.glColor4f(0.8f, 0.96f, 1f, 0f);
        GL11.glVertex2f(x + dx * outer - nx * width, y + dy * outer - ny * width);
        GL11.glVertex2f(x + dx * outer + nx * width, y + dy * outer + ny * width);
        GL11.glEnd();
    }

    private void drawFilledCircle(float cx, float cy, float radius, int segments, float r, float g, float b, float a) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(r, g, b, a);
        GL11.glVertex2f(cx, cy);
        GL11.glColor4f(r, g, b, 0f);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            GL11.glVertex2f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    private void drawRing(float cx, float cy, float radius, int segments, float width, float r, float g, float b, float a) {
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        float half = width * 0.5f;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            float dx = (float) Math.cos(angle);
            float dy = (float) Math.sin(angle);
            GL11.glColor4f(r, g, b, a);
            GL11.glVertex2f(cx + dx * (radius - half), cy + dy * (radius - half));
            GL11.glColor4f(r, g, b, a * 0.35f);
            GL11.glVertex2f(cx + dx * (radius + half), cy + dy * (radius + half));
        }
        GL11.glEnd();
    }

    private void drawRectBorder(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x1, y2);
        GL11.glEnd();
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private float smooth(float value) {
        value = clamp(value);
        return value * value * (3f - 2f * value);
    }

    private void ensureFBO(int w, int h) {
        if (fboId != 0 && fboW == w && fboH == h) return;
        destroyFBO();
        fboW = w;
        fboH = h;

        fboTexId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, fboW, fboH, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        fboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, fboTexId, 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("KabbalahLifeTreeEffect FBO incomplete: " + status);
            destroyFBO();
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private void destroyFBO() {
        if (fboId != 0) {
            GL30.glDeleteFramebuffers(fboId);
            fboId = 0;
        }
        if (fboTexId != 0) {
            GL11.glDeleteTextures(fboTexId);
            fboTexId = 0;
        }
    }

    private static class TreeNode {
        private final Vector2f position;
        private final float birthTime;
        private final int stage;
        private boolean wasHovered;

        private TreeNode(Vector2f position, float birthTime, int stage) {
            this.position = position;
            this.birthTime = birthTime;
            this.stage = stage;
        }
    }
}
