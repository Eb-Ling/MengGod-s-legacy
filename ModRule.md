---
trigger: always_on
---
# Mod开发规范

## 参考文档目录

**查阅时机**：只有需要相关知识时才会查看。

- `src/rulessystem.txt`：远行星号rules系统介绍（这不是需要你遵守的规则）。当需要在生涯模式实现对话与交互相关事件时，需要修改 `rules.csv` 文件，此时你需要阅读这个文档。
- `src/methods/shaders/ShaderUtil.java`：GPU渲染管线工具类（`data.methods.shaders.ShaderUtil`）。封装shader编译链接、FBO管理、VBO/IBO创建、矩阵操作、uniform设置、四边形渲染等方法。编写或重构渲染插件时应优先使用此工具类。
- `src/methods/shaders/GLSLSnippets.java`：GLSL可复用代码片段库（`data.methods.shaders.GLSLSnippets`）。以 `public static final String` 常量提供20个GLSL函数片段（噪声、SDF、渐变、曲线等）。编写新shader效果时应从此库引用标准片段，避免重复手写相同GLSL函数。

## MD文件修改规范

1. **3轮调整未解决**：当用户提出的需求经历了3轮以上的调整仍无法满足，在最终问题解决时需要整理问题并写入该md文档的问题记忆段落。

2. **用户主动提及**：当用户主动提及记忆问题时，同样需要写入本md文档问题记忆段落。

3. **动态调整规范**：根据用户需求调整本md文档的编码规范。

4. **问题记忆编写规范**：只需简洁描述问题内容和正确理解，用几句话写完即可，不要总结教训。

## 编码规范

1. **详细注释要求**：对于用户需要添加的功能类以及其下所有成员变量、方法，都需要分别生成注释，介绍其继承的API、实现的功能、方法路径，以及被调用的方式。成员变量可以按功能分组注释。

2. **设计方案先行**：在尝试生成详细代码之前，需要先简要介绍本次改动设计的方法和架构，征求用户意见后实施。

3. **日志记录规范**：游戏内报错和调试信息统一使用 Starsector 的 Logger 系统记录，禁止使用 `System.out` 或 `System.err`。通过 `Global.getLogger(ClassName.class)` 获取 Logger 实例，不同级别使用对应方法：`log.info()` 用于普通信息，`log.debug()` 用于调试信息，`log.warn()` 用于警告，`log.error()` 用于错误。

## 问题记忆

1. **规则文件位置混淆**：主规则文件是 `src/.qoder/rules/ModRule.md`（需要维护），`src/rulessystem.txt` 仅是参考文档（查阅生涯对话交互知识时使用）。
2. **GLSL片段着色器中禁止编写注释**：避免编译错误。如需说明，应在Java代码层面前方添加整体注释

## 审计范围

1. Java堆对象、静态集合、customData 
2. 战斗插件、分层渲染插件、舰船/生涯监听
3. 生涯脚本、Sector注册对象
4.  BoxUtil, OpenGL shader, SSBO Quad SDF等显存资源
5. 仅审计src/data运行时代码，不纳入tools/checks
6.  “明显”必须具备完整可达、重复创建、强引用持续保留、缺少清理和无界增长证据
