package edu.aitu.daa;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;

/** Reachable object graph, including backing storage, nodes and constant Metrics overhead. */
public final class MemoryBenchmark {
    public static void main(String[] args) throws IOException {
        Files.createDirectories(Path.of("results"));
        Files.writeString(Path.of("results/jol_environment.txt"), VM.current().details());
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(Path.of("results/memory.csv")));
             PrintWriter details = new PrintWriter(Files.newBufferedWriter(Path.of("results/memory_footprints.txt")))) {
            csv.println("structure,n,bytes,megabytes,objects");
            for (int n : Benchmark.SIZES) {
                int[] values = Benchmark.data(n);
                DynamicArray array = new DynamicArray();
                MyLinkedList list = new MyLinkedList();
                MinHeap heap = new MinHeap();
                for (int value : values) { array.add(value); list.add(value); heap.insert(value); }
                Object[] structures = {array, list, heap};
                String[] names = {"DynamicArray", "MyLinkedList", "MinHeap"};
                for (int i = 0; i < structures.length; i++) {
                    GraphLayout layout = GraphLayout.parseInstance(structures[i]);
                    csv.printf(Locale.ROOT, "%s,%d,%d,%.9f,%d%n", names[i], n,
                            layout.totalSize(), layout.totalSize() / 1_000_000.0, layout.totalCount());
                    details.println(names[i] + ", n=" + n);
                    details.println(layout.toFootprint());
                }
            }
        }
        System.out.println("Memory results written to results/memory.csv");
    }
}
