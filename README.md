# VisionAssist - 视觉辅助避障应用

## 项目简介

VisionAssist 是一款专为视障人士设计的 Android 应用，通过摄像头实时检测障碍物并语音播报方位、距离、场景等信息，帮助用户安全导航。

## 核心功能

- **实时目标检测**: 支持 Google ML Kit、Huawei HMS ML Kit、TFLite YOLOv8n 三种检测引擎
- **智能语音播报**: 语音播报障碍物位置、方向、距离和大小
- **场景感知**: 自动识别室内/室外环境及光照条件
- **高度可配置**: 支持灵敏度、播报间隔、语音参数等多项自定义设置
- **华为 HMS 支持**: 可选使用华为 HMS ML Kit 实现更优性能
- **低延迟设计**: 针对实时性优化，检测延迟 < 100ms

## 技术架构

```
┌─────────────────────────────────────────────────────┐
│                    VisionAssist                      │
├─────────────────────────────────────────────────────┤
│  UI Layer (MainActivity, SettingsActivity)          │
├─────────────────────────────────────────────────────┤
│  Business Logic                                     │
│  ├─ CameraManager (CameraX)                         │
│  ├─ ObjectDetector (ML Kit / HMS / TFLite)         │
│  ├─ SceneAnalyzer (环境分析)                        │
│  ├─ SpeechManager (TTS)                             │
│  └─ ConfigManager (偏好设置)                        │
├─────────────────────────────────────────────────────┤
│  Platform Support                                   │
│  ├─ Android CameraX                                 │
│  ├─ Google ML Kit                                   │
│  ├─ Huawei HMS ML Kit                              │
│  └─ TensorFlow Lite                                 │
└─────────────────────────────────────────────────────┘
```

## 模块说明

| 模块 | 说明 |
|------|------|
| `camera` | CameraX 摄像头管理，帧捕获和预处理 |
| `detector` | 目标检测接口及三种实现（ML Kit/HMS/TFLite） |
| `speech` | 语音合成管理，支持 Android TTS 和华为 TTS |
| `analysis` | 场景分析（室内/室外、光照条件） |
| `config` | 应用配置管理 |
| `utils` | 工具类（播报文本构建） |

## 构建要求

- Android Studio Hedgehog 或更高版本
- Android SDK 34
- Kotlin 1.9.22
- Gradle 8.2

## 构建步骤

1. **克隆项目**
```bash
cd /Users/Mageric/VisionAssist
```

2. **配置 Android SDK**
确保 `local.properties` 中的 SDK 路径正确：
```properties
sdk.dir=/Users/Mageric/Library/Android/sdk
```

3. **下载 YOLOv8n 模型（可选）**
将 TFLite 模型放入 `app/src/main/assets/yolov8n.tflite`

4. **构建 Debug APK**
```bash
./gradlew assembleDebug
```

5. **安装到设备**
```bash
./gradlew installDebug
```

## 配置说明

### 检测引擎选择
- **Google ML Kit**: 默认推荐，无需额外配置
- **Huawei HMS**: 需要华为移动服务，需配置 API Key
- **TFLite YOLOv8**: 本地推理，需要下载模型文件

### 关键配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `detection_engine` | 检测引擎 | google_mlkit |
| `min_confidence` | 最小置信度 | 0.6 |
| `report_interval` | 播报间隔(ms) | 1500 |
| `report_distance` | 播报距离 | true |
| `report_direction` | 播报方向 | true |
| `focus_closest` | 仅关注最近障碍 | true |
| `distance_threshold` | 检测距离阈值(m) | 3.0 |

## 测试建议

1. **室内测试**: 测试桌椅、门框等常见障碍物
2. **室外测试**: 测试行人、车辆等移动物体
3. **光照测试**: 测试明亮和昏暗环境下的识别效果
4. **性能测试**: 观察帧率和延迟是否符合要求

## 性能优化建议

1. 使用 YOLOv8n-int8 量化模型减小体积
2. 启用 NNAPI 加速（支持的设备）
3. 调整帧率目标（默认15fps）
4. 减小 CameraX 分辨率

## 未来扩展

- [ ] 深度传感器支持（TOF/结构光）
- [ ] 语义分割增强场景理解
- [ ] AR 辅助功能
- [ ] 导航路径规划
- [ ] 紧急呼叫集成

## 注意事项

1. 本应用仅为辅助工具，不能替代专业医疗建议
2. 语音播报可能受环境噪声影响
3. 距离估算为近似值，实际距离可能有偏差
4. 持续使用会消耗较多电量

## License

MIT License
