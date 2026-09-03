package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.PriorityAssembler;
import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.PriorityMatchFunction;
import cn.ykccchen.businessutil.match.PriorityMatchProcessor;
import cn.ykccchen.businessutil.match.handler.PriorityHandler;
import cn.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityFetcherScalabilityTest {

    @Test
    void emptyConfigurationsDoNotMaterializeThePowerSet() throws Exception {
        String javaExecutable = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(javaExecutable, "-Xmx64m", "-cp",
                System.getProperty("java.class.path"), PriorityFetcherScalabilityProbe.class.getName())
                .redirectErrorStream(true)
                .start();
        boolean completed = process.waitFor(5, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
        }
        assertTrue(completed, "25-dimensional initialization exceeded the process timeout");
        String output = read(process.getInputStream());
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("SCALABILITY_OK"), output);
    }

    @Test
    void onlyDistinctConfigurationShapesBecomeProcessors() {
        Map<String, String> oneDimension = record("one", "p1", "x");
        Map<String, String> twoDimensions = record("two", "p1", "x", "p25", "x");
        Map<String, String> request = record(null, "p1", "x", "p25", "x");

        for (PriorityMode mode : PriorityMode.values()) {
            PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler =
                    mapAssembler(Arrays.asList(oneDimension, twoDimensions), mode);
            addRules(assembler, 25, null);
            PriorityFetcher<Map<String, String>, Map<String, String>, String> fetcher = assembler.create();

            assertEquals(2, fetcher.getProcessorList().size());
            assertEquals("two", fetcher.match(request).getResult().get(0).get("id"));
            assertEquals("two", fetcher.tree().match(request).getResult().get(0).get("id"));
        }
    }

    @Test
    void configurationGettersAreEvaluatedOnceDuringCreate() {
        Map<String, String> first = record("first", "p1", "x");
        Map<String, String> second = record("second", "p2", "x");
        AtomicInteger configGetterCalls = new AtomicInteger();
        PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler =
                mapAssembler(Arrays.asList(first, second), PriorityMode.NUMBER_OF_MATCHES);
        addRules(assembler, 10, configGetterCalls);

        assembler.create();

        assertEquals(20, configGetterCalls.get());
    }

    @Test
    void customHandlersRemainCompatibleThroughTheDefaultActiveShapeAdapter() {
        Map<String, String> config = record("only", "p1", "x");
        PriorityHandler customHandler = new PriorityHandler() {
            @Override
            public <S, C, K> List<PriorityMatchProcessor<S, C, K>> initPriorityHandlerList(
                    List<PriorityMatchFunction<S, C, K>> functions) {
                return Collections.singletonList(
                        new PriorityMatchProcessor<>(Collections.singletonList(functions.get(0))));
            }
        };
        PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler =
                mapAssembler(Collections.singletonList(config), PriorityMode.NUMBER_OF_MATCHES)
                        .initPriorityHandler(customHandler);
        addRules(assembler, 2, null);

        PriorityFetcher<Map<String, String>, Map<String, String>, String> fetcher = assembler.create();

        assertEquals(1, fetcher.getProcessorList().size());
        assertEquals("only", fetcher.match(record(null, "p1", "x")).getResult().get(0).get("id"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static PriorityAssembler<Map<String, String>, Map<String, String>, String> mapAssembler(
            List<Map<String, String>> configs, PriorityMode mode) {
        return PriorityAssembler.from((Class) Map.class, (Class) Map.class, String.class)
                .initConfig(configs)
                .initPriorityHandler(mode);
    }

    private static void addRules(PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler,
                                 int count,
                                 AtomicInteger configGetterCalls) {
        for (int i = 1; i <= count; i++) {
            final String key = "p" + i;
            assembler.addPriorityMatchFunction(key,
                    source -> source.get(key),
                    config -> {
                        if (configGetterCalls != null) {
                            configGetterCalls.incrementAndGet();
                        }
                        return config.get(key);
                    });
        }
    }

    private static Map<String, String> record(String id, String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        if (id != null) {
            result.put("id", id);
        }
        for (int i = 0; i < values.length; i += 2) {
            result.put(values[i], values[i + 1]);
        }
        return result;
    }

    private static String read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
