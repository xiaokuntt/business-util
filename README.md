# business-util

[English](README.en.md)

`business-util` 是一个面向 Java 8 的多维业务配置匹配库。它把散落在业务代码中的条件判断、兜底规则和优先级排序，收敛为一次配置、重复查询的匹配模型。

典型场景包括：渠道策略、地区配置、用户分层、价格区间、时间窗口、灰度路由、营销规则和风控规则。

## 为什么使用这个项目

多维配置匹配看似只是几个 `if/else`，实际很快会遇到以下问题：

- 维度可以缺省，`地区 + 渠道 + 用户等级` 会产生大量组合和兜底路径。
- “匹配维度最多”与“高价值维度绝对优先”是两套不同的优先级规则。
- 除了相等判断，还可能需要前缀、区间、时间范围等业务谓词。
- 规则重复、匹配顺序和最终命中的具体配置值不容易排查。
- 配置量和维度增加后，逐组合扫描的成本会明显上升。

### 如果不使用 business-util

以“地区、渠道、用户等级”三个维度为例，如果约定配置中的空值代表兜底，并采用“匹配维度越多越优先”的规则，业务代码通常需要同时负责过滤、排序和结果聚合：

```java
public List<Rule> selectRules(Request request, List<Rule> rules) {
    List<Rule> matched = new ArrayList<>();
    for (Rule rule : rules) {
        int shape = shape(rule);
        if (shape == 0) {
            continue; // 没有有效维度的配置不参与匹配
        }
        if (matches(request.getRegion(), rule.getRegion())
                && matches(request.getChannel(), rule.getChannel())
                && matches(request.getUserLevel(), rule.getUserLevel())) {
            matched.add(rule);
        }
    }

    // NUMBER_OF_MATCHES：维度数量优先；数量相同时，region > channel > userLevel
    matched.sort((left, right) -> {
        int leftShape = shape(left);
        int rightShape = shape(right);
        int count = Integer.compare(
                Integer.bitCount(rightShape),
                Integer.bitCount(leftShape)
        );
        return count != 0 ? count : Integer.compare(rightShape, leftShape);
    });

    if (matched.isEmpty()) {
        return Collections.emptyList();
    }

    // 同一个最高优先级组合可能存在多条配置，需要一起返回
    int winningShape = shape(matched.get(0));
    return matched.stream()
            .filter(rule -> shape(rule) == winningShape)
            .collect(Collectors.toList());
}

private boolean matches(String requestValue, String configuredValue) {
    return isMissing(configuredValue)
            || (!isMissing(requestValue) && Objects.equals(requestValue, configuredValue));
}

private int shape(Rule rule) {
    int value = 0;
    if (!isMissing(rule.getRegion())) {
        value |= 1 << 2;
    }
    if (!isMissing(rule.getChannel())) {
        value |= 1 << 1;
    }
    if (!isMissing(rule.getUserLevel())) {
        value |= 1;
    }
    return value;
}

private boolean isMissing(String value) {
    return value == null || value.isEmpty();
}
```

这段代码还只处理了三个字符串维度和一种优先级模式。继续增加需求时，还要自行维护：

- `ABSOLUTE_VALUE` 的另一套排序算法。
- 前缀、数字区间、时间范围等不同类型的匹配分支。
- 多个模糊 key 同时命中后的稳定聚合顺序。
- 重复完整 key 的统计、告警、异常和样例限制。
- 命中维度及实际配置值的诊断输出。
- 大配置集下的索引、剪枝和树状查询。

使用 `business-util` 后，这些通用逻辑由匹配器统一承担，业务代码只需要声明配置、维度取值方式和必要的自定义谓词。

`business-util` 提供统一的建模方式来处理这些问题：

| 能力 | 带来的价值 |
| --- | --- |
| 配置驱动的多维匹配 | 业务代码不再维护成片的条件分支 |
| 两种内置优先级模式 | 明确控制“更具体”或“更重要”的规则优先 |
| 精确匹配与自定义谓词 | 同时支持普通值、前缀、数字区间和时间范围 |
| 缺失维度自动形成兜底 | 配置中的 `null` 或 `""` 不参与该条规则的维度组合 |
| `NAME:VALUE` 命中说明 | 直接知道本次实际匹配了哪些维度和值 |
| 创建阶段重复 key 自检查 | 在流量进入前发现歧义配置，可警告或阻止启动 |
| 普通/树状两种查询引擎 | 根据配置规模和命中分布选择执行方式 |

## 环境与安装

- Java 8 或更高版本
- Maven 3.x

Maven 依赖：

```xml
<dependency>
    <groupId>cn.ykccchen</groupId>
    <artifactId>business-util</artifactId>
    <version>1.1.0</version>
</dependency>
```

