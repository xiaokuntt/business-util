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

2. 初始化配置
```java
assembler.initConfig(configList);
```

3. 添加匹配规则
```java
assembler.addPriorityMatchFunction("规则名称", source -> source.getKey(), config -> config.getKey());
```

4. 创建优先级获取器
```java
PriorityFetcher<Source, Config, Key> fetcher = assembler.create();
```

5. 执行匹配
```java
PriorityMatchResult<List<Config>> result = fetcher.match(source);
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