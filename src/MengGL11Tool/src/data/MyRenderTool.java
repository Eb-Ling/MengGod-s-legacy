package data;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import data.HexGridRenderEffect;
import javax.imageio.ImageIO;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MyRenderTool {
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final float DEFAULT_ZOOM = 1.0f;
    private static final float ZOOM_SPEED = 0.1f;
    private static final float PAN_SPEED = 5.0f;
    private static final String FONT_RELATIVE_PATH = "font/insignia25LTaa.fnt";

    private Vector2f cameraPosition;
    private float cameraZoom;
    private List<MyRender> activeEffects;
    private List<Class<? extends MyRender>> availableRenderClasses;
    private Class<? extends MyRender> currentEffectClass;
    private int currentRenderIndex;
    private boolean isRunning;
    private Map<Character, CharacterData> fontCharacters;
    private int fontTextureId;
    private int fontTextureWidth;
    private int fontTextureHeight;
    private float fontLineHeight;

    private boolean editMode;
    private List<Vector2f> quadVertices;
    private int gridRows;
    private int gridCols;
    private int quadTextureId;
    private int quadTextureWidth;
    private int quadTextureHeight;
    private boolean showGridInput;
    private StringBuilder inputBuffer;
    private String texturePathInput;
    private boolean addVertexMode;
    private int inputStep;
    private float textureRotation;
    private boolean textureFlowEnabled;
    private float textureFlowOffset;
    private int flowDirection;
    private boolean showSpeedInput;
    private float flowSpeedMultiplier;
    private boolean spawnCurrentOnStart;
    
    private boolean circleEditMode;
    private Vector2f circleCenter;
    private float circleRadius;
    private int circleSegments;
    private int circleTextureId;
    private int circleTextureWidth;
    private int circleTextureHeight;
    private float circleTextureRotation;
    private boolean circleTextureExpand;
    private float circleExpandOffset;
    private boolean circleAutoRotate;
    private float circleRotateSpeed;
    private float circleExpandSpeed;

    private static class CharacterData {
        int x, y, width, height, xoffset, yoffset, xadvance;

        CharacterData(int x, int y, int w, int h, int xo, int yo, int xa) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.xoffset = xo;
            this.yoffset = yo;
            this.xadvance = xa;
        }
    }


    public void scanAndLoadRenderClasses() {
        String packageName = "data";
        String path = packageName.replace('.', '/');

        System.out.println("开始扫描包: " + packageName);
        System.out.println("========================================");

        try {
            System.out.println("[步骤1] 加载外部渲染类...");
            loadExternalRenderClasses();
            System.out.println("[步骤1] 完成，当前找到 " + availableRenderClasses.size() + " 个类");

            System.out.println("\n[步骤2] 扫描ClassLoader资源...");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(path);

            int resourceCount = 0;
            while (resources.hasMoreElements()) {
                resourceCount++;
                URL resource = resources.nextElement();
                System.out.println("找到资源[" + resourceCount + "]: " + resource.toString());

                String protocol = resource.getProtocol();

                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(resource.getFile(), "UTF-8");
                    File dir = new File(filePath);
                    System.out.println("扫描目录: " + dir.getAbsolutePath());
                    System.out.println("目录存在: " + dir.exists());
                    scanDirectory(dir, packageName);
                } else if ("jar".equals(protocol)) {
                    System.out.println("扫描JAR: " + resource);
                    scanJar(resource, packageName);
                }
            }

            if (resourceCount == 0) {
                System.err.println("警告: ClassLoader未找到资源，尝试直接扫描...");
                scanFallbackDirectories();
            }

            System.out.println("\n========================================");
            System.out.println("✓ 总共找到 " + availableRenderClasses.size() + " 个渲染类:");
            for (int i = 0; i < availableRenderClasses.size(); i++) {
                String className = availableRenderClasses.get(i).getSimpleName();
                System.out.println("  [" + (i + 1) + "] " + className);
            }

            if (!availableRenderClasses.isEmpty()) {
                loadCurrentRenderClass();
            } else {
                System.err.println("错误: 未找到任何渲染类！");
            }

        } catch (Exception e) {
            System.err.println("扫描渲染类失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void loadExternalRenderClasses() {
        try {
            File dataDir = new File("data");

            System.out.println("  [外部加载] 检查data目录: " + dataDir.getAbsolutePath());
            System.out.println("  [外部加载] data目录存在: " + dataDir.exists());

            if (!dataDir.exists()) {
                System.out.println("  [外部加载] 外部data目录不存在，尝试创建...");
                boolean created = dataDir.mkdir();
                if (!created) {
                    System.err.println("  [外部加载] 无法创建data目录");
                    return;
                }
                System.out.println("  [外部加载] 已创建data目录");
            }

            System.out.println("  [外部加载] 开始编译Java文件...");
            compileJavaFiles(dataDir);

            File[] classFiles = dataDir.listFiles((dir, name) ->
                    name.endsWith(".class") && !name.contains("$")
            );

            if (classFiles == null) {
                System.err.println("  [外部加载] 无法列出data目录中的文件");
                return;
            }

            System.out.println("  [外部加载] 找到 " + classFiles.length + " 个.class文件");

            if (classFiles.length == 0) {
                System.out.println("  [外部加载] 没有可用的类文件");
                return;
            }

            URL[] urls = {dataDir.toURI().toURL()};
            URLClassLoader externalClassLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

            int loadedCount = 0;
            for (File classFile : classFiles) {
                String className = classFile.getName().replace(".class", "");
                String fullClassName = "data." + className;

                try {
                    System.out.println("  [外部加载] 尝试加载: " + fullClassName);
                    Class<?> clazz = externalClassLoader.loadClass(fullClassName);

                    if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                        System.out.println("  [外部加载]   ✗ 跳过 - 是接口或抽象类");
                        continue;
                    }

                    if (!MyRender.class.isAssignableFrom(clazz)) {
                        System.out.println("  [外部加载]   ✗ 跳过 - 不实现MyRender接口");
                        continue;
                    }

                    try {
                        clazz.getDeclaredConstructor();
                    } catch (NoSuchMethodException e) {
                        System.out.println("  [外部加载]   ✗ 跳过 - 无默认构造函数");
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Class<? extends MyRender> renderClass = (Class<? extends MyRender>) clazz;

                    availableRenderClasses.add(renderClass);
                    loadedCount++;
                    System.out.println("  [外部加载]   ✓ 成功加载: " + className);

                } catch (ClassNotFoundException e) {
                    System.err.println("  [外部加载]   ✗ 无法加载: " + className + " - " + e.getMessage());
                } catch (NoClassDefFoundError e) {
                    System.err.println("  [外部加载]   ✗ 缺少依赖: " + className + " - " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("  [外部加载]   ✗ 错误: " + className + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }

            externalClassLoader.close();
            System.out.println("  [外部加载] 完成，成功加载 " + loadedCount + " 个类");

        } catch (Exception e) {
            System.err.println("  [外部加载] 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void scanJar(URL jarUrl, String packageName) {
        try {
            String jarPath = jarUrl.getPath().substring(5, jarUrl.getPath().indexOf("!"));
            jarPath = URLDecoder.decode(jarPath, "UTF-8");

            System.out.println("  JAR路径: " + jarPath);

            JarFile jarFile = new JarFile(jarPath);

            Enumeration<JarEntry> entries = jarFile.entries();
            String packagePath = packageName.replace('.', '/') + "/";

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith(packagePath) &&
                        entryName.endsWith(".class") &&
                        !entryName.contains("$")) {

                    String className = entryName.substring(packagePath.length(), entryName.length() - 6);
                    String fullClassName = packageName + "." + className;

                    try {
                        Class<?> clazz = Class.forName(fullClassName);

                        if (MyRender.class.isAssignableFrom(clazz) &&
                                !clazz.isInterface() &&
                                !Modifier.isAbstract(clazz.getModifiers())) {

                            @SuppressWarnings("unchecked")
                            Class<? extends MyRender> renderClass = (Class<? extends MyRender>) clazz;
                            availableRenderClasses.add(renderClass);
                            System.out.println("  ✓ JAR加载: " + className);
                        }
                    } catch (ClassNotFoundException e) {
                    }
                }
            }

            jarFile.close();
        } catch (Exception e) {
            System.err.println("扫描JAR文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void compileJavaFiles(File sourceDir) {
        try {
            File[] javaFiles = sourceDir.listFiles((dir, name) ->
                    name.endsWith(".java") && !name.startsWith(".")
            );

            if (javaFiles == null || javaFiles.length == 0) {
                System.out.println("    [编译] 外部data目录中没有找到.java源文件");
                return;
            }

            System.out.println("    [编译] 发现 " + javaFiles.length + " 个Java源文件");

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                System.err.println("    [编译] 警告: 未找到Java编译器，请确保使用的是JDK而不是JRE");
                System.err.println("    [编译] 将尝试使用已有的.class文件");
                return;
            }

            System.out.println("    [编译] Java编译器可用");

            List<String> options = new ArrayList<>();

            options.add("-d");
            options.add(sourceDir.getAbsolutePath());
            options.add("-cp");

            StringBuilder classpath = new StringBuilder();
            classpath.append(sourceDir.getAbsolutePath());

            String jarPath = new File("MengGL11Tool.jar").getAbsolutePath();
            classpath.append(File.pathSeparator).append(jarPath);
            System.out.println("    [编译] JAR路径: " + jarPath);

            File libDir = new File("lib");
            if (libDir.exists()) {
                File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
                if (jars != null) {
                    for (File jar : jars) {
                        classpath.append(File.pathSeparator).append(jar.getAbsolutePath());
                        System.out.println("    [编译] 添加lib: " + jar.getName());
                    }
                }
            }

            String existingClasspath = System.getProperty("java.class.path");
            if (existingClasspath != null && !existingClasspath.isEmpty()) {
                classpath.append(File.pathSeparator).append(existingClasspath);
            }

            options.add(classpath.toString());
            options.add("-encoding");
            options.add("UTF-8");

            for (File javaFile : javaFiles) {
                options.add(javaFile.getAbsolutePath());
                System.out.println("    [编译] 准备编译: " + javaFile.getName());
            }

            System.out.println("    [编译] 开始编译...");
            int result = compiler.run(null, null, null, options.toArray(new String[0]));

            if (result == 0) {
                System.out.println("    [编译] ✓ 编译成功！");
                System.out.println("    [编译] 输出目录: " + sourceDir.getAbsolutePath());

                for (File javaFile : javaFiles) {
                    String className = javaFile.getName().replace(".java", "");
                    File classFile = new File(sourceDir, className + ".class");
                    if (classFile.exists()) {
                        System.out.println("    [编译] 生成: " + className + ".class (" + classFile.length() + " bytes)");
                    } else {
                        System.err.println("    [编译] 警告: 未找到生成的类文件 " + className + ".class");
                    }
                }
            } else {
                System.err.println("    [编译] ✗ 编译失败，错误码: " + result);
            }

        } catch (Exception e) {
            System.err.println("    [编译] 异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public MyRenderTool() {
        cameraPosition = new Vector2f(0f, 0f);
        cameraZoom = DEFAULT_ZOOM;
        activeEffects = new ArrayList<>();
        availableRenderClasses = new ArrayList<>();
        currentEffectClass = null;
        currentRenderIndex = 0;
        isRunning = false;
        fontCharacters = new HashMap<>();
        fontTextureId = 0;

        editMode = false;
        quadVertices = new ArrayList<>();
        gridRows = 4;
        gridCols = 4;
        quadTextureId = 0;
        showGridInput = false;
        inputBuffer = new StringBuilder();
        texturePathInput = "";
        addVertexMode = false;
        inputStep = 0;
        textureRotation = 0f;
        textureFlowEnabled = false;
        textureFlowOffset = 0f;
        flowDirection = 0;
        showSpeedInput = false;
        flowSpeedMultiplier = 1f;
        spawnCurrentOnStart = false;
        
        circleEditMode = false;
        circleCenter = new Vector2f(0f, 0f);
        circleRadius = 100f;
        circleSegments = 32;
        circleTextureId = 0;
        circleTextureRotation = 0f;
        circleTextureExpand = false;
        circleExpandOffset = 0f;
        circleAutoRotate = false;
        circleRotateSpeed = 1f;
        circleExpandSpeed = 0.02f;
        
        loadFontData();

    }

    private void loadFontData() {
        try {
            File fontFile = findFontFile();

            if (fontFile == null || !fontFile.exists()) {
                System.err.println("字体文件不存在");
                return;
            }

            System.out.println("正在加载字体数据: " + fontFile.getAbsolutePath());

            BufferedReader reader = new BufferedReader(new FileReader(fontFile));
            String line;
            int charCount = 0;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("char ")) {
                    parseCharLine(line);
                    charCount++;
                } else if (line.startsWith("common ")) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("lineHeight=")) {
                            fontLineHeight = Float.parseFloat(part.split("=")[1]);
                        }
                    }
                }
            }
            reader.close();

            System.out.println("✓ 字体数据加载成功");
            System.out.println("  解析了 " + charCount + " 个字符定义");
            System.out.println("  实际加载了 " + fontCharacters.size() + " 个字符");
            System.out.println("  行高: " + fontLineHeight);

        } catch (Exception e) {
            System.err.println("字体数据加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private File findFontFile() {
        String[] possiblePaths = {
                "font/insignia25LTaa.fnt",
                "src/font/insignia25LTaa.fnt",
                "MengGL11Tool/src/font/insignia25LTaa.fnt"
        };

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                return file;
            }
        }

        return null;
    }

    private void loadFontTexture() {
        if (fontCharacters.isEmpty()) {
            System.err.println("无法加载纹理：字体数据为空");
            return;
        }

        try {
            File textureFile = findFontTextureFile();

            if (textureFile == null || !textureFile.exists()) {
                System.err.println("字体纹理文件不存在");
                return;
            }

            System.out.println("正在加载纹理: " + textureFile.getAbsolutePath());

            BufferedImage image = ImageIO.read(textureFile);

            if (image == null) {
                System.err.println("无法读取图片文件");
                return;
            }

            fontTextureWidth = image.getWidth();
            fontTextureHeight = image.getHeight();

            System.out.println("图片尺寸: " + fontTextureWidth + "x" + fontTextureHeight);

            int[] pixels = new int[fontTextureWidth * fontTextureHeight];
            image.getRGB(0, 0, fontTextureWidth, fontTextureHeight, pixels, 0, fontTextureWidth);

            ByteBuffer buffer = BufferUtils.createByteBuffer(fontTextureWidth * fontTextureHeight * 4);

            for (int y = 0; y < fontTextureHeight; y++) {
                for (int x = 0; x < fontTextureWidth; x++) {
                    int pixel = pixels[y * fontTextureWidth + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }

            buffer.flip();

            fontTextureId = GL11.glGenTextures();

            if (fontTextureId <= 0) {
                System.err.println("纹理ID生成失败！");
                return;
            }

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureId);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    fontTextureWidth, fontTextureHeight, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR) {
                System.err.println("OpenGL错误: " + error);
                return;
            }

            System.out.println("✓ 字体纹理加载成功!");
            System.out.println("  纹理ID: " + fontTextureId);
            System.out.println("  纹理尺寸: " + fontTextureWidth + "x" + fontTextureHeight);

        } catch (Exception e) {
            System.err.println("字体纹理加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private File findFontTextureFile() {
        String[] possiblePaths = {
                "font/insignia25LTaa_0.png",
                "src/font/insignia25LTaa_0.png",
                "MengGL11Tool/src/font/insignia25LTaa_0.png"
        };

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                return file;
            }
        }

        return null;
    }



    private void parseCharLine(String line) {
        int id = 0, x = 0, y = 0, width = 0, height = 0, xoffset = 0, yoffset = 0, xadvance = 0;

        String[] parts = line.split(" ");
        for (String part : parts) {
            if (part.startsWith("id=")) {
                id = Integer.parseInt(part.split("=")[1]);
            } else if (part.startsWith("x=")) {
                x = Integer.parseInt(part.split("=")[1]);
            } else if (part.startsWith("y=")) {
                y = Integer.parseInt(part.split("=")[1]);
            } else if (part.startsWith("width=")) {
                width = Integer.parseInt(part.split("=")[1]);
            } else if (part.startsWith("height=")) {
                height = Integer.parseInt(part.split("=")[1]);
            } else if (part.startsWith("xoffset=")) {
                xoffset = Integer.parseInt(part.split("=")[1]);
            } else if (part.startsWith("yoffset=")) {
                yoffset = Integer.parseInt(part.split("=")[1]);
            } else if (part.startsWith("xadvance=")) {
                xadvance = Integer.parseInt(part.split("=")[1]);
            }
        }

        fontCharacters.put((char) id, new CharacterData(x, y, width, height, xoffset, yoffset, xadvance));
    }

    public void switchToNextRender() {
        if (availableRenderClasses.isEmpty()) return;

        currentRenderIndex = (currentRenderIndex + 1) % availableRenderClasses.size();
        loadCurrentRenderClass();
    }

    public void switchToPreviousRender() {
        if (availableRenderClasses.isEmpty()) return;

        currentRenderIndex = (currentRenderIndex - 1 + availableRenderClasses.size()) % availableRenderClasses.size();
        loadCurrentRenderClass();
    }

    public boolean switchToRender(String simpleName) {
        if (simpleName == null || availableRenderClasses.isEmpty()) return false;
        for (int i = 0; i < availableRenderClasses.size(); i++) {
            if (simpleName.equals(availableRenderClasses.get(i).getSimpleName())) {
                currentRenderIndex = i;
                loadCurrentRenderClass();
                return true;
            }
        }
        return false;
    }

    public void setSpawnCurrentOnStart(boolean spawnCurrentOnStart) {
        this.spawnCurrentOnStart = spawnCurrentOnStart;
    }

    private void loadCurrentRenderClass() {
        if (currentRenderIndex < 0 || currentRenderIndex >= availableRenderClasses.size()) return;

        try {
            Class<? extends MyRender> renderClass = availableRenderClasses.get(currentRenderIndex);
            currentEffectClass = renderClass;
            activeEffects.clear();

            System.out.println("已切换到: " + renderClass.getSimpleName() +
                    " (" + (currentRenderIndex + 1) + "/" + availableRenderClasses.size() + ")");
        } catch (Exception e) {
            System.err.println("加载效果类失败: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void scanFallbackDirectories() {
        String[] possiblePaths = {
                "out/production/MengGL11Tool/data",
                "bin/data",
                "build/classes/data"
        };

        for (String pathStr : possiblePaths) {
            File dir = new File(pathStr);
            System.out.println("尝试路径: " + dir.getAbsolutePath());
            if (dir.exists()) {
                System.out.println("  ✓ 找到目录，开始扫描");
                scanDirectory(dir, "data");
                if (!availableRenderClasses.isEmpty()) {
                    return;
                }
            } else {
                System.out.println("  ✗ 目录不存在");
            }
        }
    }

    private void scanDirectory(File directory, String packageName) {
        if (!directory.exists()) {
            System.err.println("目录不存在: " + directory.getAbsolutePath());
            return;
        }

        System.out.println("正在扫描: " + directory.getAbsolutePath());

        File[] files = directory.listFiles(file -> {
            return file.isFile() && file.getName().endsWith(".class");
        });

        if (files == null || files.length == 0) {
            System.out.println("  未找到.class文件");
            return;
        }

        System.out.println("  找到 " + files.length + " 个类文件");

        for (File file : files) {
            String className = file.getName().replace(".class", "");

            if (className.contains("$")) continue;

            String fullClassName = packageName + "." + className;

            try {
                Class<?> clazz = Class.forName(fullClassName);

                if (clazz.isInterface()) continue;
                if (Modifier.isAbstract(clazz.getModifiers())) continue;
                if (!MyRender.class.isAssignableFrom(clazz)) continue;

                @SuppressWarnings("unchecked")
                Class<? extends MyRender> renderClass = (Class<? extends MyRender>) clazz;

                try {
                    renderClass.getDeclaredConstructor();
                    availableRenderClasses.add(renderClass);
                    System.out.println("  ✓ 添加: " + className);
                } catch (NoSuchMethodException e) {
                    System.out.println("  ✗ 跳过 " + className + " - 无默认构造函数");
                }
            } catch (ClassNotFoundException e) {
                System.err.println("  ✗ 无法加载: " + className);
            } catch (NoClassDefFoundError e) {
                System.err.println("  ✗ 缺少依赖: " + className + " - " + e.getMessage());
            } catch (Exception e) {
                System.err.println("  ✗ 错误: " + className + " - " + e.getMessage());
            }
        }
    }


    public Vector2f getCameraPosition() {
        return new Vector2f(cameraPosition);
    }

    public void setCameraPosition(float x, float y) {
        cameraPosition.x = x;
        cameraPosition.y = y;
    }

    public void moveCamera(float deltaX, float deltaY) {
        cameraPosition.x += deltaX;
        cameraPosition.y += deltaY;
    }

    public float getCameraZoom() {
        return cameraZoom;
    }

    public void setCameraZoom(float zoom) {
        cameraZoom = Math.max(0.1f, Math.min(zoom, 10.0f));
    }

    public void zoomIn() {
        cameraZoom = Math.min(cameraZoom + ZOOM_SPEED, 10.0f);
    }

    public void zoomOut() {
        cameraZoom = Math.max(cameraZoom - ZOOM_SPEED, 0.1f);
    }

    public void start() {
        try {
            initDisplay();
            initGL();
            if (spawnCurrentOnStart && activeEffects.isEmpty() && currentEffectClass != null) {
                spawnEffect(0f, 0f);
            }
            isRunning = true;
            mainLoop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    public void stop() {
        isRunning = false;
    }

    private void initDisplay() throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(WINDOW_WIDTH, WINDOW_HEIGHT));
        Display.setTitle("MyRender Tool - 2D Rendering Test");
        Display.create();

        loadFontTexture();
    }

    private void initGL() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, WINDOW_WIDTH, WINDOW_HEIGHT, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void mainLoop() {
        long lastTime = System.nanoTime();

        while (isRunning && !Display.isCloseRequested()) {
            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - lastTime) / 1_000_000_000f;
            lastTime = currentTime;

            handleInput();
            updateMouseAwareEffects();
            advanceAll(deltaTime);
            render();
            Display.update();
            Display.sync(60);
        }
    }

    private void advanceAll(float deltaTime) {
        Iterator<MyRender> iterator = activeEffects.iterator();
        while (iterator.hasNext()) {
            MyRender effect = iterator.next();
            effect.advance(deltaTime);
            if (effect.isExpired()) {
                iterator.remove();
            }
        }
    }

    private void updateMouseAwareEffects() {
        int mouseX = Mouse.getX() * WINDOW_WIDTH / Display.getWidth();
        int mouseY = WINDOW_HEIGHT - Mouse.getY() * WINDOW_HEIGHT / Display.getHeight();
        Vector2f worldPos = screenToWorld(mouseX, mouseY);
        for (MyRender effect : activeEffects) {
            if (effect instanceof MouseAwareEffect) {
                ((MouseAwareEffect) effect).setMousePosition(worldPos.x, worldPos.y);
            }
        }
    }

    private void handleInput() {
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey();

                if (key == Keyboard.KEY_F2) {
                    editMode = !editMode;
                    circleEditMode = false;
                    if (editMode) {
                        System.out.println("进入编辑模式 - QUAD_STRIP");
                    } else {
                        System.out.println("退出编辑模式");
                    }
                    return;
                }

                if (key == Keyboard.KEY_F3) {
                    circleEditMode = !circleEditMode;
                    editMode = false;
                    if (circleEditMode) {
                        System.out.println("进入圆形编辑模式");
                    } else {
                        System.out.println("退出圆形编辑模式");
                    }
                    return;
                }

                if (key == Keyboard.KEY_K) {
                    int mouseX = Mouse.getX() * WINDOW_WIDTH / Display.getWidth();
                    int mouseY = WINDOW_HEIGHT - Mouse.getY() * WINDOW_HEIGHT / Display.getHeight();
                    Vector2f worldPos = screenToWorld(mouseX, mouseY);

                    for (MyRender effect : activeEffects) {
                        if (effect instanceof HexGridRenderEffect) {
                            HexGridRenderEffect hex = (HexGridRenderEffect) effect;
                            Vector2f pos = hex.getPosition();
                            float uvX = (worldPos.x - pos.x + 128f) / 256f;
                            float uvY = (worldPos.y - pos.y + 128f) / 256f;
                            hex.triggerWave(uvX, uvY);
                        }
                    }
                    return;
                }
            }
        }

        if (editMode) {
            handleEditModeInput();
            return;
        }

        if (circleEditMode) {
            handleCircleEditModeInput();
            return;
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_W) || Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            moveCamera(0, -PAN_SPEED / cameraZoom);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_S) || Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            moveCamera(0, PAN_SPEED / cameraZoom);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_A) || Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
            moveCamera(-PAN_SPEED / cameraZoom, 0);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_D) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            moveCamera(PAN_SPEED / cameraZoom, 0);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
            zoomOut();
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
            zoomIn();
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
            cameraPosition.x = 0f;
            cameraPosition.y = 0f;
            cameraZoom = DEFAULT_ZOOM;
        }

        while (Mouse.next()) {
            int wheelDelta = Mouse.getEventDWheel();
            if (wheelDelta != 0) {
                if (wheelDelta > 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
            }

            if (Mouse.getEventButtonState()) {
                int button = Mouse.getEventButton();
                if (button == 0) {
                    int mouseX = Mouse.getEventX() * WINDOW_WIDTH / Display.getWidth();
                    int mouseY = WINDOW_HEIGHT - Mouse.getEventY() * WINDOW_HEIGHT / Display.getHeight();
                    Vector2f worldPos = screenToWorld(mouseX, mouseY);

                    spawnEffect(worldPos.x, worldPos.y);
                } else if (button == 1) {
                    switchToNextRender();
                }
            }
        }
    }

    public void spawnEffect(float x, float y) {
        if (currentEffectClass == null) {
            System.err.println("未设置效果类");
            return;
        }

        try {
            MyRender effect = currentEffectClass.getDeclaredConstructor().newInstance();

            if (effect instanceof InitializableEffect) {
                ((InitializableEffect) effect).initialize(x, y);
            }

            activeEffects.add(effect);
        } catch (Exception e) {
            System.err.println("生成效果失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleCircleEditModeInput() {
        if (showGridInput || showSpeedInput) {
            while (Keyboard.next()) {
                if (Keyboard.getEventKeyState()) {
                    int key = Keyboard.getEventKey();

                    if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_ESCAPE) {
                        if (showSpeedInput) {
                            processCircleSpeedInput();
                        } else {
                            processGridInput();
                        }
                        return;
                    } else if (key == Keyboard.KEY_BACK) {
                        if (inputBuffer.length() > 0) {
                            inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                        }
                    } else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                        if (key == Keyboard.KEY_V) {
                            pasteFromClipboard();
                        }
                    } else {
                        char c = Keyboard.getEventCharacter();
                        if (showSpeedInput) {
                            if ((c >= '0' && c <= '9') || c == '.' || c == '-') {
                                inputBuffer.append(c);
                            }
                        } else {
                            if (c >= 32 && c <= 126) {
                                inputBuffer.append(c);
                            }
                        }
                    }
                }
            }
            return;
        }

        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey();

                if (key == Keyboard.KEY_I) {
                    showGridInput = true;
                    inputBuffer.setLength(0);
                    texturePathInput = "";
                    inputStep = 0;
                    System.out.println("请输入圆形纹理路径（支持绝对路径或相对路径）:");
                } else if (key == Keyboard.KEY_R) {
                    circleAutoRotate = !circleAutoRotate;
                    System.out.println("自动旋转: " + (circleAutoRotate ? "开启" : "关闭"));
                } else if (key == Keyboard.KEY_O) {
                    circleTextureExpand = !circleTextureExpand;
                    System.out.println("纹理扩散: " + (circleTextureExpand ? "开启" : "关闭"));
                } else if (key == Keyboard.KEY_C) {
                    circleCenter.x = 0f;
                    circleCenter.y = 0f;
                    circleRadius = 100f;
                    System.out.println("已重置圆形");
                } else if (key == Keyboard.KEY_S) {
                    showSpeedInput = true;
                    inputBuffer.setLength(0);
                    System.out.println("请输入速度 (旋转/扩散, 当前旋转: " + circleRotateSpeed + "°/帧, 扩散: " + (circleExpandSpeed * 100) + "%/帧):");
                }
            }
        }

        while (Mouse.next()) {
            if (Mouse.getEventButtonState()) {
                int button = Mouse.getEventButton();
                int mouseX = Mouse.getEventX() * WINDOW_WIDTH / Display.getWidth();
                int mouseY = WINDOW_HEIGHT - Mouse.getEventY() * WINDOW_HEIGHT / Display.getHeight();
                Vector2f worldPos = screenToWorld(mouseX, mouseY);

                if (button == 0) {
                    circleCenter.x = worldPos.x;
                    circleCenter.y = worldPos.y;
                    System.out.println("设置圆心: (" + worldPos.x + ", " + worldPos.y + ")");
                } else if (button == 1) {
                    float dist = (float) Math.sqrt(
                            Math.pow(worldPos.x - circleCenter.x, 2) +
                                    Math.pow(worldPos.y - circleCenter.y, 2)
                    );
                    circleRadius = dist;
                    System.out.println("设置半径: " + (int)circleRadius);
                }
            }
        }
    }

    private void processCircleSpeedInput() {
        String input = inputBuffer.toString().trim();

        if (input.isEmpty() || Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            showSpeedInput = false;
            inputBuffer.setLength(0);
            return;
        }

        try {
            float speed = Float.parseFloat(input);
            if (speed > 0) {
                circleRotateSpeed = speed;
                circleExpandSpeed = speed * 0.02f;
                System.out.println("✓ 旋转速度: " + speed + "°/帧, 扩散速度: " + (speed * 2) + "%/帧");
            } else {
                System.err.println("✗ 速度必须大于0");
            }
        } catch (NumberFormatException e) {
            System.err.println("✗ 无效的数字格式");
        }

        showSpeedInput = false;
        inputBuffer.setLength(0);
    }



    private void handleEditModeInput() {
        if (showGridInput || showSpeedInput) {
            while (Keyboard.next()) {
                if (Keyboard.getEventKeyState()) {
                    int key = Keyboard.getEventKey();

                    if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_ESCAPE) {
                        if (showSpeedInput) {
                            processSpeedInput();
                        } else {
                            processGridInput();
                        }
                        return;
                    } else if (key == Keyboard.KEY_BACK) {
                        if (inputBuffer.length() > 0) {
                            inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                        }
                    } else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                        if (key == Keyboard.KEY_V) {
                            pasteFromClipboard();
                        }
                    } else {
                        char c = Keyboard.getEventCharacter();
                        if (showSpeedInput) {
                            if ((c >= '0' && c <= '9') || c == '.' || c == '-') {
                                inputBuffer.append(c);
                            }
                        } else {
                            if (c >= 32 && c <= 126) {
                                inputBuffer.append(c);
                            }
                        }
                    }
                }
            }
            return;
        }
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey();

                if (key == Keyboard.KEY_I) {
                    showGridInput = true;
                    inputBuffer.setLength(0);
                    texturePathInput = "";
                    inputStep = 0;
                    System.out.println("请输入纹理路径（支持绝对路径或相对路径）:");
                } else if (key == Keyboard.KEY_V) {
                    addVertexMode = !addVertexMode;
                    System.out.println("顶点添加模式: " + (addVertexMode ? "开启" : "关闭"));
                } else if (key == Keyboard.KEY_C) {
                    quadVertices.clear();
                    System.out.println("已清空所有顶点");
                } else if (key == Keyboard.KEY_L) {
                    textureRotation = (textureRotation + 90f) % 360f;
                    System.out.println("纹理旋转: " + (int)textureRotation + "°");
                } else if (key == Keyboard.KEY_P) {
                    textureFlowEnabled = !textureFlowEnabled;
                    System.out.println("纹理流动: " + (textureFlowEnabled ? "开启" : "关闭"));
                } else if (key == Keyboard.KEY_O) {
                    flowDirection = (flowDirection + 1) % 4;
                    String[] directions = {"正向", "向右", "反向", "向左"};
                    System.out.println("流动方向: " + directions[flowDirection]);
                } else if (key == Keyboard.KEY_S) {
                    showSpeedInput = true;
                    inputBuffer.setLength(0);
                    System.out.println("请输入流动速度倍数 (当前: " + flowSpeedMultiplier + "x):");
                }
            }
        }

        while (Mouse.next()) {
            if (Mouse.getEventButtonState()) {
                int button = Mouse.getEventButton();
                int mouseX = Mouse.getEventX() * WINDOW_WIDTH / Display.getWidth();
                int mouseY = WINDOW_HEIGHT - Mouse.getEventY() * WINDOW_HEIGHT / Display.getHeight();
                Vector2f worldPos = screenToWorld(mouseX, mouseY);

                if (button == 0 && addVertexMode) {
                    quadVertices.add(new Vector2f(worldPos));
                    int pairNum = (quadVertices.size() + 1) / 2;
                    System.out.println("添加顶点 #" + quadVertices.size() + " (" + worldPos.x + ", " + worldPos.y + ")");
                    if (quadVertices.size() % 2 == 0) {
                        System.out.println("  ✓ 完成第 " + pairNum + " 对顶点，可以渲染飘带段");
                    } else {
                        System.out.println("  → 请继续添加右侧顶点");
                    }
                } else if (button == 1) {
                    if (!quadVertices.isEmpty()) {
                        Vector2f removed = quadVertices.remove(quadVertices.size() - 1);
                        System.out.println("删除顶点 #" + (quadVertices.size() + 1) + "，剩余: " + quadVertices.size());
                    }
                }
            }
        }
    }

    private void processSpeedInput() {
        String input = inputBuffer.toString().trim();

        if (input.isEmpty() || Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            showSpeedInput = false;
            inputBuffer.setLength(0);
            return;
        }

        try {
            float speed = Float.parseFloat(input);
            if (speed > 0) {
                flowSpeedMultiplier = speed;
                System.out.println("✓ 流动速度设置为: " + speed + "x");
            } else {
                System.err.println("✗ 速度必须大于0");
            }
        } catch (NumberFormatException e) {
            System.err.println("✗ 无效的数字格式");
        }

        showSpeedInput = false;
        inputBuffer.setLength(0);
    }


    private void pasteFromClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            
            if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                System.out.println("剪贴板中没有文本内容");
                return;
            }
            
            String data = (String) clipboard.getData(DataFlavor.stringFlavor);
            
            if (data != null && !data.isEmpty()) {
                inputBuffer.append(data);
                System.out.println("已粘贴: " + data);
            }
        } catch (IllegalStateException e) {
            System.err.println("无法访问剪贴板: " + e.getMessage());
        } catch (Exception e) {
            if (!e.getClass().getName().contains("ClassNotFoundException")) {
                System.err.println("粘贴失败: " + e.getMessage());
            }
        }
    }

    private void processGridInput() {
        String input = inputBuffer.toString().trim();
        
        if (input.isEmpty() || Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            showGridInput = false;
            inputBuffer.setLength(0);
            inputStep = 0;
            return;
        }
        
        if (inputStep == 0) {
            texturePathInput = input;
            if (circleEditMode) {
                loadCircleTexture(texturePathInput);
            } else {
                loadQuadTexture(texturePathInput);
            }
            showGridInput = false;
            inputBuffer.setLength(0);
            inputStep = 0;
        }
    }

    private void loadQuadTexture(String path) {
        loadTextureToId(path, (textureId, width, height) -> {
            if (quadTextureId > 0) {
                GL11.glDeleteTextures(quadTextureId);
            }
            quadTextureId = textureId;
            quadTextureWidth = width;
            quadTextureHeight = height;
        });
    }

    private void loadCircleTexture(String path) {
        loadTextureToId(path, (textureId, width, height) -> {
            if (circleTextureId > 0) {
                GL11.glDeleteTextures(circleTextureId);
            }
            circleTextureId = textureId;
            circleTextureWidth = width;
            circleTextureHeight = height;
        });
    }

    @FunctionalInterface
    private interface TextureLoadCallback {
        void onLoad(int textureId, int width, int height);
    }

    private void loadTextureToId(String path, TextureLoadCallback callback) {
        try {
            File textureFile = new File(path);

            if (!textureFile.exists()) {
                System.err.println("纹理文件不存在: " + textureFile.getAbsolutePath());
                return;
            }

            System.out.println("加载纹理: " + textureFile.getAbsolutePath());

            BufferedImage image = ImageIO.read(textureFile);

            if (image == null) {
                System.err.println("无法读取图片文件");
                return;
            }

            int texWidth = image.getWidth();
            int texHeight = image.getHeight();

            int[] pixels = new int[texWidth * texHeight];
            image.getRGB(0, 0, texWidth, texHeight, pixels, 0, texWidth);

            ByteBuffer buffer = BufferUtils.createByteBuffer(texWidth * texHeight * 4);

            for (int y = 0; y < texHeight; y++) {
                for (int x = 0; x < texWidth; x++) {
                    int pixel = pixels[y * texWidth + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }

            buffer.flip();

            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    texWidth, texHeight, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            System.out.println("✓ 纹理加载成功: " + texWidth + "x" + texHeight);

            callback.onLoad(textureId, texWidth, texHeight);

        } catch (Exception e) {
            System.err.println("纹理加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void render() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glLoadIdentity();

        GL11.glTranslatef(WINDOW_WIDTH / 2f, WINDOW_HEIGHT / 2f, 0f);
        GL11.glScalef(cameraZoom, cameraZoom, 1f);
        GL11.glTranslatef(-cameraPosition.x, -cameraPosition.y, 0f);

        if (editMode) {
            renderEditMode();
        } else if (circleEditMode) {
            renderCircleEditMode();
        } else {
            for (MyRender effect : activeEffects) {
                effect.render();
            }
        }

        renderUI();
    }

    private void renderCircleEditMode() {
        if (circleTextureId > 0) {
            renderCircleWithTexture();
        } else {
            renderCircleOutline();
        }
    }

    private void renderCircleWithTexture() {
        if (circleTextureExpand) {
            circleExpandOffset += circleExpandSpeed;
            if (circleExpandOffset > 5f) circleExpandOffset -= 5f;
        }

        if (circleAutoRotate) {
            circleTextureRotation += circleRotateSpeed;
            if (circleTextureRotation >= 360f) {
                circleTextureRotation -= 360f;
            }
        }


        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, circleTextureId);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 0.9f);
        
        float rotationRad = (float) Math.toRadians(circleTextureRotation);
        
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glTexCoord2f(0.5f, 0.5f);
        GL11.glVertex2f(circleCenter.x, circleCenter.y);
        
        for (int i = 0; i <= circleSegments; i++) {
            float angle = (float) (2 * Math.PI * i / circleSegments);
            float rotatedAngle = angle + rotationRad;
            
            float expandedRadius = circleRadius * (1f + circleExpandOffset);
            
            float x = circleCenter.x + (float) Math.cos(rotatedAngle) * expandedRadius;
            float y = circleCenter.y + (float) Math.sin(rotatedAngle) * expandedRadius;
            
            float u = 0.5f + 0.5f * (float) Math.cos(angle);
            float v = 0.5f + 0.5f * (float) Math.sin(angle);
            
            GL11.glTexCoord2f(u, v);
            GL11.glVertex2f(x, y);
        }
        
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        
        renderCircleHelperLines();
    }

    private void renderCircleOutline() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        
        GL11.glLineWidth(2f);
        GL11.glColor4f(0f, 1f, 0f, 1f);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(circleCenter.x, circleCenter.y);
        GL11.glEnd();
        
        GL11.glColor4f(1f, 1f, 0f, 0.8f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < circleSegments; i++) {
            float angle = (float) (2 * Math.PI * i / circleSegments);
            float x = circleCenter.x + (float) Math.cos(angle) * circleRadius;
            float y = circleCenter.y + (float) Math.sin(angle) * circleRadius;
            GL11.glVertex2f(x, y);
        }
        GL11.glEnd();
    }

    private void renderCircleHelperLines() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(1.5f);
        
        GL11.glColor4f(0f, 1f, 0f, 1f);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(circleCenter.x, circleCenter.y);
        GL11.glEnd();
        
        GL11.glColor4f(1f, 1f, 0f, 0.6f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < circleSegments; i++) {
            float angle = (float) (2 * Math.PI * i / circleSegments);
            float x = circleCenter.x + (float) Math.cos(angle) * circleRadius;
            float y = circleCenter.y + (float) Math.sin(angle) * circleRadius;
            GL11.glVertex2f(x, y);
        }
        GL11.glEnd();
    }

    private void renderEditMode() {
        if (quadTextureId > 0 && quadVertices.size() >= 2) {
            renderQuadStrip();
        }

        renderVertexPoints();
    }

    private void renderQuadStrip() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, quadTextureId);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 0.9f);

        int vertexCount = quadVertices.size();
        int stripCount = (vertexCount / 2) - 1;

        if (stripCount <= 0) return;

        if (textureFlowEnabled) {
            float baseFlowSpeed = 0.03f;
            float flowSpeed = baseFlowSpeed * flowSpeedMultiplier;
            switch (flowDirection) {
                case 0:
                    textureFlowOffset += flowSpeed;
                    break;
                case 1:
                    textureFlowOffset += flowSpeed;
                    break;
                case 2:
                    textureFlowOffset -= flowSpeed;
                    break;
                case 3:
                    textureFlowOffset -= flowSpeed;
                    break;
            }
        }


        GL11.glBegin(GL11.GL_QUAD_STRIP);

        for (int i = 0; i < stripCount + 1; i++) {
            int leftIndex = i * 2;
            int rightIndex = i * 2 + 1;

            if (rightIndex >= vertexCount) break;

            Vector2f left = quadVertices.get(leftIndex);
            Vector2f right = quadVertices.get(rightIndex);

            float baseTex = (float) i / stripCount;
            
            float u1, v1, u2, v2;
            
            if (textureRotation == 0f) {
                if (flowDirection == 1 || flowDirection == 3) {
                    u1 = baseTex + textureFlowOffset;
                    u2 = baseTex + textureFlowOffset;
                    v1 = 0f;
                    v2 = 1f;
                } else {
                    u1 = 0f;
                    u2 = 1f;
                    v1 = baseTex + textureFlowOffset;
                    v2 = baseTex + textureFlowOffset;
                }
            } else if (textureRotation == 90f) {
                if (flowDirection == 1 || flowDirection == 3) {
                    u1 = 0f;
                    u2 = 1f;
                    v1 = baseTex + textureFlowOffset;
                    v2 = baseTex + textureFlowOffset;
                } else {
                    u1 = baseTex + textureFlowOffset;
                    u2 = baseTex + textureFlowOffset;
                    v1 = 0f;
                    v2 = 1f;
                }
            } else if (textureRotation == 180f) {
                if (flowDirection == 1 || flowDirection == 3) {
                    u1 = 1f - baseTex - textureFlowOffset;
                    u2 = 1f - baseTex - textureFlowOffset;
                    v1 = 0f;
                    v2 = 1f;
                } else {
                    u1 = 1f;
                    u2 = 0f;
                    v1 = 1f - baseTex - textureFlowOffset;
                    v2 = 1f - baseTex - textureFlowOffset;
                }
            } else {
                if (flowDirection == 1 || flowDirection == 3) {
                    u1 = 0f;
                    u2 = 1f;
                    v1 = 1f - baseTex - textureFlowOffset;
                    v2 = 1f - baseTex - textureFlowOffset;
                } else {
                    u1 = 1f - baseTex - textureFlowOffset;
                    u2 = 1f - baseTex - textureFlowOffset;
                    v1 = 1f;
                    v2 = 0f;
                }
            }

            GL11.glTexCoord2f(u1, v1);
            GL11.glVertex2f(left.x, left.y);
            GL11.glTexCoord2f(u2, v2);
            GL11.glVertex2f(right.x, right.y);
        }

        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private void renderVertexPoints() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);

        GL11.glPointSize(6f);
        GL11.glColor4f(0f, 1f, 0f, 1f);
        GL11.glBegin(GL11.GL_POINTS);
        for (Vector2f vertex : quadVertices) {
            GL11.glVertex2f(vertex.x, vertex.y);
        }
        GL11.glEnd();

        GL11.glLineWidth(2f);
        GL11.glColor4f(1f, 1f, 0f, 0.8f);
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < quadVertices.size() - 1; i += 2) {
            if (i + 1 < quadVertices.size()) {
                Vector2f v1 = quadVertices.get(i);
                Vector2f v2 = quadVertices.get(i + 1);
                GL11.glVertex2f(v1.x, v1.y);
                GL11.glVertex2f(v2.x, v2.y);
            }
        }
        GL11.glEnd();

        if (quadVertices.size() >= 4) {
            GL11.glColor4f(0f, 1f, 1f, 0.5f);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int i = 0; i < quadVertices.size(); i += 2) {
                Vector2f v = quadVertices.get(i);
                GL11.glVertex2f(v.x, v.y);
            }
            GL11.glEnd();

            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int i = 1; i < quadVertices.size(); i += 2) {
                Vector2f v = quadVertices.get(i);
                GL11.glVertex2f(v.x, v.y);
            }
            GL11.glEnd();
        }
    }

    private void renderUI() {
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(0f, 0f, 0f, 0.7f);

        float uiHeight;
        if (editMode) {
            uiHeight = 240f;
        } else if (circleEditMode) {
            uiHeight = 200f;
        } else {
            uiHeight = 150f;
        }

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(10, 10);
        GL11.glVertex2f(400, 10);
        GL11.glVertex2f(400, 10 + uiHeight);
        GL11.glVertex2f(10, 10 + uiHeight);
        GL11.glEnd();

        String currentEffect = currentEffectClass == null ? "无" : currentEffectClass.getSimpleName();

        float baseScale = 0.8f;
        float lineHeight = 22f;
        float startY = 25f;

        if (editMode) {
            drawText("编辑模式 - QUAD_STRIP", 20, startY, baseScale);
            drawText(String.format("顶点数: %d (%d对)", quadVertices.size(), quadVertices.size() / 2), 20, startY + lineHeight * 1.2f, baseScale);
            drawText(String.format("飘带段: %d", Math.max(0, quadVertices.size() / 2 - 1)), 20, startY + lineHeight * 2.4f, baseScale);
            drawText(String.format("纹理旋转: %.0f°", textureRotation), 20, startY + lineHeight * 3.4f, baseScale);

            String flowStatus = textureFlowEnabled ? "开启" : "关闭";
            String[] directions = {"正向", "向右", "反向", "向左"};
            drawText(String.format("流动: %s (%s)", flowStatus, directions[flowDirection]), 20, startY + lineHeight * 4.4f, baseScale);
            drawText(String.format("速度倍数: %.1fx", flowSpeedMultiplier), 20, startY + lineHeight * 5.2f, baseScale);

            drawText("F2-退出 V-切换添加", 20, startY + lineHeight * 6.4f, baseScale * 0.85f);
            drawText("I-纹理 L-旋转 P-流动", 20, startY + lineHeight * 7.6f, baseScale * 0.85f);
            drawText("O-方向 S-速度 C-清空", 20, startY + lineHeight * 8.6f, baseScale * 0.85f);
            drawText("左键-添加 右键-删除", 20, startY + lineHeight * 9.6f, baseScale * 0.85f);
            drawText("Ctrl+V-粘贴 F3-圆形模式", 20, startY + lineHeight * 10.6f, baseScale * 0.85f);

            if (showGridInput) {
                String prompt = inputStep == 0 ? "纹理路径:" : "暂不使用";
                drawText(prompt, 20, startY + lineHeight * 11.8f, baseScale * 0.9f);
                drawText(inputBuffer.toString() + "_", 20, startY + lineHeight * 13.0f, baseScale * 0.9f);
            }

            if (showSpeedInput) {
                drawText("流动速度倍数:", 20, startY + lineHeight * 11.8f, baseScale * 0.9f);
                drawText(inputBuffer.toString() + "_", 20, startY + lineHeight * 13.0f, baseScale * 0.9f);
            }
        } else if (circleEditMode) {
            drawText("圆形编辑模式", 20, startY, baseScale);
            drawText(String.format("圆心: (%.0f, %.0f)", circleCenter.x, circleCenter.y), 20, startY + lineHeight * 1.2f, baseScale);
            drawText(String.format("半径: %.0f", circleRadius), 20, startY + lineHeight * 2.4f, baseScale);
            drawText(String.format("旋转: %.1f°", circleTextureRotation), 20, startY + lineHeight * 3.4f, baseScale);
            drawText(String.format("自动旋转: %s", circleAutoRotate ? "开启" : "关闭"), 20, startY + lineHeight * 4.2f, baseScale);
            drawText(String.format("扩散: %s", circleTextureExpand ? "开启" : "关闭"), 20, startY + lineHeight * 5.0f, baseScale);
            drawText(String.format("旋转速度: %.1f°/帧", circleRotateSpeed), 20, startY + lineHeight * 5.8f, baseScale);
            drawText("F3-退出 I-加载纹理", 20, startY + lineHeight * 7.0f, baseScale * 0.85f);
            drawText("R-旋转 O-扩散 S-速度", 20, startY + lineHeight * 8.2f, baseScale * 0.85f);
            drawText("C-重置 左键-圆心 右键-半径", 20, startY + lineHeight * 9.2f, baseScale * 0.85f);

            if (showGridInput) {
                drawText("纹理路径:", 20, startY + lineHeight * 10.4f, baseScale * 0.9f);
                drawText(inputBuffer.toString() + "_", 20, startY + lineHeight * 11.6f, baseScale * 0.9f);
            }

            if (showSpeedInput) {
                drawText("速度 (旋转°/扩散%):", 20, startY + lineHeight * 10.4f, baseScale * 0.9f);
                drawText(inputBuffer.toString() + "_", 20, startY + lineHeight * 11.6f, baseScale * 0.9f);
            }
        } else {

            drawText("当前效果: " + currentEffect, 20, startY, baseScale);
            drawText(String.format("  [%d/%d]", currentRenderIndex + 1, availableRenderClasses.size()), 20, startY + lineHeight * 1.2f, baseScale * 0.9f);
            drawText(String.format("活跃实例: %d", activeEffects.size()), 20, startY + lineHeight * 2.4f, baseScale);
            drawText(String.format("位置: %.0f, %.0f", cameraPosition.x, cameraPosition.y), 20, startY + lineHeight * 3.6f, baseScale);
            drawText(String.format("缩放: %.2f", cameraZoom), 20, startY + lineHeight * 4.8f, baseScale);
            drawText("控制说明:", 20, startY + lineHeight * 6.2f, baseScale);
            drawText("  左键 - 生成效果", 20, startY + lineHeight * 7.4f, baseScale * 0.85f);
            drawText("  右键 - 切换效果", 20, startY + lineHeight * 8.4f, baseScale * 0.85f);
            drawText("  F2 - 飘带模式", 20, startY + lineHeight * 9.4f, baseScale * 0.85f);
            drawText("  F3 - 圆形模式", 20, startY + lineHeight * 10.4f, baseScale * 0.85f);
        }

        GL11.glPopMatrix();
    }


    private void drawText(String text, float x, float y, float scale) {
        if (fontTextureId <= 0 || fontCharacters.isEmpty()) {
            drawFallbackText(text, x, y, scale);
            return;
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0f);
        GL11.glScalef(scale, scale, 1f);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureId);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        float currentX = 0f;
        float maxY = 0f;

        for (char c : text.toCharArray()) {
            CharacterData charData = fontCharacters.get(c);
            if (charData != null) {
                float u1 = charData.x / (float) fontTextureWidth;
                float v1 = charData.y / (float) fontTextureHeight;
                float u2 = (charData.x + charData.width) / (float) fontTextureWidth;
                float v2 = (charData.y + charData.height) / (float) fontTextureHeight;

                float renderX = currentX + charData.xoffset;
                float renderY = charData.yoffset;

                float charBottom = renderY + charData.height;
                if (charBottom > maxY) {
                    maxY = charBottom;
                }

                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(u1, v1);
                GL11.glVertex2f(renderX, renderY);
                GL11.glTexCoord2f(u2, v1);
                GL11.glVertex2f(renderX + charData.width, renderY);
                GL11.glTexCoord2f(u2, v2);
                GL11.glVertex2f(renderX + charData.width, renderY + charData.height);
                GL11.glTexCoord2f(u1, v2);
                GL11.glVertex2f(renderX, renderY + charData.height);
                GL11.glEnd();

                currentX += charData.xadvance;
            }
        }

        GL11.glPopMatrix();
    }

    private void drawFallbackText(String text, float x, float y, float scale) {
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(x, y, 0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        String[] lines = text.split("\n");
        float lineHeight = 20f * scale;

        for (int i = 0; i < lines.length && i < 5; i++) {
            drawSimpleLine(lines[i], 0, -i * lineHeight, scale);
        }

        GL11.glPopMatrix();
    }

    private void drawSimpleLine(String text, float x, float y, float scale) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0f);
        GL11.glScalef(scale * 0.5f, scale * 0.5f, 1f);

        GL11.glLineWidth(2f);
        GL11.glBegin(GL11.GL_LINES);

        float xOffset = 0f;
        for (char c : text.toCharArray()) {
            drawCharLines(c, xOffset, 0);
            xOffset += 12f;
        }

        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private void drawCharLines(char c, float x, float y) {
        switch (Character.toUpperCase(c)) {
            case 'A':
                GL11.glVertex2f(x+2, y); GL11.glVertex2f(x+5, y+10);
                GL11.glVertex2f(x+5, y+10); GL11.glVertex2f(x+8, y);
                break;
            case 'B':
                GL11.glVertex2f(x+2, y); GL11.glVertex2f(x+2, y+10);
                GL11.glVertex2f(x+2, y+10); GL11.glVertex2f(x+7, y+8);
                GL11.glVertex2f(x+7, y+8); GL11.glVertex2f(x+7, y+5);
                GL11.glVertex2f(x+7, y+5); GL11.glVertex2f(x+2, y+5);
                GL11.glVertex2f(x+2, y+5); GL11.glVertex2f(x+7, y+3);
                GL11.glVertex2f(x+7, y+3); GL11.glVertex2f(x+7, y);
                GL11.glVertex2f(x+7, y); GL11.glVertex2f(x+2, y);
                break;
            default:
                GL11.glVertex2f(x+2, y+2); GL11.glVertex2f(x+8, y+8);
                break;
        }
    }

    private Vector2f screenToWorld(float screenX, float screenY) {
        float worldX = (screenX - WINDOW_WIDTH / 2f) / cameraZoom + cameraPosition.x;
        float worldY = (screenY - WINDOW_HEIGHT / 2f) / cameraZoom + cameraPosition.y;
        return new Vector2f(worldX, worldY);
    }

    private void cleanup() {
        Display.destroy();
    }


    public interface MyRender {
        void advance(float amount);

        void render();

        boolean isExpired();
    }

    public interface InitializableEffect extends MyRender {
        void initialize(float x, float y);
    }

    public interface MouseAwareEffect extends MyRender {
        void setMousePosition(float x, float y);
    }
}

