package edu.aitu.daa;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Random;

/** Deterministic inputs, fresh state, three warm-ups and five measured repetitions. */
public final class Benchmark {
    static final int[] SIZES = {100, 1_000, 10_000, 100_000};
    private static final int WARMUPS = 3;
    private static final int RUNS = 5;
    private static volatile long sink;
    private record Sample(long nanos, long steps, long moves, long comparisons) { }
    @FunctionalInterface private interface Trial { Sample run(); }

    static int[] data(int n) {
        Random random = new Random(42);
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = random.nextInt(Integer.MAX_VALUE);
        return values;
    }

    private static Sample sample(long elapsed, Metrics metrics) {
        return new Sample(elapsed, metrics.steps(), metrics.moves(), metrics.comparisons());
    }

    private static Sample sequenceTrial(String workload, String variant, boolean linked, int[] values) {
        IntSequence sequence = linked ? new MyLinkedList() : new DynamicArray();
        for (int value : values) sequence.add(value);
        int n = values.length;
        Random random = new Random(42);
        int[] queries = new int[workload.equals("W1") ? 10_000 : 1_000];
        long expected = 0;
        for (int i = 0; i < queries.length; i++) {
            if (workload.equals("W1")) {
                queries[i] = random.nextInt(n);
                expected += values[queries[i]];
            } else if (workload.equals("W2")) {
                queries[i] = i % 2 == 0 ? values[random.nextInt(n)] : -i - 1;
                if (i % 2 == 0) expected++;
            } else {
                queries[i] = random.nextInt();
                expected += queries[i];
            }
        }
        sequence.metrics().reset(); // setup and query generation are not measured
        long checksum = 0;
        long start = System.nanoTime();
        switch (workload) {
            case "W1" -> { for (int index : queries) checksum += sequence.get(index); }
            case "W2" -> { for (int value : queries) if (sequence.contains(value)) checksum++; }
            case "W3" -> {
                int index = variant.equals("head") ? 0 : n / 2;
                for (int value : queries) sequence.add(index, value);
                for (int i = 0; i < queries.length; i++) checksum += sequence.remove(index);
            }
            default -> throw new IllegalArgumentException(workload);
        }
        long elapsed = System.nanoTime() - start;
        Sample result = sample(elapsed, sequence.metrics());
        if (checksum != expected || sequence.size() != n) throw new AssertionError("Sequence workload failed");
        sink = checksum;
        return result;
    }

    private static Sample heapTrial(int[] values) {
        MinHeap heap = new MinHeap();
        int[] extracted = new int[values.length];
        long start = System.nanoTime();
        for (int value : values) heap.insert(value);
        for (int i = 0; i < extracted.length; i++) extracted[i] = heap.extractMin();
        long elapsed = System.nanoTime() - start;
        Sample result = sample(elapsed, heap.metrics());
        long sum = 0;
        long expected = 0;
        for (int i = 0; i < values.length; i++) {
            if (i > 0 && extracted[i - 1] > extracted[i]) throw new AssertionError("Unsorted heap output");
            sum += extracted[i]; expected += values[i];
        }
        if (sum != expected || heap.size() != 0) throw new AssertionError("Heap workload failed");
        sink = sum;
        return result;
    }

    private static Sample buildTrial(boolean floyd, int[] values) {
        long start = System.nanoTime();
        MinHeap heap;
        if (floyd) heap = MinHeap.buildHeap(values);
        else {
            heap = new MinHeap();
            for (int value : values) heap.insert(value);
        }
        long elapsed = System.nanoTime() - start;
        Sample result = sample(elapsed, heap.metrics());
        int[] snapshot = heap.snapshot();
        for (int i = 1; i < snapshot.length; i++)
            if (snapshot[(i - 1) / 2] > snapshot[i]) throw new AssertionError("Invalid built heap");
        sink = heap.peekMin();
        return result;
    }

