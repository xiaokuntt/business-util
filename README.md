# business-util

## 介绍
业务工具集，为解决复杂的业务规则和业务配置，提供一种工具类解决方案，提高开发效率和代码可读性

## 软件架构
该工具库主要由以下核心组件构成：
- PriorityAssembler：优先级装配器，用于构建优先级匹配规则
- PriorityFetcher：优先级获取器，执行匹配逻辑
- PriorityMatchFunction：优先级匹配函数，定义匹配规则
- PriorityMatchProcessor：优先级匹配处理器，处理匹配流程
- PriorityMatchProcessorTree：优先级匹配处理器树，构建匹配树结构
- PriorityMatchResult：匹配结果对象
- PriorityHandler：优先级处理接口
- PriorityMatchType：匹配类型枚举
- PriorityMode：优先级模式枚举

## 安装教程
1. 将项目克隆到本地：`git clone https://gitee.com/xiaokuntt/business-util`
2. 使用Maven构建项目：`mvn clean install`

## MAVEN使用
```java
<dependency>
    <groupId>cn.ykccchen</groupId>
    <artifactId>business-util</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
        
```

## 使用说明
1. 创建优先级匹配规则
```java
PriorityAssembler<Source, Config, Key> assembler = PriorityAssembler.from(Source.class, Config.class, Key.class);
```

2. 初始化配置，将配置进行加载
```java
assembler.initConfig(configList);
```

3. 添加匹配规则
```java
//默认为相等的匹配规则
assembler.addPriorityMatchFunction("规则名称", source -> source.getKey(), config -> config.getKey());

//如果是正则或者区间匹配，可以使用boolean匹配模式
priorityAssembler.addPriorityMatchFunction("规则名称" , source -> source.getKey(), config -> config.getKey() , (source, config) -> {
            if (config instanceof Range && source instanceof Integer) {
                Range<Integer> range = (Range) config;
                return range.contains((Integer) source);
            }
            return false;
        })
```
 :exclamation: 特别注意，在同一个优先级下（[a,b,c]级别），不管是boolean还是string等KEY对象，应该有且只有一个值能匹配上 :exclamation: 



4. 设置优先级处理器模式，可以自定义，内置2种模式，不选择默认 PriorityMode.NUMBER_OF_MATCHES 
```java
/**
 * 配置值数量优先
 * PriorityMode.NUMBER_OF_MATCHES 
 * A B C D 4个配置维度
 * A C D 优先级大于 A B
 * A B C  优先级大于 A B D
 * 3个维度一定大于2个维度
 */
assembler.initPriorityHandler(PriorityMode.NUMBER_OF_MATCHES);
/**
  * 绝对值优先
  * A B C D 4个配置维度
  * A B  优先级大于 A C D
  * A B C  优先级大于 A B
  * A B C  优先级大于 A B D
  * 绝对价值维度
  */
assembler.initPriorityHandler(PriorityMode.ABSOLUTE_VALUE);

```

5. 创建优先级获取器
```java
//默认会做剪枝，删除不存在的优先级校验项
PriorityFetcher<Source, Config, Key> fetcher = assembler.create();
// 优先级匹配初始化后可以转换为树，非必须项，当存在叶子节点数据 >= 8 时，才比较有性价比， priorityFetcher.getProcessorList() >= 8
PriorityFetcher<Map<String, Serializable>, Map<String, Serializable>, Serializable> priorityFetcher = priorityAssembler.create().tree();
```

5. 执行匹配
```java
// 获取优先级最高的配置
PriorityMatchResult<List<Config>> result = fetcher.match(source);
// 获取全部能匹配的数据，集合顺序为优先级顺序
List<PriorityMatchResult<List<Map<String, Serializable>>>> resultList = fetcher.match(source, true);
```


## 简单demo
```java
    // 这里使用有序的哈希MAP， 保证优先级的顺序的对的，这个逻辑必须
    List<Function<Map<String, Serializable>, Serializable>> mapByValue = new ArrayList<>();
    // key值为维度的获取方式， value值为价值，值越高价值越大
    mapByValue.add(map -> map.get("p1"));
    mapByValue.add(map -> map.get("p2"));
    mapByValue.add(map -> map.get("p3"));
    mapByValue.add(map -> map.get("p4"));
    //初始化配置优先级组合集
    PriorityAssembler<Map<String, Serializable>, Map<String, Serializable>, Serializable> priorityAssembler = PriorityAssembler.from(new PriorityAssembler.TypeReference<Map<String, Serializable>>() {
            }, new PriorityAssembler.TypeReference<Map<String, Serializable>>() {
            }, new PriorityAssembler.TypeReference<Serializable>() {
            })
            .initConfig(new ArrayList())
            .initPriorityHandler(PriorityMode.ABSOLUTE_VALUE);
    //初始化数据获取逻辑
    for (int i = 0; i < mapByValue.size()-1; i++) {
        priorityAssembler.addPriorityMatchFunction("配置"+ (i+1), mapByValue.get(i), mapByValue.get(i));
    }
    priorityAssembler.addPriorityMatchFunction("配置"+ 4, mapByValue.get(3), mapByValue.get(3), (source, config) -> {
        if (config instanceof Range && source instanceof Integer) {
            Range<Integer> range = (Range) config;
            return range.contains((Integer) source);
        }
        return false;
    });


    // 创建对象
    PriorityFetcher<Map<String, Serializable>, Map<String, Serializable>, Serializable> priorityFetcher = priorityAssembler.create().tree();

    // 匹配
    for (Map<String, Serializable> req : new ArrayList()) {
        PriorityMatchResult<List<Map<String, Serializable>>> match = priorityFetcher.match(req);
        if (match != null) {
            System.out.println("需求：" + req.toString() + "， 配置：" + match.toString() +",配置值:"+match.getResult());
        } else {
            System.out.println("需求：" + req.toString() + "未命中配置");
        }
    }

```

## 参与贡献
1. Fork项目
2. 创建新分支
3. 提交代码
4. 创建Pull Request

## 特技
1. 支持多种优先级匹配模式（NUMBER_OF_MATCHES和ABSOLUTE_VALUE）
2. 提供树形结构匹配算法
3. 支持模糊匹配和精确匹配两种模式
4. 可处理多级优先级配置
5. 提供详细的匹配结果记录

## License
该项目遵循Apache-2.0协议，详情请查看LICENSE文件。