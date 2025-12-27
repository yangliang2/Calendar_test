# Calendar 项目全面解读报告

> 本文档对 Calendar 项目进行全面的技术分析，包括项目功能、技术栈、架构设计、模块依赖、分支管理和代码质量等方面。

## 目录

- [1. 项目内容和主要功能](#1-项目内容和主要功能)
- [2. 项目技术栈](#2-项目技术栈)
- [3. 项目架构](#3-项目架构)
- [4. 模块主要功能](#4-模块主要功能)
- [5. 依赖关系](#5-依赖关系)
- [6. 分支管理及历史提交分析](#6-分支管理及历史提交分析)
- [7. 设计模式及代码质量](#7-设计模式及代码质量)
- [8. 总结](#8-总结)

---

## 1. 项目内容和主要功能

### 1.1 项目概述

**Calendar** 是一个高度可定制的日历库，支持 **Android** 和 **Compose Multiplatform**（跨平台）。该项目由 **Kizito Nwose** 开发维护，自 2019 年起已持续开发超过 5 年，是一个成熟且活跃的开源项目。

- **GitHub**: https://github.com/kizitonwose/Calendar
- **许可证**: MIT License
- **Maven Group**: `com.kizitonwose.calendar`

### 1.2 核心功能

| 功能 | 说明 |
|------|------|
| **多种日历模式** | 周视图、月视图、年视图 |
| **日期选择** | 单选、多选、范围选择 |
| **日期禁用** | 支持禁用特定日期 |
| **边界日期** | 限制日历日期范围 |
| **自定义视图** | 完全自定义日期单元格和日历样式 |
| **自定义首日** | 任意设置一周的第一天 |
| **滚动方向** | 支持水平和垂直滚动 |
| **热力图日历** | 类似 GitHub 贡献图的数据展示 |
| **Headers/Footers** | 年/月/周的头部和尾部自定义 |
| **程序化滚动** | 支持代码控制滚动到指定日期 |

### 1.3 提供的 Composables

库提供了 6 种日历组件：

```kotlin
HorizontalCalendar()      // 水平滚动月日历
VerticalCalendar()        // 垂直滚动月日历
WeekCalendar()            // 水平滚动周日历
HeatMapCalendar()         // 热力图日历
HorizontalYearCalendar()  // 水平滚动年日历
VerticalYearCalendar()    // 垂直滚动年日历
```

### 1.4 发布的 Artifacts

| Artifact | 说明 |
|----------|------|
| `com.kizitonwose.calendar:view` | Android View 系统日历 |
| `com.kizitonwose.calendar:compose` | Android Compose 日历 |
| `com.kizitonwose.calendar:compose-multiplatform` | 跨平台 Compose 日历 |

---

## 2. 项目技术栈

### 2.1 编程语言与框架

| 类别 | 技术 |
|------|------|
| **主要语言** | Kotlin 2.2.20 |
| **UI 框架** | Jetpack Compose 1.9.x (Android), Compose Multiplatform 1.9.0 |
| **构建系统** | Gradle 8.x + Kotlin DSL |
| **JDK 版本** | Java 17 |

### 2.2 核心依赖库

| 依赖 | 版本 | 用途 |
|------|------|------|
| `kotlinx-datetime` | 0.7.1 | 跨平台日期时间处理 |
| `kotlinx-serialization` | 1.9.0 | 序列化支持 |
| `androidx.recyclerview` | 1.4.0 | View 系统日历的基础 |
| `androidx.compose.foundation` | 1.9.1 | Compose LazyRow/LazyColumn |
| `java.time` | - | Android 日期 API (需 Java 8 desugaring) |

### 2.3 构建与发布工具

| 工具 | 用途 |
|------|------|
| `kotlinter` | Kotlin 代码格式检查 |
| `maven-publish` (Vanniktech) | Maven Central 发布 |
| `binary-compatibility-validator` | API 兼容性检查 |
| `version-check` | 依赖版本检查 |

### 2.4 CI/CD

- **平台**: GitHub Actions
- **流水线包含**:
  - Ktlint 代码格式检查
  - API 兼容性检查
  - 单元测试
  - Instrumentation 测试 (API 24, 29)
  - 自动发布到 Maven Central (Sonatype Central Portal)

---

## 3. 项目架构

### 3.1 模块结构

```
Calendar (根项目)
├── core                          # 核心数据模型 (纯 JVM 库)
├── data                          # 数据计算逻辑 (纯 JVM 库)
├── view                          # Android View 系统日历
├── compose                       # Android Compose 日历
├── compose-multiplatform/        # 跨平台 Compose 日历
│   ├── library                   # 跨平台库
│   └── sample                    # 跨平台示例
├── sample                        # Android 示例应用
└── buildSrc                      # 构建配置
```

### 3.2 架构层次图

```
┌─────────────────────────────────────────────────────────────┐
│                    展示层 (UI Layer)                         │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────────┐ │
│  │   view      │  │   compose   │  │compose-multiplatform │ │
│  │(RecyclerView│  │(LazyRow/    │  │(Android/iOS/Desktop  │ │
│  │   based)    │  │ LazyColumn) │  │  /Web/WasmJS)        │ │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬───────────┘ │
└─────────┼────────────────┼───────────────────┼──────────────┘
          │                │                   │
┌─────────┼────────────────┼───────────────────┼──────────────┐
│         ▼                ▼                   ▼              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    data 模块                         │   │
│  │           (日历数据计算和生成逻辑)                    │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    core 模块                         │   │
│  │              (核心数据模型和类型定义)                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                    数据层 (Data Layer)                       │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 跨平台源集层次结构

```
commonMain (通用代码)
├── jvmMain (JVM 平台共享)
│   ├── androidMain
│   └── desktopMain
├── nonJvmMain (非 JVM 平台共享)
│   ├── nativeMain (iOS 等原生平台)
│   └── webMain
│       ├── jsMain (JavaScript)
│       └── wasmJsMain (WebAssembly)
```

### 3.4 支持的平台

| 平台 | 支持状态 |
|------|----------|
| Android | ✅ View + Compose |
| iOS (arm64/x64/simulator) | ✅ Compose Multiplatform |
| Desktop (JVM) | ✅ Compose Multiplatform |
| JavaScript | ✅ Compose Multiplatform |
| WasmJS | ✅ Compose Multiplatform |

---

## 4. 模块主要功能

### 4.1 core 模块

**定位**: 核心数据模型定义（纯 Kotlin，无平台依赖）

| 类 | 功能 |
|---|---|
| `CalendarDay` | 表示日历上的一天 (日期 + 位置) |
| `CalendarMonth` | 表示一个月 (年月 + 周数据) |
| `CalendarYear` | 表示一年 |
| `Week` / `WeekDay` | 周日历数据模型 |
| `DayPosition` | 日期位置枚举 (InDate/MonthDate/OutDate) |
| `OutDateStyle` | 外部日期样式 (EndOfRow/EndOfGrid) |

**核心数据类示例**:

```kotlin
@Immutable
public data class CalendarDay(
    val date: LocalDate, 
    val position: DayPosition
) : Serializable

public enum class DayPosition {
    InDate,     // 上月日期（填充）
    MonthDate,  // 当月日期
    OutDate     // 下月日期（填充）
}
```

### 4.2 data 模块

**定位**: 日历数据计算逻辑

| 类/函数 | 功能 |
|---|---|
| `MonthData` | 月数据计算 (inDays, outDays, 总天数) |
| `WeekData` | 周数据计算 |
| `YearData` | 年数据计算 |
| `DataStore` | 数据缓存存储 |
| `getCalendarMonthData()` | 生成月历数据 |
| `getHeatMapCalendarMonthData()` | 生成热力图数据 |

**核心计算逻辑**:

```kotlin
public fun getCalendarMonthData(
    startMonth: YearMonth,
    offset: Int,
    firstDayOfWeek: DayOfWeek,
    outDateStyle: OutDateStyle,
): MonthData {
    val month = startMonth.plusMonths(offset.toLong())
    val firstDay = month.atStartOfMonth()
    val inDays = firstDayOfWeek.daysUntil(firstDay.dayOfWeek)
    // ... 计算 outDays 和返回 MonthData
}
```

### 4.3 view 模块

**定位**: Android View 系统实现（基于 RecyclerView）

| 类 | 功能 |
|---|---|
| `CalendarView` | 月日历视图 (继承 RecyclerView) |
| `WeekCalendarView` | 周日历视图 |
| `YearCalendarView` | 年日历视图 |
| `Binder` 接口 | ViewHolder 绑定模式 |
| `MonthCalendarAdapter` | 月历 RecyclerView 适配器 |
| `CalendarPageSnapHelper` | 分页滚动辅助 |

**Binder 模式**:

```kotlin
public interface Binder<Data, Container : ViewContainer> {
    public fun create(view: View): Container
    public fun bind(container: Container, data: Data)
}

public interface MonthDayBinder<Container : ViewContainer> : 
    Binder<CalendarDay, Container>
```

### 4.4 compose 模块

**定位**: Android Compose 实现（基于 LazyRow/LazyColumn）

| Composable | 功能 |
|---|---|
| `HorizontalCalendar` | 水平月日历 |
| `VerticalCalendar` | 垂直月日历 |
| `WeekCalendar` | 周日历 |
| `HeatMapCalendar` | 热力图日历 |
| `HorizontalYearCalendar` | 水平年日历 |
| `VerticalYearCalendar` | 垂直年日历 |
| `CalendarState` | 日历状态管理 |

**使用示例**:

```kotlin
@Composable
fun MainScreen() {
    val state = rememberCalendarState(
        startMonth = YearMonth.now().minusMonths(100),
        endMonth = YearMonth.now().plusMonths(100),
        firstVisibleMonth = YearMonth.now(),
        firstDayOfWeek = firstDayOfWeekFromLocale()
    )
    
    HorizontalCalendar(
        state = state,
        dayContent = { day -> DayContent(day) },
        monthHeader = { month -> MonthHeader(month) }
    )
}
```

### 4.5 compose-multiplatform 模块

**定位**: 跨平台 Compose 实现

与 Android compose 模块 API 完全一致，差异仅在于日期类型导入来源：

| 平台 | 日期类型来源 |
|------|-------------|
| Android | `java.time.YearMonth` |
| Multiplatform | `kotlinx.datetime.YearMonth` |

---

## 5. 依赖关系

### 5.1 模块依赖图

```
┌───────────────────────────────────────────────────────────┐
│                        sample                              │
│                          │                                 │
│               ┌──────────┼──────────┐                     │
│               ▼          ▼          ▼                     │
│          ┌────────┐ ┌────────┐ ┌────────┐                │
│          │  view  │ │compose │ │  data  │                │
│          └───┬────┘ └───┬────┘ └───┬────┘                │
│              │          │          │                      │
│      ┌───────┴──────────┴──────────┘                      │
│      │                                                    │
│      ▼                                                    │
│  ┌────────┐                                               │
│  │  core  │ (无依赖)                                       │
│  └────────┘                                               │
└───────────────────────────────────────────────────────────┘

compose-multiplatform/library (独立模块，内含 core + data + compose)
```

### 5.2 详细依赖说明

| 模块 | 依赖 | 类型 | 说明 |
|------|------|------|------|
| **core** | `compose.runtime` | compileOnly | 仅用于 `@Immutable` 注解 |
| **data** | `core` | implementation | 使用核心模型 |
| **data** | `kotlin-stdlib` | implementation | Kotlin 标准库 |
| **view** | `core` | api | 暴露核心类型 |
| **view** | `data` | implementation | 数据计算 |
| **view** | `recyclerview` | api | RecyclerView 基础 |
| **compose** | `core` | api | 暴露核心类型 |
| **compose** | `data` | implementation | 数据计算 |
| **compose** | `compose.*` | implementation | Compose UI 库 |
| **compose-multiplatform** | `kotlinx-datetime` | api | 跨平台日期 |
| **compose-multiplatform** | `kotlinx-serialization` | implementation | 序列化 |

### 5.3 外部依赖版本

```toml
[versions]
agp = "8.13.0"
kotlin = "2.2.20"
composeAndroid = "1.9.1"
composeMultiplatform = "1.9.0"
kotlinxSerialization = "1.9.0"
kotlinx-datetime = "0.7.1"
junit5 = "5.13.4"
espresso = "3.7.0"
```

---

## 6. 分支管理及历史提交分析

### 6.1 分支策略

```
main (主分支，唯一长期分支)
  │
  ├── release_X.Y.Z (版本发布分支，合并后删除)
  ├── compose_X.Y (Compose 版本升级分支)
  └── feature/* (功能分支)
```

**特点**:
- 采用简洁的 **trunk-based development** 模式
- 所有开发在功能分支完成后合并到 main
- 通过 Git tags 管理版本发布

### 6.2 版本发布历史

| 版本 | 主要变更 |
|------|----------|
| 2.9.0 | 升级 Compose 到 1.9.x |
| 2.8.0 | 迁移到 Kotlin YearMonth |
| 2.7.0 | 升级 Compose Multiplatform 到 1.8.0 |
| 2.6.x | minSdk 升级到 21 |
| 2.5.x | 首次支持 Compose Multiplatform |
| 2.0.x | 首次支持 Compose UI 1.2 |

### 6.3 提交统计

| 年份 | 提交数 |
|------|--------|
| 2024 年至今 | 227 |
| 2023 年 | 42 |
| 2022 年 | 130 |

### 6.4 贡献者分析

| 贡献者 | 提交数 | 占比 |
|--------|--------|------|
| Kizito Nwose | 802 | ~95% |
| Goooler | 8 | - |
| luis | 6 | - |
| Alex Petrakov | 4 | - |
| 其他社区贡献者 | ~20 | - |

### 6.5 重要里程碑

| 提交 | 描述 |
|------|------|
| `bc3b746` | 添加 JavaScript 平台支持 |
| `8fbeda0` | 迁移到 Kotlin YearMonth |
| `f5dbbe4` | 升级 Compose Multiplatform 到 1.9.0 |
| `ed7c548` | 迁移到 Sonatype Central Portal |

---

## 7. 设计模式及代码质量

### 7.1 设计模式

#### 1. State Pattern (状态模式)

```kotlin
@Stable
public class CalendarState(
    startMonth: YearMonth,
    endMonth: YearMonth,
    ...
) : ScrollableState {
    var startMonth by mutableStateOf(startMonth)
    var endMonth by mutableStateOf(endMonth)
    val firstVisibleMonth by derivedStateOf { ... }
}
```

- 使用 Compose 的 `mutableStateOf` 管理可变状态
- 通过 `derivedStateOf` 派生计算状态
- 支持状态保存/恢复 (`Saver`)

#### 2. ViewHolder/Binder Pattern

```kotlin
public interface Binder<Data, Container : ViewContainer> {
    fun create(view: View): Container
    fun bind(container: Container, data: Data)
}
```

- 分离视图创建和数据绑定
- View 系统复用 ViewHolder 模式

#### 3. Factory Pattern (工厂模式)

```kotlin
public fun getCalendarMonthData(
    startMonth: YearMonth,
    offset: Int,
    firstDayOfWeek: DayOfWeek,
    outDateStyle: OutDateStyle,
): MonthData
```

- 通过工厂函数创建复杂对象

#### 4. Strategy Pattern (策略模式)

```kotlin
public enum class OutDateStyle {
    EndOfRow,   // 填充到行末
    EndOfGrid   // 填充到 6x7 网格
}
```

- 日期生成策略可配置

#### 5. Observer Pattern (观察者模式)

```kotlin
public var monthScrollListener: MonthScrollListener? = null
```

- 通过回调监听滚动事件

#### 6. Adapter Pattern (适配器模式)

- `MonthCalendarAdapter` 适配 RecyclerView
- Compose 版本使用 `LazyListScope` 扩展

#### 7. Builder/DSL Pattern

```kotlin
HorizontalCalendar(
    state = state,
    dayContent = { Day(it) },
    monthHeader = { MonthHeader(it) },
    monthBody = { month, content -> Box { content() } },
    monthContainer = { month, container -> container() }
)
```

- 使用 Compose DSL 风格构建 UI

### 7.2 代码质量

#### 代码规模

- **总代码行数**: ~26,000 行 Kotlin
- **模块化程度**: 高度模块化，职责清晰

#### 质量保证措施

| 措施 | 说明 |
|------|------|
| **Ktlint** | 代码格式一致性 |
| **Binary Compatibility Validator** | API 兼容性检查 |
| **Explicit API Mode** | 强制公开 API 声明可见性 |
| **JUnit 5** | 单元测试框架 |
| **Instrumentation Tests** | Android 集成测试 |
| **CI/CD** | 自动化测试和发布 |

#### 代码风格亮点

```kotlin
// 使用 @Immutable 优化 Compose 重组
@Immutable
public data class CalendarDay(val date: LocalDate, val position: DayPosition)

// 使用 @Stable 标记稳定的状态类
@Stable
public class CalendarState(...)

// 自定义 equals/hashCode 优化比较
override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    // 只比较关键字段，提升性能
}
```

### 7.3 文档质量

| 文档 | 说明 |
|------|------|
| `README.md` | 详尽的使用指南和版本兼容表 |
| `docs/Compose.md` | Compose 版本详细教程 |
| `docs/View.md` | View 版本详细教程 |
| KDoc 注释 | 公开 API 都有文档注释 |
| 示例项目 | Android 和跨平台示例应用 |

### 7.4 架构优点

1. **关注点分离**: core/data/ui 清晰分层
2. **平台抽象**: 通过 `expect/actual` 实现跨平台
3. **高度可定制**: 只提供逻辑，UI 完全由用户控制
4. **性能优化**: 使用 LazyList 按需加载，DataStore 缓存
5. **状态管理**: 完善的状态保存/恢复机制
6. **API 稳定**: 使用 BCV 保证兼容性

---

## 8. 总结

### 8.1 项目评估

| 维度 | 评价 | 说明 |
|------|------|------|
| **功能完整性** | ⭐⭐⭐⭐⭐ | 覆盖所有常见日历场景 |
| **跨平台支持** | ⭐⭐⭐⭐⭐ | Android/iOS/Desktop/Web |
| **可定制性** | ⭐⭐⭐⭐⭐ | 完全自定义 UI |
| **代码质量** | ⭐⭐⭐⭐⭐ | 清晰架构 + 完善测试 |
| **文档** | ⭐⭐⭐⭐⭐ | 详尽的使用指南和示例 |
| **维护活跃度** | ⭐⭐⭐⭐⭐ | 持续更新，响应社区 |

### 8.2 适用场景

- 需要高度自定义日历 UI 的应用
- 跨平台（Android/iOS/Desktop/Web）日历需求
- 需要周/月/年多种视图切换
- 热力图展示（如贡献图、习惯追踪）
- 日期范围选择、预约系统

### 8.3 学习价值

该项目是 Android/Compose 日历组件的优秀参考实现，值得学习：

- **模块化设计**: 清晰的层次划分和职责分离
- **状态管理**: Compose 状态管理最佳实践
- **跨平台适配**: Kotlin Multiplatform 项目结构
- **API 设计**: 灵活的 Composable API 设计
- **代码质量**: 完善的测试和 CI/CD 流程

---

*文档生成时间: 2024-12*
