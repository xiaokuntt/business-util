package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.PriorityAssembler;
import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.handler.PriorityMode;

import java.util.Collections;
import java.util.Map;

/** Runs the high-dimensional regression in a heap-limited child JVM. */
public final class PriorityFetcherScalabilityProbe {

    private PriorityFetcherScalabilityProbe() {
    }

    public static void main(String[] args) {
        for (PriorityMode mode : PriorityMode.values()) {
            PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler = assembler(mode);
            for (int index = 1; index <= 25; index++) {
                final String key = "p" + index;
                assembler.addPriorityMatchFunction(key, source -> source.get(key), config -> config.get(key));
            }
            PriorityFetcher<Map<String, String>, Map<String, String>, String> fetcher = assembler.create();
            if (!fetcher.getProcessorList().isEmpty() || fetcher.match(Collections.emptyMap()) != null) {
                throw new AssertionError("Empty configuration created active processors for " + mode);
            }
        }
        System.out.println("SCALABILITY_OK");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler(PriorityMode mode) {
        return PriorityAssembler.from((Class) Map.class, (Class) Map.class, String.class)
                .initConfig(Collections.emptyList())
                .initPriorityHandler(mode);
    }
}
