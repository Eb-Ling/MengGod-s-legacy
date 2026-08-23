package data.scripts.kernel.shader;

import org.apache.log4j.Level;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

import com.fs.starfarer.api.Global;

/**
 * 弹射蒸汽 shader core。
 */
public final class PC_FighterCatapultShaderCore {
    private static boolean initAttempted = false;
    private static boolean disabled = false;

    public static int steamProgram = 0;

    private PC_FighterCatapultShaderCore() {
    }

    public static int getSteamProgram() {
        if (steamProgram != 0 || disabled) {
            return steamProgram;
        }
        if (!initAttempted) {
            init();
        }
        return steamProgram;
    }

    public static void init() {
        initAttempted = true;
        try {
            if (!GLContext.getCapabilities().OpenGL20) {
                disabled = true;
                return;
            }
            steamProgram = createShaderVF("PC_catapult_steam", SteamEffectShader.VERT, SteamEffectShader.FRAG);
            disabled = steamProgram == 0;
        } catch (Throwable ex) {
            disabled = true;
            steamProgram = 0;
            GL20.glUseProgram(0);
            Global.getLogger(PC_FighterCatapultShaderCore.class)
                    .log(Level.ERROR, "PC catapult steam shader disabled.", ex);
        }
    }

    protected static int createShaderVF(String name, String vertSource, String fragSource) {
        int vert = createShader(name + "_vert", vertSource, GL20.GL_VERTEX_SHADER);
        int frag = createShader(name + "_frag", fragSource, GL20.GL_FRAGMENT_SHADER);
        if (vert == 0 || frag == 0) {
            return 0;
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vert);
        GL20.glAttachShader(program, frag);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            Global.getLogger(PC_FighterCatapultShaderCore.class).log(Level.ERROR,
                    name + " shader link failed:\n"
                            + GL20.glGetProgramInfoLog(program, GL20.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH)));
            GL20.glDeleteProgram(program);
            return 0;
        }

        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);
        return program;
    }

    protected static int createShader(String name, String source, int type) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            Global.getLogger(PC_FighterCatapultShaderCore.class).log(Level.ERROR,
                    name + " shader compile failed:\n"
                            + GL20.glGetShaderInfoLog(shader, GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH)));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static final class SteamEffectShader {
        private static final String VERT = """
                #version 110

                varying vec2 v_uv;

                void main() {
                    gl_Position = ftransform();
                    v_uv = gl_MultiTexCoord0.xy;
                }
                """;

        private static final String FRAG = """
                #version 110

                const float INTENSITY = 0.2;
                const float DENSITY = 2.0;

                uniform float u_time;
                uniform float u_progress;
                uniform float u_dissipation;

                varying vec2 v_uv;

                float hash(vec2 p) {
                    vec2 p2 = fract(p * vec2(123.34, 456.21));
                    p2 += dot(p2, p2 + 45.32);
                    return fract(p2.x * p2.y);
                }

                float noise(vec2 p) {
                    vec2 i = floor(p);
                    vec2 f = fract(p);

                    float a = hash(i);
                    float b = hash(i + vec2(1.0, 0.0));
                    float c = hash(i + vec2(0.0, 1.0));
                    float d = hash(i + vec2(1.0, 1.0));

                    vec2 u = f * f * (3.0 - 2.0 * f);

                    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
                }

                float fbm(vec2 p) {
                    float v = 0.1;
                    float a = 0.5;
                    vec2 shift = vec2(100.0, 100.0);
                    int i;
                    for (i = 0; i < 5; i++) {
                        v += a * noise(p);
                        p = p * DENSITY + shift;
                        a *= INTENSITY;
                    }
                    return v;
                }

                void main() {
                    vec2 uv = v_uv.yx*5.0;

                    vec2 warpUV = uv;
                    warpUV.x *= 2.0;
                    float warp = fbm(vec2(uv.x * 2.0 - u_time * 1.5, uv.y * 4.0));

                    vec2 noiseUV = vec2(uv.x*0.5 - u_time * 1.8, uv.y + warp * 0.5);
                    float density = fbm(noiseUV);

                    float edgeMask = smoothstep(-0.9, 0.9, uv.x) *
                                    smoothstep(5.0, 1.0, uv.x) *
                                    smoothstep(5.0, 3.0, uv.y) *
                                    smoothstep(0.0, u_progress*2.0, uv.y);

                    float dissolve = smoothstep(u_dissipation, u_dissipation + 0.5, density);

                    float progressMask = smoothstep(u_progress, u_progress - 0.1, uv.y/5.0);

                    float finalAlpha = density * edgeMask * progressMask * dissolve * 3.0;

                    vec3 steamColor = vec3(0.98, 0.99, 1.0);

                    gl_FragColor = vec4(steamColor, finalAlpha);
                }
                """;
    }
}
