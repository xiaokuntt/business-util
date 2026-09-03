package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.PriorityAssembler;
import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.PriorityMatchResult;
import cn.ykccchen.businessutil.match.handler.PriorityMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class MatchingFixtures {

    private MatchingFixtures() {
    }

    static Source source(String a, String b, String c, String d) {
        return new Source(a, b, c, d);
    }

    static Config config(String id, String a, String b, String c, String d) {
        return new Config(id, a, b, c, d);
    }

    static PriorityAssembler<Source, Config, String> assembler(List<Config> configs, PriorityMode mode) {
        return PriorityAssembler.from(Source.class, Config.class, String.class)
                .initConfig(configs)
                .initPriorityHandler(mode)
                .addPriorityMatchFunction("A", Source::getA, Config::getA)
                .addPriorityMatchFunction("B", Source::getB, Config::getB)
                .addPriorityMatchFunction("C", Source::getC, Config::getC)
                .addPriorityMatchFunction("D", Source::getD, Config::getD);
    }

    static FetcherPair pair(List<Config> configs, PriorityMode mode) {
        PriorityAssembler<Source, Config, String> assembler = assembler(configs, mode);
        return new FetcherPair(assembler.create(), assembler.create().tree());
    }

    static List<List<String>> project(List<PriorityMatchResult<List<Config>>> results) {
        List<List<String>> projection = new ArrayList<>();
        for (PriorityMatchResult<List<Config>> result : results) {
            List<String> group = new ArrayList<>();
            group.add(result.getName());
            group.add(String.valueOf(result.getLevel()));
            for (Config config : result.getResult()) {
                group.add(config.getId());
            }
            projection.add(group);
        }
        return projection;
    }

    static List<String> ids(PriorityMatchResult<List<Config>> result) {
        List<String> ids = new ArrayList<>();
        for (Config config : result.getResult()) {
            ids.add(config.getId());
        }
        return ids;
    }

    static List<String> ids(List<PriorityMatchResult<List<Config>>> results) {
        List<String> ids = new ArrayList<>();
        for (PriorityMatchResult<List<Config>> result : results) {
            for (Config config : result.getResult()) {
                ids.add(config.getId());
            }
        }
        return ids;
    }

    static final class FetcherPair {
        private final PriorityFetcher<Source, Config, String> level;
        private final PriorityFetcher<Source, Config, String> tree;

        FetcherPair(PriorityFetcher<Source, Config, String> level,
                    PriorityFetcher<Source, Config, String> tree) {
            this.level = level;
            this.tree = tree;
        }

        List<PriorityFetcher<Source, Config, String>> both() {
            return Arrays.asList(level, tree);
        }

        PriorityFetcher<Source, Config, String> level() {
            return level;
        }

        PriorityFetcher<Source, Config, String> tree() {
            return tree;
        }
    }

    static final class Source {
        private final String a;
        private final String b;
        private final String c;
        private final String d;

        Source(String a, String b, String c, String d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        String getA() {
            return a;
        }

        String getB() {
            return b;
        }

        String getC() {
            return c;
        }

        String getD() {
            return d;
        }
    }

    static final class Config {
        private final String id;
        private final String a;
        private final String b;
        private final String c;
        private final String d;

        Config(String id, String a, String b, String c, String d) {
            this.id = id;
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        String getId() {
            return id;
        }

        String getA() {
            return a;
        }

        String getB() {
            return b;
        }

        String getC() {
            return c;
        }

        String getD() {
            return d;
        }
    }
}