从源码构建：

```bash
git clone https://gitee.com/xiaokuntt/business-util.git
cd business-util
mvn clean install -Dgpg.skip=true
```

## 快速开始

假设请求对象 `Request` 和配置对象 `Rule` 都提供 `getRegion()`、`getChannel()`；`Rule` 另外提供 `getId()`。

```java
List<Rule> rules = Arrays.asList(
        new Rule("CN_APP", "CN", "APP"),
        new Rule("CN_DEFAULT", "CN", null),
        new Rule("GLOBAL", null, null)
);

PriorityFetcher<Request, Rule, String> fetcher = PriorityAssembler
        .from(Request.class, Rule.class, String.class)
        .initConfig(rules)
        .addPriorityMatchFunction("region", Request::getRegion, Rule::getRegion)
        .addPriorityMatchFunction("channel", Request::getChannel, Rule::getChannel)
        .create();

PriorityMatchResult<List<Rule>> result =
        fetcher.match(new Request("CN", "APP"));

System.out.println(result.getResult().get(0).getId()); // CN_APP
System.out.println(result.getName());                  // region_channel
System.out.println(result.getNameAndValue());          // region:CN_channel:APP
```

完整流程只有三步：

1. 通过 `PriorityAssembler` 加载配置。
2. 按重要性从高到低注册匹配维度；第一个维度的 priority 为 `0`。
3. 调用 `create()` 构建可重复使用的 `PriorityFetcher`，再执行查询。

配置中的 `null` 和真正的空字符串 `""` 表示该维度缺失。上例中的 `CN_DEFAULT` 因此只属于 `region` 组合，可作为更完整规则未命中时的兜底。所有维度都缺失的配置不会形成可查询规则。

### 核心对象

| 对象 | 职责 |
| --- | --- |
| `PriorityAssembler<S,C,K>` | 加载配置、注册维度并选择优先级和检查策略 |
| `PriorityMatchFunction<S,C,K>` | 描述一个维度如何从请求/配置取值，以及如何比较 |
| `PriorityFetcher<S,C,K>` | 保存构建后的索引并执行匹配 |
| `PriorityMatchResult<T>` | 返回优先级组合、`NAME:VALUE` 说明和业务配置 |

其中 `S` 是请求类型，`C` 是配置类型，`K` 是维度 key 类型。同一个装配器中的维度共用 `K`；维度值类型不同时，可以使用共同父类型（例如 `Object`），并在自定义谓词中完成类型判断。

## 获取一个结果或全部结果

```java
// 返回最高优先级结果；没有命中时返回 null
PriorityMatchResult<List<Rule>> winner = fetcher.match(request);

// 返回所有命中组合；按照优先级从高到低排列，没有命中时返回空集合
List<PriorityMatchResult<List<Rule>>> all = fetcher.match(request, true);
```

同一个优先级组合可能包含多条配置，所以结果值始终是 `List<C>`。一个组合最多产生一个 `PriorityMatchResult`。

## 配置优先级

维度注册顺序代表维度价值：越早注册，优先级越高。

### NUMBER_OF_MATCHES

默认模式。先比较参与匹配的维度数量，维度越多越优先；数量相同时，再按维度注册顺序比较。

```java
assembler.initPriorityHandler(PriorityMode.NUMBER_OF_MATCHES);
```

例如注册顺序为 `A、B、C、D`：

- `A+C+D` 高于 `A+B`，因为三维高于二维。
- `A+B+C` 高于 `A+B+D`，因为相同维度数下 `C` 比 `D` 更早注册。

### ABSOLUTE_VALUE

高价值维度具有绝对优先权，按每个维度“是否存在”的序列逐位比较。

```java
assembler.initPriorityHandler(PriorityMode.ABSOLUTE_VALUE);
```

例如注册顺序为 `A、B、C、D`：

- `A+B` 高于 `A+C+D`，因为 `B` 的价值高于 `C` 和 `D`。
- `A+B+C` 高于 `A+B`，因为前两维相同，前者还包含 `C`。

如果内置模式不满足业务需求，可以实现 `PriorityHandler` 并传给 `initPriorityHandler(...)`。

## 精确匹配、内置匹配器与自定义匹配

三个参数的 `addPriorityMatchFunction` 使用 Java `equals` 进行精确匹配：

```java
assembler.addPriorityMatchFunction(
        "region",
        Request::getRegion,
        Rule::getRegion
);
```

### 内置匹配器

`PriorityMatchers` 提供常用规则，所有匹配器的参数顺序固定为“请求值、配置值”：

