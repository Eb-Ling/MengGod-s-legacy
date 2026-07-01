package data;

import java.io.File;

public class RenderTestMain {
    public static void main(String[] args) {
        loadLWJGLLibraries();
        
        MyRenderTool tool = new MyRenderTool();
        
        System.out.println("正在扫描渲染类...");
        tool.scanAndLoadRenderClasses();
        if (tool.switchToRender("KabbalahLifeTreeEffect")) {
            tool.setSpawnCurrentOnStart(true);
        }
        
        tool.setCameraPosition(0f, 0f);
        tool.setCameraZoom(1.0f);
        
        System.out.println("\n=== MyRender Tool 启动 ===");
        System.out.println("控制说明:");
        System.out.println("W/A/S/D 或 方向键 - 移动摄像头");
        System.out.println("Q/E - 缩小/放大");
        System.out.println("R - 重置摄像头");
        System.out.println("鼠标左键 - 与当前渲染交互");
        System.out.println("鼠标右键 - 切换到下一个渲染类");
        System.out.println("=========================");
        
        tool.start();
    }
    
    private static void loadLWJGLLibraries() {
        String nativePath = "D:\\Starsector\\starsector-core\\native\\windows";
        File nativeDir = new File(nativePath);
        
        if (!nativeDir.exists()) {
            System.err.println("✗ 错误: 未找到原生库目录: " + nativePath);
            return;
        }
        
        System.setProperty("java.library.path", nativePath);
        
        try {
            System.load(nativePath + "\\lwjgl64.dll");
            System.out.println("✓ 已加载 lwjgl64.dll");
            
            File openALFile = new File(nativePath + "\\OpenAL64.dll");
            if (openALFile.exists()) {
                System.load(nativePath + "\\OpenAL64.dll");
                System.out.println("✓ 已加载 OpenAL64.dll");
            }
            
            System.out.println("✓ LWJGL原生库加载成功");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("✗ 加载LWJGL库失败: " + e.getMessage());
            System.err.println("\n请尝试以下方案:");
            System.err.println("1. 在IDEA中: Run → Edit Configurations");
            System.err.println("2. 在VM options中添加:");
            System.err.println("   -Djava.library.path=" + nativePath);
            System.err.println("3. 重新运行程序");
            throw e;
        }
    }
}
