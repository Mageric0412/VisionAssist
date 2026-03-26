# VisionAssist - 视觉辅助避障应用

## 项目概述
帮助弱视、盲人通过摄像头实时识别障碍物并语音播报方位、距离、场景信息。

## 技术架构
- **平台**: Android (API 26+) / 华为HMS
- **检测引擎**: TFLite + ML Kit + HMS ML Kit
- **语音合成**: Android TTS / HMS SpeechKit
- **模型**: YOLOv8n-tflite / SSD-MobileNet

## 核心模块
1. **CameraManager** - 摄像头捕获管理
2. **ObjectDetector** - 目标检测引擎（支持多引擎切换）
3. **SceneAnalyzer** - 场景分析模块
4. **SpeechManager** - 语音播报管理
5. **ConfigManager** - 配置管理

## 性能要求
- 检测延迟 < 100ms
- 语音播报延迟 < 300ms
- 帧率 >= 15fps

## 配置项
- 检测灵敏度
- 播报语音选择
- 播报内容类型（方位/距离/场景）
- 检测距离阈值
- 播报间隔