| 类别 | 内置方法 |
| --- | --- |
| 通用 | `equal`、`notEqual` |
| 大小比较 | `greaterThan`、`greaterThanOrEqual`、`lessThan`、`lessThanOrEqual` |
| 区间 | `rangeContains`、`rangeNotContains`、`numberRangeContains`、`numberRangeNotContains`、`timeRangeContains`、`timeRangeNotContains`、`rangesOverlap`、`rangesDisjoint` |
| 字符串 | `stringEqualsIgnoreCase`、`stringNotEqualsIgnoreCase`、`stringStartsWith`、`stringStartsWithIgnoreCase`、`stringNotStartsWith`、`stringNotStartsWithIgnoreCase`、`stringEndsWith`、`stringEndsWithIgnoreCase`、`stringNotEndsWith`、`stringNotEndsWithIgnoreCase`、`stringContains`、`stringContainsIgnoreCase`、`stringNotContains`、`stringNotContainsIgnoreCase` |
| 正则 | `stringMatchesRegex`、`stringNotMatchesRegex` |
| 单值与集合 | `elementInCollection`、`elementNotInCollection` |
| 集合与单值 | `collectionContainsElement`、`collectionNotContainsElement` |
| 集合与集合 | `collectionIntersects`、`collectionContainsAll`、`collectionContainedBy`、`collectionDisjoint` |

请求值和配置值类型不同时，使用 `addPriorityMatcher(...)`。例如请求是数字，配置必须是明确的 `PriorityRange`，不能用单个数字冒充区间：

```java
PriorityAssembler<Request, Rule, Object> assembler = PriorityAssembler
        .from(Request.class, Rule.class, Object.class)
        .initConfig(ruleList)
        .addPriorityMatcher(
                "amount",
                Request::getAmount,
                Rule::getAmountRange,
                PriorityMatchers.<BigDecimal>numberRangeContains()
        );
```

创建区间：

```java
PriorityRange<BigDecimal> price = PriorityRange.closedOpen(
        new BigDecimal("10"), new BigDecimal("20")); // [10,20)
PriorityRange<Instant> activeTime =
        PriorityRange.closed(start, end);             // [start,end]
```

支持 `closed`、`open`、`closedOpen`、`openClosed`、`atLeast`、`greaterThan`、`atMost` 和 `lessThan`。非法、倒置或实际为空的区间在创建时直接抛出异常。

字符串和集合示例：

```java
assembler
        .addPriorityMatcher("path", Request::getPath, Rule::getPathPrefix,
                PriorityMatchers.stringStartsWith())
        .addPriorityMatcher("role", Request::getRole, Rule::getAllowedRoles,
                PriorityMatchers.<String>elementInCollection());
```

集合配置会在创建索引时复制为不可修改值，调用方后续修改原集合不会破坏匹配索引。成员/集合关系匹配会忽略集合内部的 `null` 和 `""`；`equal`、`notEqual` 则保留这些元素并按完整集合做 Java 相等性判断。为避免改变 Java 相等语义，`equal`、`notEqual` 的集合配置必须实现 `List` 或 `Set`；`ArrayDeque` 等其他 `Collection` 会在创建索引时明确抛出异常，可改用成员/集合关系匹配器或自定义匹配器。空集合本身始终是有效配置，并遵循相应匹配器的语义。

### 自定义匹配器

四个参数的重载接收 `BiPredicate<K, K>`。第一个参数是请求值，第二个参数是配置值。下面的规则允许请求文本匹配多个配置前缀：

```java
assembler.addPriorityMatchFunction(
        "prefix",
        Request::getPath,
        Rule::getPathPrefix,
        (requestPath, configuredPrefix) -> requestPath.startsWith(configuredPrefix)
);
```

请求值和配置值类型不同时，也可以实现 `PriorityMatcher<SV,CV>`：

```java
PriorityMatcher<BigDecimal, PriorityRange<BigDecimal>> matcher =
        (amount, range) -> range.contains(amount);
```

组件不会自动转换时区或数字精度。建议：

- 数字先统一为同一种类型和精度，例如 `BigDecimal`。
- 时间先统一到同一时间线，例如 `Instant`。
- 在加载配置前拒绝倒置区间和无效区间。
- 匹配器保持确定、无副作用；抛出的异常会原样传播。

## 空值与空字符

以下规则同时作用于请求值和配置值：

- `null`：缺失，不参与匹配。
- `""`：缺失，不参与匹配。
- `" "`、`"\t"` 等空白字符：有效值，不会自动 `trim()`。
- 非字符串 key：只判断 `null`，不会被当作空字符串。

如果业务希望忽略首尾空白，应在 getter 中先完成标准化。

## 查看实际命中的 NAME:VALUE

`PriorityMatchResult.getNameAndValue()` 展示实际命中的配置 key，而不是请求输入值：

