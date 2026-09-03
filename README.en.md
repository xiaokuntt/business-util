# business-util

[中文](README.md)

`business-util` is a Java 8 library for matching requests against multi-dimensional business configuration. It replaces scattered conditions, fallback branches, and ad hoc priority sorting with a configure-once, query-many matching model.

Typical uses include channel policies, regional configuration, customer segmentation, price or time windows, gradual rollout, marketing rules, and risk-control routing.

## Why use it?

Multi-dimensional matching quickly becomes difficult to maintain:

- Optional dimensions create many combinations and fallback paths.
- “Most dimensions win” and “the most valuable dimension wins” require different ordering rules.
- Real systems need prefix, range, or time-window matching in addition to equality.
- Duplicate rules and the exact values behind a match are hard to diagnose.
- Scanning every combination becomes expensive as configuration grows.

### Without business-util

Consider three dimensions: region, channel, and customer tier. If an empty configured value means fallback and the shape with the most dimensions wins, application code must handle filtering, ordering, and aggregation itself:

```java
public List<Rule> selectRules(Request request, List<Rule> rules) {
    List<Rule> matched = new ArrayList<>();
    for (Rule rule : rules) {
        int shape = shape(rule);
        if (shape == 0) {
            continue; // A rule with no effective dimension is not queryable
        }
        if (matches(request.getRegion(), rule.getRegion())
                && matches(request.getChannel(), rule.getChannel())
                && matches(request.getUserLevel(), rule.getUserLevel())) {
            matched.add(rule);
        }
    }

    // NUMBER_OF_MATCHES: dimension count first, then region > channel > userLevel
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

    // Several configurations can belong to the same winning shape
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

This already takes substantial code for only three string dimensions and one priority policy. Further requirements add more infrastructure:

- A separate ordering algorithm for `ABSOLUTE_VALUE`.
- Type-specific branches for prefixes, numeric ranges, and time windows.
- Stable aggregation when several predicate keys match.
- Duplicate complete-key counts, warnings, exceptions, and bounded samples.
- Diagnostics containing the matched dimensions and actual configured values.
- Indexing, pruning, and tree traversal for large configuration sets.

With `business-util`, the matcher owns these common concerns. Application code declares configuration, value extractors, and only the custom predicates that belong to its domain.

The library provides:

| Capability | Benefit |
| --- | --- |
| Configuration-driven matching | Removes large conditional decision trees from business code |
| Two built-in priority modes | Chooses either the most specific or the most valuable rule |
| Exact and predicate matching | Supports ordinary keys, prefixes, numeric ranges, and time ranges |
| Automatic fallback shapes | A `null` or `""` configuration value omits that dimension |
| `NAME:VALUE` diagnostics | Shows which concrete dimensions and values produced a result |
| Create-time duplicate checking | Warns about or rejects ambiguous complete keys before traffic arrives |
| Level and tree engines | Allows execution to be tuned for configuration size and hit distribution |

## Requirements and installation

- Java 8 or newer
- Maven 3.x

```xml
<dependency>
    <groupId>cn.ykccchen</groupId>
    <artifactId>business-util</artifactId>
    <version>1.1.0</version>
</dependency>
```

Build from source:

```bash
git clone https://gitee.com/xiaokuntt/business-util.git
cd business-util
mvn clean install -Dgpg.skip=true
```

## Quick start

Assume `Request` and `Rule` expose `getRegion()` and `getChannel()`, and `Rule` also exposes `getId()`.

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

The lifecycle has three steps:

1. Load configuration through `PriorityAssembler`.
2. Register dimensions from highest to lowest value; the first dimension has priority `0`.
3. Call `create()` once and reuse the resulting `PriorityFetcher` for queries.

A `null` or zero-length string in configuration means that the dimension is absent. `CN_DEFAULT` therefore belongs to the `region`-only shape and acts as a fallback. A configuration with no effective dimensions is not queryable.

### Core objects

| Object | Responsibility |
| --- | --- |
| `PriorityAssembler<S,C,K>` | Loads configuration, registers dimensions, and selects priority/checking policies |
| `PriorityMatchFunction<S,C,K>` | Defines extraction and comparison for one dimension |
| `PriorityFetcher<S,C,K>` | Holds the built index and executes queries |
| `PriorityMatchResult<T>` | Returns the priority shape, `NAME:VALUE` explanation, and business configuration |

`S` is the request type, `C` is the configuration type, and `K` is the dimension-key type. Dimensions in one assembler share `K`; use a common parent type such as `Object` and perform type checks in custom predicates when dimensions use different value types.

## Fetching the winner or every match

```java
// Highest-priority result, or null when nothing matches
PriorityMatchResult<List<Rule>> winner = fetcher.match(request);

