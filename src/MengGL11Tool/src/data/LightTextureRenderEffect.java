package data;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class LightTextureRenderEffect implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {

    private Vector2f position;
    private float elapsed;
    private float duration;
    private boolean expired;
    
    private float canvasWidth;
    private float canvasHeight;
    
    private int shaderProgram;
    private int vbo;
    private int ibo;
    private int textureId;
    private int textureWidth;
    private int textureHeight;
    
    private int fbo1, fbo2;
    private int tex1, tex2;
    private int blurProgramH, blurProgramV;
    private int uBlurRadiusLocH, uBlurRadiusLocV;
    private int uTexSizeLocH, uTexSizeLocV;
    private int uBlurTexLocH, uBlurTexLocV;
    
    private int uTimeLoc;
    private int uTextureLoc;

    public LightTextureRenderEffect() {
        this.position = new Vector2f(0f, 0f);
        this.elapsed = 0f;
        this.duration = 1000.0f;
        this.expired = false;
        this.canvasWidth = 144f;
        this.canvasHeight = 240f;

        loadTexture();
        createFramebuffers();
        createBuffers();
        createBlurShaders();
        createShaderProgram();
        cacheUniformLocations();
    }

    private void loadTexture() {
        try {
            String texturePath = "D:\\Starsector\\mods\\MengGod's legacy\\graphics\\Meng\\ships\\fire\\li\\example_light.png";
            File textureFile = new File(texturePath);
            
            if (!textureFile.exists()) {
                System.err.println("纹理文件不存在: " + texturePath);
                return;
            }

            BufferedImage image = ImageIO.read(textureFile);
            if (image == null) {
                System.err.println("无法读取纹理文件");
                return;
            }

            textureWidth = image.getWidth();
            textureHeight = image.getHeight();
            
            int[] pixels = new int[textureWidth * textureHeight];
            image.getRGB(0, 0, textureWidth, textureHeight, pixels, 0, textureWidth);

            ByteBuffer buffer = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
            
            for (int y = 0; y < textureHeight; y++) {
                for (int x = 0; x < textureWidth; x++) {
                    int pixel = pixels[y * textureWidth + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }
            
            buffer.flip();

            textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    textureWidth, textureHeight, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            System.out.println("光照纹理加载成功: " + textureWidth + "x" + textureHeight);

        } catch (Exception e) {
            System.err.println("光照纹理加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createFramebuffers() {
        tex1 = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 
                         textureWidth, textureHeight, 0, 
                         GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        
        fbo1 = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo1);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, tex1, 0);
        int status1 = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status1 != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("FBO1 incomplete: " + status1);
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        
        tex2 = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex2);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 
                         textureWidth, textureHeight, 0, 
                         GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        
        fbo2 = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo2);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, tex2, 0);
        int status2 = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status2 != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("FBO2 incomplete: " + status2);
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        
        System.out.println("FBOs created successfully");
    }

    private void createBlurShaders() {
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
            
            String fragmentSourceH = 
                "#version 110\n" +
                "varying vec2 v_uv;\n" +
                "uniform sampler2D u_texture;\n" +
                "uniform float u_blurRadius;\n" +
                "uniform vec2 u_texSize;\n" +
                "\n" +
                "void main() {\n" +
                "    float step = 1.0 / u_texSize.x;\n" +
                "    float radius = u_blurRadius * step;\n" +
                "    \n" +
                "    vec4 sum = vec4(0.0);\n" +
                "    float weightSum = 0.0;\n" +
                "    \n" +
                "    for (int i = -4; i <= 4; i++) {\n" +
                "        float offset = float(i) * radius / 4.0;\n" +
                "        float weight = exp(-float(i*i) / 8.0);\n" +
                "        vec4 texel = texture2D(u_texture, v_uv + vec2(offset, 0.0));\n" +
                "        sum += texel * weight;\n" +
                "        weightSum += weight;\n" +
                "    }\n" +
                "    \n" +
                "    gl_FragColor = sum / weightSum;\n" +
                "}\n";
            
            String fragmentSourceV = 
                "#version 110\n" +
                "varying vec2 v_uv;\n" +
                "uniform sampler2D u_texture;\n" +
                "uniform float u_blurRadius;\n" +
                "uniform vec2 u_texSize;\n" +
                "\n" +
                "void main() {\n" +
                "    float step = 1.0 / u_texSize.y;\n" +
                "    float radius = u_blurRadius * step;\n" +
                "    \n" +
                "    vec4 sum = vec4(0.0);\n" +
                "    float weightSum = 0.0;\n" +
                "    \n" +
                "    for (int i = -4; i <= 4; i++) {\n" +
                "        float offset = float(i) * radius / 4.0;\n" +
                "        float weight = exp(-float(i*i) / 8.0);\n" +
                "        vec4 texel = texture2D(u_texture, v_uv + vec2(0.0, offset));\n" +
                "        sum += texel * weight;\n" +
                "        weightSum += weight;\n" +
                "    }\n" +
                "    \n" +
                "    gl_FragColor = sum / weightSum;\n" +
                "}\n";

            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, vertexSource);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Blur vertex shader compile failed: " + GL20.glGetShaderInfoLog(vert, 1024));
                return;
            }

            int fragH = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(fragH, fragmentSourceH);
            GL20.glCompileShader(fragH);
            if (GL20.glGetShaderi(fragH, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Horizontal blur fragment shader compile failed: " + GL20.glGetShaderInfoLog(fragH, 1024));
                return;
            }

            int fragV = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(fragV, fragmentSourceV);
            GL20.glCompileShader(fragV);
            if (GL20.glGetShaderi(fragV, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Vertical blur fragment shader compile failed: " + GL20.glGetShaderInfoLog(fragV, 1024));
                return;
            }

            blurProgramH = GL20.glCreateProgram();
            GL20.glAttachShader(blurProgramH, vert);
            GL20.glAttachShader(blurProgramH, fragH);
            GL20.glBindAttribLocation(blurProgramH, 0, "a_position");
            GL20.glBindAttribLocation(blurProgramH, 1, "a_texCoord");
            GL20.glLinkProgram(blurProgramH);

            if (GL20.glGetProgrami(blurProgramH, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("Horizontal blur program link failed: " + GL20.glGetProgramInfoLog(blurProgramH, 1024));
                return;
            }

            blurProgramV = GL20.glCreateProgram();
            GL20.glAttachShader(blurProgramV, vert);
            GL20.glAttachShader(blurProgramV, fragV);
            GL20.glBindAttribLocation(blurProgramV, 0, "a_position");
            GL20.glBindAttribLocation(blurProgramV, 1, "a_texCoord");
            GL20.glLinkProgram(blurProgramV);

            if (GL20.glGetProgrami(blurProgramV, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("Vertical blur program link failed: " + GL20.glGetProgramInfoLog(blurProgramV, 1024));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(fragH);
            GL20.glDeleteShader(fragV);

            uBlurRadiusLocH = GL20.glGetUniformLocation(blurProgramH, "u_blurRadius");
            uTexSizeLocH = GL20.glGetUniformLocation(blurProgramH, "u_texSize");
            uBlurTexLocH = GL20.glGetUniformLocation(blurProgramH, "u_texture");
            
            uBlurRadiusLocV = GL20.glGetUniformLocation(blurProgramV, "u_blurRadius");
            uTexSizeLocV = GL20.glGetUniformLocation(blurProgramV, "u_texSize");
            uBlurTexLocV = GL20.glGetUniformLocation(blurProgramV, "u_texture");

            System.out.println("Gaussian blur shaders compiled OK");
        } catch (Exception e) {
            System.err.println("Blur shader error: " + e.getMessage());
            e.printStackTrace();
        }
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
                "uniform sampler2D u_texture;\n" +
                "uniform float u_time;\n" +
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
                "void main() {\n" +
                "    vec4 texColor = texture2D(u_texture, v_uv);\n" +
                "    \n" +
                "    if (texColor.a == 0.0) discard;\n" +
                "    \n" +
                "    float timeParam = u_time * 0.1;\n" +
                "    float yPos = v_uv.y+ timeParam;\n" +
                "    float texCoord = cos(texColor.a*6.28);\n" +
                "    \n" +
                "    vec3 noiseCoord = vec3(texColor.a*3.0, yPos,  timeParam);\n" +
                "    float noiseValue = turbulence(noiseCoord);\n" +
                "    \n" +
                "    float edgeSoftness = pow(texColor.a, 0.5);\n" +
                "    float alpha = noiseValue * edgeSoftness;\n" +
                "    vec3 color = vec3(0.8, 0.4, 1.0)*0.1 + vec3(0.8, 0.4, 1.0)* alpha;\n" +
                "    \n" +
                "    gl_FragColor = vec4(color, alpha);\n" +
                "}\n";
            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, vertexSource);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Vertex shader compile failed: " + GL20.glGetShaderInfoLog(vert, 1024));
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, fragmentSource);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Fragment shader compile failed: " + GL20.glGetShaderInfoLog(frag, 1024));
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vert);
            GL20.glAttachShader(shaderProgram, frag);
            GL20.glBindAttribLocation(shaderProgram, 0, "a_position");
            GL20.glBindAttribLocation(shaderProgram, 1, "a_texCoord");
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("Shader program link failed: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);

            System.out.println("LightTextureRender shader compiled OK");
        } catch (Exception e) {
            System.err.println("Shader error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createBuffers() {
        float halfWidth = canvasWidth * 0.5f;
        float halfHeight = canvasHeight * 0.5f;

        FloatBuffer verts = BufferUtils.createFloatBuffer(16);
        verts.put(new float[]{
            -halfWidth, -halfHeight,  0f, 0f,
             halfWidth, -halfHeight,  1f, 0f,
             halfWidth,  halfHeight,  1f, 1f,
            -halfWidth,  halfHeight,  0f, 1f,
        });
        verts.flip();

        IntBuffer indices = BufferUtils.createIntBuffer(6);
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
        uTextureLoc = GL20.glGetUniformLocation(shaderProgram, "u_texture");
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

    private void performDoubleBlur() {
        float blurRadius = 5.0f;
        
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL20.glUseProgram(0);
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo1);
        GL11.glViewport(0, 0, textureWidth, textureHeight);
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(-canvasWidth/2, canvasWidth/2, -canvasHeight/2, canvasHeight/2, -1, 1);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glColorMask(true, true, true, true);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        
        GL20.glUseProgram(blurProgramH);
        GL20.glUniform1f(uBlurRadiusLocH, blurRadius);
        GL20.glUniform2f(uTexSizeLocH, textureWidth, textureHeight);
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL20.glUniform1i(uBlurTexLocH, 0);
        
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
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex1);
        
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL20.glUseProgram(0);
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo2);
        GL11.glViewport(0, 0, textureWidth, textureHeight);
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(-canvasWidth/2, canvasWidth/2, -canvasHeight/2, canvasHeight/2, -1, 1);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glColorMask(true, true, true, true);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        
        GL20.glUseProgram(blurProgramV);
        GL20.glUniform1f(uBlurRadiusLocV, blurRadius);
        GL20.glUniform2f(uTexSizeLocV, textureWidth, textureHeight);
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex1);
        GL20.glUniform1i(uBlurTexLocV, 0);
        
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
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex2);
    }

    @Override
    public void render() {
        if (expired || shaderProgram <= 0 || textureId == 0) return;

        performDoubleBlur();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        GL11.glTranslatef(position.x, position.y, 0f);

        GL20.glUseProgram(shaderProgram);
        GL20.glUniform1f(uTimeLoc, elapsed);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex2);
        GL20.glUniform1i(uTextureLoc, 0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL20.glUseProgram(0);

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
        if (blurProgramH > 0) {
            GL20.glDeleteProgram(blurProgramH);
            blurProgramH = 0;
        }
        if (blurProgramV > 0) {
            GL20.glDeleteProgram(blurProgramV);
            blurProgramV = 0;
        }
        if (fbo1 != 0) {
            GL30.glDeleteFramebuffers(fbo1);
            fbo1 = 0;
        }
        if (fbo2 != 0) {
            GL30.glDeleteFramebuffers(fbo2);
            fbo2 = 0;
        }
        if (tex1 != 0) {
            GL11.glDeleteTextures(tex1);
            tex1 = 0;
        }
        if (tex2 != 0) {
            GL11.glDeleteTextures(tex2);
            tex2 = 0;
        }
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
        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
            textureId = 0;
        }
    }

    public void setDuration(float dur) {
        this.duration = dur;
    }
}