```text
region:CN_channel:APP
```

- NAME 默认来自 `addPriorityMatchFunction(name, ...)`。
- 未提供名称时使用 `priority[n]`。
- 同一路径的维度用 `_` 连接。
- 自定义谓词命中多个配置 key 时，去重后用 `;` 连接，例如 `prefix:U;prefix:US`。

如需自定义显示规则，实现 `PriorityNameAndValueHandler`：

```java
public final class BusinessNameAndValueHandler
        implements PriorityNameAndValueHandler<Request, Rule, String> {
    @Override
    public String handle(String defaultName,
                         int priority,
                         Request source,
                         Rule config,
                         String sourceValue,
                         String matchedConfigValue) {
        return "业务-" + defaultName + "=" + matchedConfigValue;
    }
}

assembler.initPriorityNameAndValueHandler(
        new BusinessNameAndValueHandler()
);
```

处理器可以使用名称、priority、请求对象、配置对象、请求值和实际命中的配置值。返回 `null` 会忽略当前维度；异常会原样传播。

## 创建时检查重复完整 key

重复检查默认关闭。检查对象是配置的“完整有效 key”，即所有非 `null`、非 `""` 维度及其实际值。

```java
PriorityFetcher<Request, Rule, String> fetcher = assembler
        .initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING)
        .create();

DuplicateKeyCheckReport<Rule, String> report =
        fetcher.getDuplicateKeyCheckReport();

System.out.println(report.getDuplicateGroupCount());  // 重复组数
System.out.println(report.getDuplicateRecordCount()); // 重复配置总数
System.out.println(report.getSamples());              // 最多 10 条配置样例
```

| 等级 | 行为 |
| --- | --- |
| `OFF` | 默认值，不执行重复分组检查 |
| `WARNING` | 创建成功，保存报告，并通过 `java.util.logging` 输出一次警告 |
| `EXCEPTION` | 发现重复时中止 `create()` 并抛出 `DuplicateMatchKeyException` |

异常中的 `getReport()` 可以获取相同的统计信息。`WARNING` 不会删除配置，也不会改变匹配结果或顺序。

重复检查使用 key 的 Java `equals`，不会推断两个正则、区间或自定义谓词是否存在语义重叠。不同维度组合也不会互相判重。

## 普通模式与树状模式

默认使用按优先级组合执行的普通模式：

```java
PriorityFetcher<Request, Rule, String> levelFetcher = assembler.create();
```

配置创建后可以启用树状模式：

```java
PriorityFetcher<Request, Rule, String> treeFetcher = assembler.create().tree();
```

当前树引擎用于 `match(source, true)` 的全优先级查询；`match(source)` 仍按 processor 顺序提前结束。两种引擎返回相同的结果分组、顺序和 `nameAndValue`。

树模式并非在所有负载下都更快。项目内置的 50,000 条配置、14 个维度、16,383 种有效组合基准中：

| 查询负载 | 普通模式中位数 | 树模式中位数 | 结论 |
| --- | ---: | ---: | --- |
| 无命中 | 536.917 μs | 86.750 μs | 树模式约快 6.19 倍 |
| 选择性命中 | 2,811.833 μs | 759.250 μs | 树模式约快 3.70 倍 |
| 几乎全命中 | 17,247.042 μs | 33,765.667 μs | 普通模式约快 1.96 倍 |

这些数据只用于说明负载差异，不是固定性能承诺。`μs` 表示微秒，即百万分之一秒。请用真实配置和请求分布选择执行方式：

```bash
mvn -Dtest=PriorityFetcherPerformanceTest test
```

## 行为约定

- 只有配置中真实存在的维度组合会创建处理器，不会预先构造全部维度幂集。
- 自定义谓词命中多个 key 桶时，按 key 首次加载顺序合并；桶内保持配置加载顺序。
- 返回的配置列表是浅层副本，修改列表本身不会影响后续查询。
- `add(PriorityMatchFunction)` 的 priority 必须从 `0` 开始，并与注册顺序连续。
- `BiPredicate`、`PriorityHandler` 和 `PriorityNameAndValueHandler` 不能为 `null`。
- 建议在应用启动阶段完成配置加载和 `create()/tree()`，请求阶段只调用 `match(...)`。

## 测试

运行全部测试：

```bash
mvn test
```

项目测试覆盖精确匹配、空值/空字符、优先级顺序、数字与时间范围边界、重复 key 自检查、普通/树状等价性、大批量配置和性能对比。

## 参与贡献

1. Fork 项目并创建分支。
2. 增加实现及对应测试。
3. 运行 `mvn test`。
4. 提交 Pull Request。

## License

[Apache License 2.0](LICENSE)