// Every matching shape in priority order; an empty list when nothing matches
List<PriorityMatchResult<List<Rule>>> all = fetcher.match(request, true);
```

One priority shape can contain several configurations, so each result value is a `List<C>`. A shape produces at most one `PriorityMatchResult`.

## Priority modes

Registration order defines dimension value: earlier dimensions have higher priority.

### NUMBER_OF_MATCHES

This is the default. Shapes with more dimensions come first; ties are resolved by registration order.

```java
assembler.initPriorityHandler(PriorityMode.NUMBER_OF_MATCHES);
```

For dimensions registered as `A, B, C, D`, `A+C+D` ranks above `A+B`, and `A+B+C` ranks above `A+B+D`.

### ABSOLUTE_VALUE

Earlier dimensions have absolute precedence. Presence is compared one dimension at a time.

```java
assembler.initPriorityHandler(PriorityMode.ABSOLUTE_VALUE);
```

For `A, B, C, D`, `A+B` ranks above `A+C+D`, while `A+B+C` ranks above `A+B`.

Implement `PriorityHandler` and pass it to `initPriorityHandler(...)` when neither built-in order matches the business policy.

## Exact, built-in, and custom matching

The three-argument method uses Java equality:

```java
assembler.addPriorityMatchFunction(
        "region",
        Request::getRegion,
        Rule::getRegion
);
```

### Built-in matchers

`PriorityMatchers` provides common rules. Every matcher receives the source value first and the configured value second:

| Category | Built-ins |
| --- | --- |
| General | `equal`, `notEqual` |
| Comparison | `greaterThan`, `greaterThanOrEqual`, `lessThan`, `lessThanOrEqual` |
| Range | `rangeContains`, `rangeNotContains`, `numberRangeContains`, `numberRangeNotContains`, `timeRangeContains`, `timeRangeNotContains`, `rangesOverlap`, `rangesDisjoint` |
| String | `stringEqualsIgnoreCase`, `stringNotEqualsIgnoreCase`, `stringStartsWith`, `stringStartsWithIgnoreCase`, `stringNotStartsWith`, `stringNotStartsWithIgnoreCase`, `stringEndsWith`, `stringEndsWithIgnoreCase`, `stringNotEndsWith`, `stringNotEndsWithIgnoreCase`, `stringContains`, `stringContainsIgnoreCase`, `stringNotContains`, `stringNotContainsIgnoreCase` |
| Regex | `stringMatchesRegex`, `stringNotMatchesRegex` |
| Element/config collection | `elementInCollection`, `elementNotInCollection` |
| Source collection/element | `collectionContainsElement`, `collectionNotContainsElement` |
| Collection/collection | `collectionIntersects`, `collectionContainsAll`, `collectionContainedBy`, `collectionDisjoint` |

Use `addPriorityMatcher(...)` when source and configured values have different types. A numeric request uses an explicit configured `PriorityRange`; a scalar number is not accepted as a range:

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

Create numeric or time ranges with the same immutable value type:

```java
PriorityRange<BigDecimal> price = PriorityRange.closedOpen(
        new BigDecimal("10"), new BigDecimal("20")); // [10,20)
PriorityRange<Instant> activeTime =
        PriorityRange.closed(start, end);             // [start,end]
```

Factories include `closed`, `open`, `closedOpen`, `openClosed`, `atLeast`, `greaterThan`, `atMost`, and `lessThan`. Invalid, reversed, or effectively empty ranges fail at construction.

String and collection rules are equally direct:

```java
assembler
        .addPriorityMatcher("path", Request::getPath, Rule::getPathPrefix,
                PriorityMatchers.stringStartsWith())
        .addPriorityMatcher("role", Request::getRole, Rule::getAllowedRoles,
                PriorityMatchers.<String>elementInCollection());
```

Configured collections are copied into stable unmodifiable index keys. Membership and set operations ignore missing members (`null` and `""`); `equal` and `notEqual` retain them and apply Java equality to the complete collection. To preserve Java equality semantics, collection configurations used with `equal` or `notEqual` must implement `List` or `Set`; other `Collection` types such as `ArrayDeque` fail explicitly while the index is created and should use a membership/set matcher or a custom matcher. An empty collection is always a valid configured value and follows the selected matcher's semantics.

### Custom matchers

The four-argument overload accepts a `BiPredicate<K, K>`. Its first argument is the request value and its second argument is a configured value:

```java
assembler.addPriorityMatchFunction(
        "prefix",
        Request::getPath,
        Rule::getPathPrefix,
        (requestPath, configuredPrefix) -> requestPath.startsWith(configuredPrefix)
);
```

For different source/configuration value types, implement `PriorityMatcher<SV,CV>`:

```java
PriorityMatcher<BigDecimal, PriorityRange<BigDecimal>> matcher =
        (amount, range) -> range.contains(amount);