    private static void measure(PrintWriter results, PrintWriter raw, String workload,
                                String variant, String structure, int n, Trial trial) {
        for (int i = 0; i < WARMUPS; i++) trial.run();
        Sample[] samples = new Sample[RUNS];
        for (int run = 0; run < RUNS; run++) {
            samples[run] = trial.run();
            Sample s = samples[run];
            if (run > 0 && (s.steps != samples[0].steps || s.moves != samples[0].moves
                    || s.comparisons != samples[0].comparisons)) throw new AssertionError("Non-deterministic counters");
            raw.printf(Locale.ROOT, "%s,%s,%s,%d,%d,%.6f,%d,%d,%d%n", workload, variant,
                    structure, n, run + 1, s.nanos / 1_000_000.0, s.steps, s.moves, s.comparisons);
        }
        // Five-element insertion sort avoids a production dependency on collection helpers.
        for (int i = 1; i < RUNS; i++) {
            Sample current = samples[i];
            int j = i - 1;
            while (j >= 0 && samples[j].nanos > current.nanos) {
                samples[j + 1] = samples[j]; j--;
            }
            samples[j + 1] = current;
        }
        Sample median = samples[RUNS / 2];
        results.printf(Locale.ROOT, "%s,%s,%s,%d,%.6f,%d,%d,%d%n", workload, variant, structure,
                n, median.nanos / 1_000_000.0, median.steps, median.moves, median.comparisons);
        results.flush(); raw.flush();
        System.out.printf(Locale.ROOT, "%s %-10s %-13s n=%6d  median=%10.4f ms%n",
                workload, variant, structure, n, median.nanos / 1_000_000.0);
    }

    public static void main(String[] args) throws IOException {
        Path directory = Path.of("results");
        Files.createDirectories(directory);
        // Exercise every code path before measuring even the smallest case.
        int[] warmData = data(1_000);
        for (int round = 0; round < 30; round++) {
            for (boolean linked : new boolean[]{false, true}) {
                sequenceTrial("W1", "-", linked, warmData);
                sequenceTrial("W2", "-", linked, warmData);
                sequenceTrial("W3", "head", linked, warmData);
                sequenceTrial("W3", "middle", linked, warmData);
            }
            heapTrial(warmData);
            buildTrial(true, warmData);
            buildTrial(false, warmData);
        }
        String header = "workload,variant,structure,n,time_ms,steps,moves,comparisons";
        String rawHeader = "workload,variant,structure,n,run,time_ms,steps,moves,comparisons";
        try (PrintWriter results = new PrintWriter(Files.newBufferedWriter(directory.resolve("results.csv")));
             PrintWriter raw = new PrintWriter(Files.newBufferedWriter(directory.resolve("raw_runs.csv")))) {
            results.println(header); raw.println(rawHeader);
            for (int n : SIZES) {
                int[] values = data(n);
                for (String workload : new String[]{"W1", "W2", "W3"}) {
                    String[] variants = workload.equals("W3") ? new String[]{"head", "middle"} : new String[]{"-"};
                    for (String variant : variants) for (boolean linked : new boolean[]{false, true}) {
                        String structure = linked ? "MyLinkedList" : "DynamicArray";
                        measure(results, raw, workload, variant, structure, n,
                                () -> sequenceTrial(workload, variant, linked, values));
                    }
                }
                measure(results, raw, "W4", "-", "MinHeap", n, () -> heapTrial(values));
            }
        }
        try (PrintWriter results = new PrintWriter(Files.newBufferedWriter(directory.resolve("build_heap.csv")));
             PrintWriter raw = new PrintWriter(Files.newBufferedWriter(directory.resolve("build_heap_raw.csv")))) {
            results.println(header); raw.println(rawHeader);
            for (int n : SIZES) for (String order : new String[]{"random", "descending"}) {
                int[] values = data(n);
                if (order.equals("descending")) for (int i = 0; i < n; i++) values[i] = n - i;
                for (boolean floyd : new boolean[]{true, false})
                    measure(results, raw, "BUILD", order, floyd ? "Floyd" : "RepeatedInsert", n,
                            () -> buildTrial(floyd, values));
            }
        }
        Files.writeString(directory.resolve("environment.txt"),
                "Generated: " + Instant.now() + "\nJava: " + System.getProperty("java.version")
                + "\nVM: " + System.getProperty("java.vm.name") + "\nOS: " + System.getProperty("os.name")
                + " " + System.getProperty("os.arch") + "\nAvailable processors: " + Runtime.getRuntime().availableProcessors()
                + "\nSeed: 42\nGlobal warm-up: 30 rounds of all operations at n=1000\nWarm-up trials per case: " + WARMUPS + "\nMeasured trials per case: " + RUNS
                + "\nTiming: System.nanoTime; median; instrumented operations; single JVM\n"
                + "W1-W3 exclude initial filling, random generation and validation.\n"
                + "W3 inserts all 1000 values, then removes all 1000 at fixed original n/2 or 0.\n"
                + "W4 includes insertion and extraction; excludes sorted-order validation.\n");
        System.out.println("Benchmark finished. Checksums validated; sink=" + sink);
    }
}
