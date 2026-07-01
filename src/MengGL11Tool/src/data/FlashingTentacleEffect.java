package data;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class FlashingTentacleEffect implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {

    private Vector2f position;
    private float elapsed;
    private float duration;
    private boolean expired;

    private float width;
    private float height;
    
    private int shaderProgram;
    private int vbo;
    private int ibo;
    private int fboId = 0;
    private int fboTexId = 0;
    private int fboW = 0;
    private int fboH = 0;

    private int uTimeLoc;
    private int uDurationLoc;
    private int uProgressLoc;

    public FlashingTentacleEffect() {
        this.position = new Vector2f(0f, 0f);
        this.elapsed = 0f;
        this.duration = 1000.0f;
        this.expired = false;
        this.width = 500f;
        this.height = 500f;

        createShaderProgram();
        createBuffers();
        cacheUniformLocations();
    }

    private void createShaderProgram() {
        try {
            String vertexSource = 
                "#version 110\n" +
                "attribute vec2 a_position;\n" +
                "attribute vec2 a_texCoord;\n" +
                "varying vec2 v_uv;\n" +
                "void main() {\n" +
                "    v_uv = a_texCoord;\n" +
                "    gl_Position = gl_ModelViewProjectionMatrix * vec4(a_position, 0.0, 1.0);\n" +
                "}\n";
            
            String fragmentSource = 
                "#version 110\n" +
                "varying vec2 v_uv;\n" +
                "uniform float u_time;\n" +
                "uniform float u_duration;\n" +
                "uniform float u_progress;\n" +
                "\n" +
                "vec4 permute(vec4 x) {\n" +
                "    return mod(((x * 34.0) + 1.0) * x, 289.0);\n" +
                "}\n" +
                "\n" +
                "float snoise(vec3 v) {\n" +
                "    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);\n" +
                "    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);\n" +
                "\n" +
                "    vec3 i  = floor(v + dot(v, C.yyy));\n" +
                "    vec3 x0 = v - i + dot(i, C.xxx);\n" +
                "\n" +
                "    vec3 g = step(x0.yzx, x0.xyz);\n" +
                "    vec3 l = 1.0 - g;\n" +
                "    vec3 i1 = min(g.xyz, l.zxy);\n" +
                "    vec3 i2 = max(g.xyz, l.zxy);\n" +
                "\n" +
                "    vec3 x1 = x0 - i1 + C.xxx;\n" +
                "    vec3 x2 = x0 - i2 + C.yyy;\n" +
                "    vec3 x3 = x0 - D.yyy;\n" +
                "\n" +
                "    i = mod(i, 289.0);\n" +
                "    vec4 p = permute(permute(permute(\n" +
                "             i.z + vec4(0.0, i1.z, i2.z, 1.0))\n" +
                "           + i.y + vec4(0.0, i1.y, i2.y, 1.0))\n" +
                "           + i.x + vec4(0.0, i1.x, i2.x, 1.0));\n" +
                "\n" +
                "    float n_ = 0.142857142857;\n" +
                "    vec3 ns = n_ * D.wyz - D.xzx;\n" +
                "\n" +
                "    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);\n" +
                "\n" +
                "    vec4 x_ = floor(j * ns.z);\n" +
                "    vec4 y_ = floor(j - 7.0 * x_);\n" +
                "\n" +
                "    vec4 x = x_ * ns.x + ns.yyyy;\n" +
                "    vec4 y = y_ * ns.x + ns.yyyy;\n" +
                "    vec4 h = 1.0 - abs(x) - abs(y);\n" +
                "\n" +
                "    vec4 b0 = vec4(x.xy, y.xy);\n" +
                "    vec4 b1 = vec4(x.zw, y.zw);\n" +
                "\n" +
                "    vec4 s0 = floor(b0) * 2.0 + 1.0;\n" +
                "    vec4 s1 = floor(b1) * 2.0 + 1.0;\n" +
                "    vec4 sh = -step(h, vec4(0.0));\n" +
                "\n" +
                "    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;\n" +
                "    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;\n" +
                "\n" +
                "    vec3 p0 = vec3(a0.xy, h.x);\n" +
                "    vec3 p1 = vec3(a0.zw, h.y);\n" +
                "    vec3 p2 = vec3(a1.xy, h.z);\n" +
                "    vec3 p3 = vec3(a1.zw, h.w);\n" +
                "\n" +
                "    vec4 norm = 1.0 / vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3));\n" +
                "    p0 *= norm.x;\n" +
                "    p1 *= norm.y;\n" +
                "    p2 *= norm.z;\n" +
                "    p3 *= norm.w;\n" +
                "\n" +
                "    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);\n" +
                "    m = m * m;\n" +
                "    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));\n" +
                "}\n" +
                "\n" +
                "float turbulence(vec3 p) {\n" +
                "    float value = 0.0;\n" +
                "    float amplitude = 1.0;\n" +
                "    float frequency = 1.0;\n" +
                "\n" +
                "    for (int i = 0; i < 6; i++) {\n" +
                "        value += amplitude * abs(snoise(p * frequency));\n" +
                "        amplitude *= 0.5;\n" +
                "        frequency *= 2.0;\n" +
                "    }\n" +
                "\n" +
                "    return value * 0.5;\n" +
                "}\n" +
                "\n" +
                "float bezier4(vec2 p0, vec2 p1, vec2 p2, vec2 p3, vec2 p4, float t) {\n" +
                "    float u = 1.0 - t;\n" +
                "    float tt = t * t;\n" +
                "    float uu = u * u;\n" +
                "    float uuu = uu * u;\n" +
                "    float uuuu = uuu * u;\n" +
                "    float ttt = tt * t;\n" +
                "    float tttt = ttt * t;\n" +
                "    \n" +
                "    vec2 point = uuuu * p0 + 4.0 * uuu * t * p1 + 6.0 * uu * tt * p2 + 4.0 * u * ttt * p3 + tttt * p4;\n" +
                "    return point.y;\n" +
                "}\n" +
                "\n" +
                "float singleFilament(vec2 uv, vec2 origin, float angle, float length,\n" +
                "                     float baseWidth, float frequency, float amplitude,\n" +
                "                     float speed, float opacity, float time) {\n" +
                "    vec2 dir = vec2(cos(angle), sin(angle));\n" +
                "    vec2 perp = vec2(-dir.y, dir.x);\n" +
                "    \n" +
                "    vec2 delta = uv - origin;\n" +
                "    float along = dot(delta, dir) / length;\n" +
                "    float across = dot(delta, perp);\n" +
                "    \n" +
                "    float wave = sin(along * frequency * 10.0 - time * speed) * amplitude * along;\n" +
                "    \n" +
                "    float widthFalloff = 1.0 - smoothstep(0.5, 1.0, along);\n" +
                "    float currentWidth = baseWidth * widthFalloff;\n" +
                "    \n" +
                "    float distFromCenter = abs(across - wave);\n" +
                "    float lateral = smoothstep(currentWidth, 0.0, distFromCenter);\n" +
                "    \n" +
                "    float mask = step(0.0, along) * step(along, 1.0);\n" +
                "    \n" +
                "    return lateral * mask * opacity;\n" +
                "}\n" +
                "\n" +
                "float bezierRibbon(vec2 normalizedUV, float startX, float startY, float ribbonLength,\n" +
                "                   float p1Amp, float p1Freq, float p2Amp, float p2Freq,\n" +
                "                   float p3Amp, float p3Freq, float p4Amp, float p4Freq,\n" +
                "                   float time) {\n" +
                "    float p1Offset = sin(time * p1Freq) * p1Amp;\n" +
                "    float p2Offset = sin(time * p2Freq + 1.0) * p2Amp;\n" +
                "    float p3Offset = sin(time * p3Freq + 2.0) * p3Amp;\n" +
                "    float p4Offset = sin(time * p4Freq + 3.0) * p4Amp;\n" +
                "    \n" +
                "    vec2 p0 = vec2(startX, startY);\n" +
                "    vec2 p1 = vec2(startX + ribbonLength * 0.25, startY + p1Offset);\n" +
                "    vec2 p2 = vec2(startX + ribbonLength * 0.5, startY + p2Offset);\n" +
                "    vec2 p3 = vec2(startX + ribbonLength * 0.75, startY + p3Offset);\n" +
                "    vec2 p4 = vec2(startX + ribbonLength, startY + p4Offset);\n" +
                "    \n" +
                "    float paramT = normalizedUV.x /2.0 / ribbonLength;\n" +
                "    \n" +
                "    if (paramT < 0.0 || paramT > 1.0) {\n" +
                "        return 0.0;\n" +
                "    }\n" +
                "    \n" +
                "    float bezierY = bezier4(p0, p1, p2, p3, p4, paramT);\n" +
                "    \n" +
                "    float baseWidth = 3.0*(0.01 + normalizedUV.x * 0.03);\n" +
                "    float widthGrowth = pow(paramT, 0.4);\n" +
                "    float maxDistance = baseWidth * max(widthGrowth, 0.1);\n" +
                "    float expandedMaxDistance = maxDistance * 1.5;\n" +
                "    \n" +
                "    float distanceFromCurve = abs(normalizedUV.y - bezierY);\n" +
                "    \n" +
                "    if (distanceFromCurve > expandedMaxDistance) {\n" +
                "        return 0.0;\n" +
                "    }\n" +
                "    \n" +
                "    float edgeFade = 1.0 - smoothstep(maxDistance, expandedMaxDistance, distanceFromCurve);\n" +
                "    \n" +
                "    float startFade = smoothstep(0.0, 0.1, paramT);\n" +
                "    float endFade = 1.0 - smoothstep(0.9, 1.0, paramT);\n" +
                "    float longitudinalFade = startFade * endFade;\n" +
                "    \n" +
                "    float normalizedPos = (normalizedUV.y - (bezierY - expandedMaxDistance)) / (2.0 * expandedMaxDistance);\n" +
                "    normalizedPos = clamp(normalizedPos, 0.0, 1.0);\n" +
                "    \n" +
                "    float cosValue = cos(normalizedPos * 3.14159);\n" +
                "    \n" +
                "    vec3 noiseCoord = vec3(cosValue , 1.5 * (paramT - time * 0.1), time * 0.1);\n" +
                "    float noiseValue = turbulence(noiseCoord);\n" +
                "    \n" +
                "    noiseValue = pow(noiseValue, 2.0);\n" +
                "    float adjustedNoise = noiseValue * 1.8 - 0.5;\n" +
                "    \n" +
                "    float alpha = (1.0 + adjustedNoise) * edgeFade * longitudinalFade;\n" +
                "    \n" +
                "    return alpha;\n" +
                "}\n" +

                "void main() {\n" +
                "    vec2 uv = vec2(v_uv.x*2.0,2.0*(v_uv.y-0.5)+0.5);\n" +
                "    \n" +
                "    float aspectX = 500.0;\n" +
                "    float aspectY = 500.0;\n" +
                "    vec2 correctedUV = vec2(uv.x * aspectX / aspectY, uv.y);\n" +
                "    \n" +
                "    float baseWidth = 0.03;\n" +
                "    \n" +
                "    float filament1 = singleFilament(correctedUV, vec2(0.0, 0.48), -0.45, 0.85, baseWidth, 0.8, 0.05, 6.0, 0.9, u_time);\n" +
                "    float filament2 = singleFilament(correctedUV, vec2(0.0, 0.52), -0.35, 0.7, baseWidth * 0.85, 0.6, 0.04, 7.0, 0.7, u_time);\n" +
                "    float filament3 = singleFilament(correctedUV, vec2(0.0, 0.49), -0.25, 0.8, baseWidth * 0.9, 0.7, 0.055, 6.5, 0.85, u_time);\n" +
                "    float filament4 = singleFilament(correctedUV, vec2(0.0, 0.51), -0.15, 0.65, baseWidth * 0.8, 0.9, 0.045, 7.5, 0.65, u_time);\n" +
                "    float filament5 = singleFilament(correctedUV, vec2(0.0, 0.50), -0.05, 0.9, baseWidth * 0.75, 0.5, 0.035, 8.0, 0.95, u_time);\n" +
                "    float filament6 = singleFilament(correctedUV, vec2(0.0, 0.48), 0.05, 0.75, baseWidth * 0.85, 0.85, 0.042, 6.8, 0.8, u_time);\n" +
                "    float filament7 = singleFilament(correctedUV, vec2(0.0, 0.52), 0.15, 0.6, baseWidth * 0.7, 0.65, 0.048, 7.2, 0.75, u_time);\n" +
                "    float filament8 = singleFilament(correctedUV, vec2(0.0, 0.49), 0.25, 0.85, baseWidth * 0.9, 0.75, 0.038, 6.3, 0.9, u_time);\n" +
                "    float filament9 = singleFilament(correctedUV, vec2(0.0, 0.51), 0.35, 0.7, baseWidth * 0.8, 0.55, 0.044, 7.8, 0.7, u_time);\n" +
                "    float filament10 = singleFilament(correctedUV, vec2(0.0, 0.50), 0.45, 0.8, baseWidth * 0.75, 0.8, 0.05, 6.5, 0.85, u_time);\n" +
                "    float filament11 = singleFilament(correctedUV, vec2(0.0, 0.49), -0.4, 0.65, baseWidth * 0.8, 0.7, 0.046, 6.8, 0.6, u_time);\n" +
                "    float filament12 = singleFilament(correctedUV, vec2(0.0, 0.51), -0.2, 0.9, baseWidth * 0.85, 0.65, 0.052, 7.0, 0.8, u_time);\n" +
                "    float filament13 = singleFilament(correctedUV, vec2(0.0, 0.50), 0.1, 0.55, baseWidth * 0.7, 0.8, 0.04, 7.5, 0.65, u_time);\n" +
                "    float filament14 = singleFilament(correctedUV, vec2(0.0, 0.48), 0.3, 0.8, baseWidth * 0.9, 0.6, 0.048, 6.5, 0.75, u_time);\n" +
                "    float filament15 = singleFilament(correctedUV, vec2(0.0, 0.52), 0.4, 0.7, baseWidth * 0.75, 0.8, 0.043, 7.2, 0.7, u_time);\n" +
                "    \n" +
                "    float tentacleIntensity = filament1 + filament2 + filament3 + filament4 + filament5 + \n" +
                "                              filament6 + filament7 + filament8 + filament9 + filament10 +\n" +
                "                              filament11 + filament12 + filament13 + filament14 + filament15;\n" +
                "    \n" +
                "    vec2 normalizedUV = vec2(uv.x, 1.0 - uv.y);\n" +
                "    \n" +
                "    float ribbon1 = bezierRibbon(normalizedUV, 0.0, 0.45, 0.35, 0.08, 1.5, 0.12, 2.0, 0.20, 1.8, 0.35, 1.2, u_time);\n" +
                "    float ribbon2 = bezierRibbon(normalizedUV, 0.0, 0.50, 0.43, 0.07, 1.8, 0.20, 2.3, 0.35, 2.1, 0.51, 1.6, u_time);\n" +
                "    float ribbon3 = bezierRibbon(normalizedUV, 0.0, 0.55, 0.15, 0.02, 1.3, 0.08, 1.7, 0.17, 1.3, 0.25, 0.9, u_time);\n" +
                "    float ribbon4 = bezierRibbon(normalizedUV, 0.0, 0.48, 0.40, 0.04, 2.0, 0.16, 2.5, 0.29, 2.3, 0.45, 1.7, u_time);\n" +
                "    float ribbon5 = bezierRibbon(normalizedUV, 0.0, 0.52, 0.30, 0.07, 1.6, 0.11, 1.9, 0.20, 1.6, 0.30, 1.1, u_time);\n" +
                "    float ribbon6 = bezierRibbon(normalizedUV, 0.0, 0.47, 0.38, 0.08, 1.9, 0.13, 2.2, 0.22, 2.0, 0.36, 1.4, u_time);\n" +
                "    float ribbon7 = bezierRibbon(normalizedUV, 0.0, 0.53, 0.42, 0.06, 1.4, 0.18, 1.8, 0.30, 1.6, 0.45, 1.0, u_time);\n" +
                "    float ribbon8 = bezierRibbon(normalizedUV, 0.0, 0.49, 0.25, 0.08, 1.7, 0.12, 2.1, 0.20, 1.9, 0.36, 1.3, u_time);\n" +

                "    float ribbonIntensity = ribbon1 + ribbon2 + ribbon3 + ribbon4 + ribbon5 + ribbon6 + ribbon7 + ribbon8;\n" +
                "    ribbonIntensity *= 0.4;\n" +
                "    \n" +
                "    float totalIntensity = tentacleIntensity + ribbonIntensity;\n" +
                "    \n" +
                "    float startFade = smoothstep(0.0, 0.05, normalizedUV.x);\n" +
                "    vec3 color = vec3(0.8, 0.4, 1.0);\n" +
                "    float alpha = totalIntensity*startFade;\n" +
                "    \n" +
                "    gl_FragColor = vec4(color * alpha, alpha);\n" +
                "}\n";
            
            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, vertexSource);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Tentacle vert compile failed: " + GL20.glGetShaderInfoLog(vert, 1024));
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, fragmentSource);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Tentacle frag compile failed: " + GL20.glGetShaderInfoLog(frag, 1024));
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vert);
            GL20.glAttachShader(shaderProgram, frag);
            GL20.glBindAttribLocation(shaderProgram, 0, "a_position");
            GL20.glBindAttribLocation(shaderProgram, 1, "a_texCoord");
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("Tentacle link failed: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);

            System.out.println("FlashingTentacle shader compiled OK");
        } catch (Exception e) {
            System.err.println("FlashingTentacle shader error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String loadShaderFromFile(String filePath) {
        StringBuilder source = new StringBuilder();
        
        java.io.InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            System.err.println("无法找到着色器资源: " + filePath);
            return "";
        }
        
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append("\n");
            }
        } catch (java.io.IOException e) {
            System.err.println("无法加载着色器文件: " + filePath);
            e.printStackTrace();
        }
        return source.toString();
    }

    private void createBuffers() {
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;

        FloatBuffer verts = org.lwjgl.BufferUtils.createFloatBuffer(16);
        verts.put(new float[]{
            -halfWidth, -halfHeight,  0f, 0f,
             halfWidth, -halfHeight,  1f, 0f,
             halfWidth,  halfHeight,  1f, 1f,
            -halfWidth,  halfHeight,  0f, 1f,
        });
        verts.flip();

        IntBuffer indices = org.lwjgl.BufferUtils.createIntBuffer(6);
        indices.put(new int[]{0, 1, 2, 0, 2, 3});
        indices.flip();

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verts, GL15.GL_STATIC_DRAW);

        ibo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void cacheUniformLocations() {
        if (shaderProgram <= 0) return;
        uTimeLoc = GL20.glGetUniformLocation(shaderProgram, "u_time");
        uDurationLoc = GL20.glGetUniformLocation(shaderProgram, "u_duration");
        uProgressLoc = GL20.glGetUniformLocation(shaderProgram, "u_progress");
    }

    @Override
    public void initialize(float x, float y) {
        this.position.x = x;
        this.position.y = y;
        this.elapsed = 0f;
        this.expired = false;
    }

    @Override
    public void advance(float amount) {
        elapsed += amount;
        
        if (elapsed >= duration) {
            expired = true;
            return;
        }
    }

    private void ensureFBO(int w, int h) {
        int tw = nextPowerOfTwo(w);
        int th = nextPowerOfTwo(h);
        if (fboId != 0 && fboW == tw && fboH == th) return;
        destroyFBO();
        fboW = tw;
        fboH = th;

        fboTexId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, fboW, fboH,
                0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        fboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, fboTexId, 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            destroyFBO();
            return;
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private void destroyFBO() {
        if (fboId != 0) { GL30.glDeleteFramebuffers(fboId); fboId = 0; }
        if (fboTexId != 0) { GL11.glDeleteTextures(fboTexId); fboTexId = 0; }
    }

    private int nextPowerOfTwo(int value) {
        if (value <= 0) return 1;
        value--;
        value |= value >> 1; value |= value >> 2;
        value |= value >> 4; value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }

    private int renderToFBO() {
        ensureFBO((int) width, (int) height);
        if (fboId == 0) return 0;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL20.glUseProgram(0);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL11.glViewport(0, 0, fboW, fboH);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(-width/2, width/2, -height/2, height/2, -1, 1);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glColorMask(true, true, true, true);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        float progress = elapsed / duration;
        progress = Math.max(0.0f, Math.min(1.0f, progress));

        GL20.glUseProgram(shaderProgram);
        GL20.glUniform1f(uTimeLoc, elapsed);
        GL20.glUniform1f(uDurationLoc, duration);
        GL20.glUniform1f(uProgressLoc, progress);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);

        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL20.glUseProgram(0);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glPopAttrib();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexId);
        return fboTexId;
    }

    @Override
    public void render() {
        if (expired || shaderProgram <= 0) return;

        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;

        int texId = renderToFBO();
        if (texId == 0) return;

        java.nio.FloatBuffer before = org.lwjgl.BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, before);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        GL11.glTranslatef(position.x, position.y, 0f);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        float uvScaleX = width / (float) fboW;
        float uvScaleY = height / (float) fboH;
        float uvOffsetY = (fboH - height) / (2.0f * fboH);

        float offset = width * 0.6f;

        // 左翅膀
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(uvScaleX, uvOffsetY);
        GL11.glVertex2f(-halfWidth - offset, -halfHeight);
        GL11.glTexCoord2f(0f, uvOffsetY);
        GL11.glVertex2f(halfWidth - offset, -halfHeight);
        GL11.glTexCoord2f(0f, uvOffsetY + uvScaleY);
        GL11.glVertex2f(halfWidth - offset, halfHeight);
        GL11.glTexCoord2f(uvScaleX, uvOffsetY + uvScaleY);
        GL11.glVertex2f(-halfWidth - offset, halfHeight);
        GL11.glEnd();

        // 右翅膀
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, uvOffsetY);
        GL11.glVertex2f(-halfWidth + offset, -halfHeight);
        GL11.glTexCoord2f(uvScaleX, uvOffsetY);
        GL11.glVertex2f(halfWidth + offset, -halfHeight);
        GL11.glTexCoord2f(uvScaleX, uvOffsetY + uvScaleY);
        GL11.glVertex2f(halfWidth + offset, halfHeight);
        GL11.glTexCoord2f(0f, uvOffsetY + uvScaleY);
        GL11.glVertex2f(-halfWidth + offset, halfHeight);
        GL11.glEnd();

        GL11.glDepthMask(true);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glPopAttrib();
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    
    public void cleanup() {
        expired = true;
        if (shaderProgram > 0) {
            GL20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (ibo != 0) {
            GL15.glDeleteBuffers(ibo);
            ibo = 0;
        }
        destroyFBO();
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void setDuration(float dur) {
        this.duration = dur;
    }
}