```

The library does not infer numeric precision or time-zone conversion. Normalize values before matching, reject invalid ranges while loading configuration, and keep matchers deterministic and side-effect free. Matcher exceptions propagate unchanged.

## Null and empty values

- `null`: absent and never matched.
- `""`: absent and never matched.
- `" "`, `"\t"`, and other whitespace strings: valid values; no implicit trimming occurs.
- Non-string keys: only `null` is considered absent.

Normalize whitespace in the getter when the business domain treats it as empty.

## Explaining a match with NAME:VALUE

`PriorityMatchResult.getNameAndValue()` shows actual matched configuration keys rather than only request values:

```text
region:CN_channel:APP
```

Names come from `addPriorityMatchFunction(name, ...)`; unnamed dimensions use `priority[n]`. Dimensions in one path use `_`. When a predicate matches several distinct key paths, de-duplicated paths use `;`, for example `prefix:U;prefix:US`.

Implement `PriorityNameAndValueHandler` to customize each rendered pair:

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
        return "business-" + defaultName + "=" + matchedConfigValue;
    }
}

assembler.initPriorityNameAndValueHandler(
        new BusinessNameAndValueHandler()
);
```

The handler receives the registered name, priority, source/configuration objects, source value, and actual matched configuration value. Returning `null` omits the dimension; exceptions propagate unchanged.

## Create-time duplicate complete-key checking

Duplicate checking is disabled by default. A complete effective key contains all configuration dimensions that are neither `null` nor `""`.

```java
PriorityFetcher<Request, Rule, String> fetcher = assembler
        .initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING)
        .create();

DuplicateKeyCheckReport<Rule, String> report =
        fetcher.getDuplicateKeyCheckReport();

System.out.println(report.getDuplicateGroupCount());
System.out.println(report.getDuplicateRecordCount());
System.out.println(report.getSamples()); // At most 10 configuration samples
```

| Level | Behavior |
| --- | --- |
| `OFF` | Default; skip duplicate grouping |
| `WARNING` | Create the fetcher, retain the report, and emit one `java.util.logging` warning |
| `EXCEPTION` | Abort `create()` with `DuplicateMatchKeyException` when duplicates exist |

The exception exposes the same report through `getReport()`. WARNING does not remove configurations or change results.

Duplicate detection uses Java equality. It does not infer semantic overlap between regex, range, or custom predicates, and it does not compare different dimension shapes with each other.

## Level and tree engines

The level engine is the default:

```java
PriorityFetcher<Request, Rule, String> levelFetcher = assembler.create();
```

Enable the tree engine after creation:

```java
PriorityFetcher<Request, Rule, String> treeFetcher = assembler.create().tree();
```

The tree engine currently serves `match(source, true)` all-priority queries. `match(source)` still exits early in processor order. Both engines preserve identical grouping, order, and `nameAndValue` output.

The tree engine is not universally faster. A repository benchmark with 50,000 configurations, 14 dimensions, and 16,383 active shapes produced:

| Workload | Level median | Tree median | Observation |
| --- | ---: | ---: | --- |
| No match | 536.917 μs | 86.750 μs | Tree about 6.19x faster |
| Selective hit | 2,811.833 μs | 759.250 μs | Tree about 3.70x faster |
| Near-full hit | 17,247.042 μs | 33,765.667 μs | Level about 1.96x faster |

These figures demonstrate workload differences and are not a performance guarantee. `μs` means microseconds, or one millionth of a second. Benchmark with representative data:

```bash
mvn -Dtest=PriorityFetcherPerformanceTest test
```

## Behavioral contracts

- Only dimension shapes present in configuration become processors; the full dimension power set is not precomputed.
- Predicate key buckets merge in first-seen key order and retain configuration-load order within each bucket.
- Result lists are detached shallow copies.
- `add(PriorityMatchFunction)` priorities must start at `0` and remain continuous with registration order.
- `BiPredicate`, `PriorityHandler`, and `PriorityNameAndValueHandler` must not be `null`.
- Build configuration and call `create()/tree()` during application initialization; use `match(...)` during request processing.

## Tests

```bash
mvn test
```

The suite covers equality, null/empty values, priority ordering, numeric and time-range boundaries, duplicate checking, level/tree equivalence, large configuration sets, and performance comparison.

## Contributing

1. Fork the repository and create a branch.
2. Add the implementation and corresponding tests.
3. Run `mvn test`.
4. Open a pull request.

## License

[Apache License 2.0](LICENSE)
