# DAA Assignment 2 - Data Structures

**Student:** Yerzhan Moldir  
**Group:** SE-2509  
**Repository:** https://github.com/molya002/DAA_Assignment2_Data_Structures  
**Release:** main, reviewed revision `v1.1`; original required tag `v1.0` is retained

Java 17-compatible Maven project implementing primitive-int DynamicArray, a singly linked MyLinkedList with a tail, and an array-based MinHeap. Both optional tasks are included: JOL memory footprints and Floyd buildHeap.

## Build and run

Requires JDK 17+ and Maven 3.9+. The recorded run used Java 25 on Windows 11. Run commands from the project root.

```sh
mvn clean verify
mvn -q compile exec:java
```

The first command builds the project and runs all JUnit 5 tests. The second command runs the complete required benchmark and bonus heap-build comparisons in one invocation, writing `results/results.csv`, `raw_runs.csv`, `build_heap.csv`, `build_heap_raw.csv` and `environment.txt`. It takes approximately a minute, depending on the machine.

Memory benchmark (PowerShell, with JOL self-attachment enabled for reliable object sizes):

```powershell
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading'
mvn -q compile exec:java '-Dexec.mainClass=edu.aitu.daa.MemoryBenchmark'
```

On JDK 17 omit `-XX:+EnableDynamicAgentLoading` if the VM does not accept it. POSIX alternative:

```sh
MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true' mvn -q compile exec:java -Dexec.mainClass=edu.aitu.daa.MemoryBenchmark
```

JOL 0.17 may emit Unsafe deprecation warnings on recent JDKs. `jol_environment.txt` records the VM layout; `memory_footprints.txt` records each graph. If dynamic attachment is blocked, the default JOL fallback may estimate object sizes; enable attachment or use a permitted JDK before treating those figures as measured sizes.

Generate plots and the five-page report from the saved CSVs:

```sh
python -m pip install -r requirements.txt
python scripts/plot_results.py
python scripts/build_report.py
python scripts/validate_results.py
```

## Project layout

```text
src/main/java/edu/aitu/daa/   structures, interface, metrics and benchmarks
src/test/java/edu/aitu/daa/   JUnit 5 tests and randomized reference checks
results/results.csv         36 required case medians
results/raw_runs.csv        180 measured trials (warm-ups excluded)
results/plots/              four workload figures and three bonus figures
results/build_heap*.csv     random and descending build comparisons
results/memory.csv          JOL reachable sizes for all structures and sizes
scripts/                    plot, report and CSV validation scripts
REPORT.md / REPORT.pdf      analysis, proofs, graphs and discussion
DEFENSE_RU.md               Russian study notes for the oral defense
```

## API and invariants

`IntSequence` exposes `add(value)`, `add(index,value)`, `remove(index)` (returns the removed int), `get(index)`, `contains(value)`, `size()` and `metrics()`. Insertion indices are inclusive `0..size`; read/removal indices are `0..size-1`. Invalid indices throw `IndexOutOfBoundsException` before changing the structure.

MinHeap exposes `insert`, `peekMin`, `extractMin`, `size`, `metrics`, and static `buildHeap(int[])`. Empty reads/removals throw `IllegalStateException`. `buildHeap` copies its input, including empty arrays, and rejects null. `snapshot()` is a defensive diagnostic copy used outside timed operations.

Array and heap capacity starts at 8 and doubles when full. They never shrink, so storage is O(peak size), even after removals. The list keeps head and tail consistent when the last node is removed. Production data structures do not import java.util collections; only tests use ArrayList and PriorityQueue as oracles. Benchmark uses java.util.Random and Locale, which are not collections.

## Exact counting convention

- `steps`: one explicit read of an int[] cell (including growth copies, heap comparisons and swaps), or one traversal through a node's `next` link. A traversal returning null still counts. Fetching `head`, `tail` or a node's int value is not a next-link traversal.
- `moves`: relocation of an existing array element (shift, growth copy, heap swap or root replacement); one heap swap counts as two moves. Floyd's input copy counts one read and one move per element. Storing a newly supplied int is not a shift and is not counted as a move.
- List `moves`: every explicit write to `head`, `tail` or a node's `next` field, including null updates. Local traversal-variable assignments and implicit null initialization are excluded.
- `comparisons`: comparisons between element values only, including equality in contains. Index checks, loop bounds, reference equality and benchmark validation are excluded.
- Counters are longs, updated inside methods; reads of metrics, resets, diagnostic snapshots and JOL inspection are outside timing and do not increment algorithm counters.

Different physical event categories must not be interpreted as identical CPU costs. For example, get(0) on a list has zero next-link steps but still performs constant work.

## Measurement protocol

All cases use `new Random(42)` and fresh structures. A 30-round global warm-up at n=1000 precedes three discarded warm-ups per case and five recorded trials. The CSV stores the median time and the matching counters; counters are asserted identical across all five trials. No timings are manually edited. There are no forced garbage collections.

W1-W3 exclude initial fill, query generation and correctness checks. W1 has 10,000 uniformly random valid indices. W2 alternates 500 known-present nonnegative values and 500 guaranteed-absent negative values. W3 performs all 1,000 insertions before all 1,000 removals, at index 0 or the fixed original n/2; it includes any capacity growth caused by those insertions. W4 times both n insertions and n extractions; sorted-order and checksum validation occur afterward. Bonus build timings include allocation and input copying. Returned values feed a checked checksum and a volatile sink.

These are instrumented, single-JVM educational benchmarks, not JMH microbenchmarks. Counters add overhead, short measurements are noisy, JIT compilation and OS scheduling may persist after warm-up, and cases are run in a fixed order. A small-n reversal is not proof of a different asymptotic bound. CPU cache behavior is an explanation consistent with the results, not a hardware-counter measurement. W2 tests linear search, not a separate iteration workload.

## Git and submission

Development branches: `feature/array`, `feature/list`, `feature/heap`, `feature/metrics`; each was merged after tests or measurement. Local commits identify the assisting tool rather than claiming manual authorship. `v1.0` marks the initial completed release; `v1.1` records the review corrections on main without rewriting the published original tag. An included Git bundle preserves all branches and history without shipping credentials or local Git configuration.

Review revision: 17 passing JUnit tests, including six additional hand-calculated metric/ownership/error-path checks. The CSV audit also checks exact array W3 shift/growth totals and the 500-hit difference between array reads and list traversals in W2. The report distinguishes conditional average costs from amortized costs. Production algorithms and recorded timing samples are unchanged. See REVIEW.txt for the criterion-by-criterion review.

Upload `DAA_Assignment2_Yerzhan_Moldir_SE-2509.zip` to Moodle and include the repository link above. Defense is in Week 5. The assignment permits AI only for debugging and explanation; this project was generated with substantial AI assistance and must not be represented as independent work. Confirm acceptability with the instructor and understand every submitted line.
