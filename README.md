

# business-util

## 介绍

`business-util` 是一个提供业务逻辑处理工具的 Java 项目，其核心功能是基于优先级的匹配逻辑处理，适用于复杂业务场景下的条件匹配与结果获取。该项目适用于需要高效、灵活匹配机制的业务系统。

## 软件架构

项目基于泛型编程和函数式编程思想构建，核心组件包括：

- **PriorityAssembler**: 构建匹配器的入口，提供初始化配置和匹配函数的方法。
- **PriorityFetcher**: 执行匹配逻辑的核心类，支持记录使用次数和剪枝优化。
- **PriorityMatchFunction**: 定义匹配函数的结构，支持通过字段提取进行匹配。
- **PriorityMatchProcessor**: 处理多个匹配函数的组合逻辑。
- **PriorityMatchResult**: 用于封装匹配结果。
- **PriorityHandler**: 接口，预留扩展处理逻辑。
- **PriorityMode**: 枚举实现的默认 `PriorityHandler`，用于定义匹配模式。

## 安装教程

1. 确保已安装 JDK 1.8 或更高版本。
2. 将本项目作为 Maven 依赖引入，添加以下内容到 `pom.xml`：

```xml
<dependency>
    <groupId>com.ykccchen</groupId>
    <artifactId>business-util</artifactId>
    <version>版本号</version>
</dependency>
```

3. 通过 Maven 构建项目：

```bash
mvn clean install
```

## 使用说明

1. **初始化配置**：使用 `PriorityAssembler.from(...).initConfig(...)` 初始化配置列表。
2. **添加匹配函数**：通过 `addPriorityMatchFunction(...)` 添加匹配规则。
3. **创建匹配器**：调用 `create()` 创建 `PriorityFetcher`。
4. **执行匹配**：调用 `match(...)` 方法执行匹配逻辑。

示例代码：

```java
PriorityFetcher<String, String, String> fetcher = PriorityAssembler
    .from(String.class, String.class, String.class)
    .initConfig(configList)
    .addPriorityMatchFunction("example", s -> s, c -> c)
    .create();

PriorityMatchResult<List<String>> result = fetcher.match("example");
```

## 参与贡献

欢迎提交 Pull Request 或提出 Issue 参与项目改进。请遵循以下流程：

1. Fork 项目仓库。
2. 创建新分支进行开发。
3. 提交代码并创建 PR。
4. 等待审核与合并。

请确保提交的代码符合编码规范，并通过所有测试用例。

## 特技

- **泛型支持**：支持任意类型的匹配源、配置与键提取。
- **优先级匹配**：通过优先级定义匹配顺序，避免冲突。
- **剪枝优化**：支持匹配树剪枝，提高性能。
- **使用记录**：可记录匹配过程的调用次数，便于统计和分析。

## License

本项目遵循 Apache 2.0 协议，详见 [LICENSE](LICENSE) 文件。