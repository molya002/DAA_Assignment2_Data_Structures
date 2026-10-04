"""Build a fixed five-page PDF and an equivalent Markdown report from actual results."""
from pathlib import Path
import csv
from html import escape
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Paragraph, Table, TableStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from matplotlib import get_data_path

ROOT = Path(__file__).resolve().parents[1]
FONT = Path(get_data_path()) / 'fonts' / 'ttf'
pdfmetrics.registerFont(TTFont('DejaVu', str(FONT / 'DejaVuSans.ttf')))
pdfmetrics.registerFont(TTFont('DejaVu-Bold', str(FONT / 'DejaVuSans-Bold.ttf')))
pdfmetrics.registerFontFamily('DejaVu', normal='DejaVu', bold='DejaVu-Bold')
W, H = 595.28, 841.89
M = 39
WIDTH = W - 2 * M
NAVY = colors.HexColor('#17334b')
BLUE = colors.HexColor('#2166ac')

def load(name):
    with (ROOT / 'results' / name).open(newline='') as f:
        return list(csv.DictReader(f))

results = load('results.csv')
memory = load('memory.csv')
builds = load('build_heap.csv')
def result(w, s, variant='-'):
    return next(r for r in results if r['workload'] == w and r['structure'] == s and r['n'] == '100000' and r['variant'] == variant)

complexities = [
    ['DynamicArray.add(x)', 'Θ(1)', 'Θ(1)*', 'Θ(n)', 'Θ(n)', 'Full buffer copies n ints; doubling gives amortized Θ(1).'],
    ['DynamicArray.add(i,x)', 'Θ(1)', 'Θ(n)', 'Θ(n)', 'Θ(n)', 'Uniform i shifts n/2 values on average; growth may copy n.'],
    ['DynamicArray.remove(i)', 'Θ(1)', 'Θ(n)', 'Θ(n)', 'Θ(1)', 'Shifts n-i-1 values; the array does not shrink.'],
    ['DynamicArray.get(i)', 'Θ(1)', 'Θ(1)', 'Θ(1)', 'Θ(1)', 'One checked array read, independent of i.'],
    ['DynamicArray.contains(x)', 'Θ(1)', 'Θ(n)', 'Θ(n)', 'Θ(1)', 'First match stops; uniform hit or a miss examines Θ(n).'],
    ['MyLinkedList.add(x)', 'Θ(1)', 'Θ(1)', 'Θ(1)', 'Θ(1)', 'Tail pointer avoids traversal; allocate one node.'],
    ['MyLinkedList.add(i,x)', 'Θ(1)', 'Θ(n)', 'Θ(n)', 'Θ(1)', 'Head and tail cases are constant; other indices traverse.'],
    ['MyLinkedList.remove(i)', 'Θ(1)', 'Θ(n)', 'Θ(n)', 'Θ(1)', 'Head is constant; finding the predecessor costs Θ(i+1).'],
    ['MyLinkedList.get(i)', 'Θ(1)', 'Θ(n)', 'Θ(n)', 'Θ(1)', 'Exactly i next-link traversals plus constant work.'],
    ['MyLinkedList.contains(x)', 'Θ(1)', 'Θ(n)', 'Θ(n)', 'Θ(1)', 'Scan until match or null; uniform hit/miss model.'],
    ['MinHeap.insert(x)', 'Θ(1)', 'Θ(1)*', 'Θ(n)', 'Θ(n)', 'Swim ≤ log n levels; full backing array adds a linear copy.'],
    ['MinHeap.peekMin()', 'Θ(1)', 'Θ(1)', 'Θ(1)', 'Θ(1)', 'Read the root after checking non-emptiness.'],
    ['MinHeap.extractMin()', 'Θ(1)', 'Θ(log n)', 'Θ(log n)', 'Θ(1)', 'Sink at most the height; equal values can stop at the root.'],
    ['MinHeap.buildHeap(a)', 'Θ(n)', 'Θ(n)', 'Θ(n)', 'Θ(n)', 'Copy n values; bottom-up work sums to O(n).'],
]
notes = ('n is the current size. Auxiliary space is the peak extra allocation per call; output storage is included for buildHeap. '
         'Average indices are uniform; contains uses a fixed positive fraction of misses or uniformly located first hits. '
         'Heap averages assume random distinct priorities. *Append Θ(1) is amortized across a sequence; heap insertion Θ(1) is '
         'expected-amortized across random-permutation insertions [2], not the cost of a forced resize. Without resizing, the '
         'worst heap insertion is Θ(log n); with growth the amortized worst-case bound is O(log n). '
         'Stored space: list Θ(n); array/heap Θ(capacity), or Θ(peak n) because no shrinking occurs. '
         'size() and metrics() are Θ(1) in all cases; diagnostic snapshot() is Θ(n) time and output space.')

proof1 = [
    ('Invariant', 'Let A be the unchanged array contents and n the size at entry. Before iteration i of DynamicArray.contains(x), 0 ≤ i ≤ n and every A[j] with 0 ≤ j < i differs from x.'),
    ('Initialization', 'At i=0 the examined prefix is empty, so the statement holds vacuously.'),
    ('Maintenance', 'The loop compares A[i] with x. If equal, returning true is correct because a witness exists. Otherwise A[i] also differs from x; incrementing i extends the verified prefix by one and preserves the invariant.'),
    ('Termination', 'Each non-returning iteration decreases n-i. At normal exit i=n, the invariant covers every stored element, so returning false is correct. An early return has already supplied a matching element.'),
    ('Conclusion', 'Every exit reports membership correctly, and the finite decreasing variant ensures termination. The array and size are unchanged.'),
]
proof2 = [
    ('Invariant', 'Let A be the original contents, n the original size and k the validated removal index. Before shift iteration i, k ≤ i ≤ n-1; entries j<k equal A[j], entries k ≤ j<i equal A[j+1], and entries i ≤ j<n still equal A[j]. The saved removed value equals A[k].'),
    ('Initialization', 'At i=k no entry has been shifted, the shifted interval is empty, and the method has saved A[k]. All other entries still match A.'),
    ('Maintenance', 'For i<n-1, cell i+1 still contains A[i+1]. Assigning it to cell i gives the correct replacement; incrementing i extends the shifted interval. The prefix before k and the unprocessed suffix remain unchanged.'),
    ('Termination', 'The integer n-1-i decreases each iteration. At exit i=n-1, the prefix [0,k) is unchanged and [k,n-1) contains A[k+1..n). Decrementing size discards the final redundant slot.'),
    ('Conclusion', 'The result is precisely the original sequence without index k, in the original order, and the returned value is A[k]. Bounds checking also makes invalid input fail before mutation.'),
]
protocol = ('Maven/JUnit 5: 11 tests pass, including 48,000 randomized sequence operations and 20,000 mixed heap operations, '
            'plus edge cases, sorted extraction, growth and exact small counter checks. Heap order is checked after every tested insert/extract. '
            'Benchmark: Random(42), n=100/1,000/10,000/100,000, fresh states, 30 global warm-up rounds, then 3 discarded + 5 measured trials per case. '
            'Median nanoTime is exported with deterministic counters; 36 medians and 180 raw trials are retained. W1: 10,000 gets; W2: 500 hits + 500 guaranteed negative misses; '
            'W3: 1,000 inserts followed by 1,000 removals at fixed 0 or original n/2. W1-W3 exclude filling; W4 includes n inserts and n extracts. '
            'Random generation and validation are outside the timed region; returned results are checked and consumed. Environment: Java 25.0.1, Windows 11 amd64.')

def fmt(w, s, v='-'):
    return f"{float(result(w,s,v)['time_ms']):.3f}"

discussion = [
    f"At n=100,000, W1 took {fmt('W1','DynamicArray')} ms for DynamicArray and {fmt('W1','MyLinkedList')} ms for MyLinkedList.",
    'The array reads one indexed cell, whereas the list follows approximately i links to reach index i.',
    'Sequential array scans also benefit from contiguous int storage, so a fetched cache line can serve several neighboring values.',
    'List traversal depends on the previous node address and can stall on pointer chasing even for a linear-time algorithm.',
    f"W2 took {fmt('W2','DynamicArray')} versus {fmt('W2','MyLinkedList')} ms despite similar linear comparison counts.",
    'Node object headers, alignment and references enlarge the working set, while allocation and garbage collection can add variability.',
    'The benchmark supports this locality explanation but does not directly measure cache misses or isolate GC costs.',
    f"For W3 at the head, the list took {fmt('W3','MyLinkedList','head')} ms versus {fmt('W3','DynamicArray','head')} ms because it rewires links without shifting a suffix.",
    f"At the middle, the list took {fmt('W3','MyLinkedList','middle')} ms versus {fmt('W3','DynamicArray','middle')} ms because index lookup must traverse the prefix.",
    'The head workload has constant list work per operation; array work also depends on the temporary 1,000-element increase.',
    'The list is useful for head updates and tail appends, but removing its tail still requires finding the predecessor.',
    f"MinHeap completed W4 in {fmt('W4','MinHeap')} ms and suits repeated minimum-priority extraction rather than arbitrary indexed lookup.",
    'Instrumented single-JVM timings include counter overhead and OS noise, so small-size reversals are not asymptotic evidence.',
    'Floyd construction guarantees linear work by summing node heights, while descending-input repeated insertion requires Θ(n log n) comparisons.',
    'Random insertion can have linear expected total work, so the bonus includes descending input as well as random data rather than claiming a logarithmic gap for every input.',
]

c = canvas.Canvas(str(ROOT / 'REPORT.pdf'), pagesize=(W,H))
c.setTitle('DAA Assignment 2 - Yerzhan Moldir - SE-2509')
c.setAuthor('Yerzhan Moldir; prepared with AI assistance')
y = 0
def page(number, title, subtitle):
    global y
    if number > 1: c.showPage()
    c.setFillColor(NAVY); c.rect(0,H-94,W,94,fill=1,stroke=0)
    c.setFillColor(colors.white); c.setFont('DejaVu-Bold',17); c.drawString(M,H-39,title)
    c.setFont('DejaVu',8.8); c.drawString(M,H-61,subtitle)
    c.setFillColor(colors.HexColor('#667788')); c.setFont('DejaVu',8)
    c.drawString(M,23,'Yerzhan Moldir | SE-2509 | DAA Assignment 2 | 04 October 2026')
    c.drawRightString(W-M,23,f'{number} / 5')
    y=H-111

def para(text, size=9, gap=6, bold=False):
    global y
    style=ParagraphStyle('body',fontName='DejaVu-Bold' if bold else 'DejaVu',fontSize=size,leading=size*1.4,textColor=NAVY)
    p=Paragraph(text,style); _,h=p.wrap(WIDTH,1000)
    if y-h<40: raise RuntimeError(f'Page overflow: {text[:70]} at {y-h}')
    p.drawOn(c,M,y-h); y-=h+gap

def image(name, height=None):
    global y
    from PIL import Image
    path=ROOT/'results'/'plots'/name
    iw,ih=Image.open(path).size
    height=height or WIDTH*ih/iw
    if y-height<40: raise RuntimeError('Image overflow: '+name)
    c.drawImage(str(path),M,y-height,width=WIDTH,height=height,preserveAspectRatio=True,anchor='c')
    y-=height+9

page(1,'Data structures: design and complexity','Primitive int storage | DynamicArray, MyLinkedList and MinHeap')
para('Three structures are implemented from scratch with internal physical-event counters. The list is singly linked with a tail; both array-backed structures double capacity. No production structure uses java.util collections. All bounds below describe this implementation, including resizing.',9)
headers=['Operation','Best','Average','Worst','Extra','Justification']
style=ParagraphStyle('cell',fontName='DejaVu',fontSize=7.1,leading=9.5,textColor=NAVY)
data=[[Paragraph(escape(s),style) for s in row] for row in [headers]+complexities]
table=Table(data,colWidths=[121,43,49,49,43,WIDTH-305])
table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#dceaf4')),('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#f2f6f9')]),('VALIGN',(0,0),(-1,-1),'TOP'),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5),('LEFTPADDING',(0,0),(-1,-1),4),('RIGHTPADDING',(0,0),(-1,-1),4)]))
_,th=table.wrap(WIDTH,1000); table.drawOn(c,M,y-th); y-=th+10
para(notes,8.1)
para('Θ means a tight upper and lower bound. Amortized cost distributes occasional resizing across a sequence; it is distinct from an average over random inputs.',8.1)

page(2,'Correctness and measurement protocol','Two loop-invariant proofs tied to the submitted methods')
for title,proof in [('Proof 1: DynamicArray.contains(x)',proof1),('Proof 2: DynamicArray.remove(k)',proof2)]:
    para(title,10.2,bold=True)
    for label,text in proof: para('<b>'+label+'.</b> '+escape(text),8.7,gap=4)
para('Validation and reproducibility',10.2,bold=True)
para(protocol,8.4)
para('Counting: steps = int-array cell read or next-link traversal; moves = existing-element relocation or list-field pointer write; comparisons = element-value comparison. A swap is two moves; growth/input copies count. Loop/index checks and new-int stores are excluded. Full conventions are in README.md.',8.4)

page(3,'Indexed access and linear search','Figures 1-2 | Five-trial medians; event counts are measured inside methods')
image('W1.png',228)
para('Figure 1. W1: fixed 10,000 queries. Array steps remain exactly 10,000, whereas list steps grow with n. Both move and element-comparison counts are zero. Positive axes use log scales; zero-event panels retain a visible zero.',9)
image('W2.png',228)
para('Figure 2. W2: 1,000 queries, exactly half successful. Both implementations use the same values and query order, so element-comparison counts match. The array counts cell reads; the list counts traversals, with no traversal needed after a successful comparison.',9)
para('W1 measures different amounts of work; W2 compares similar linear work through different memory layouts. Actual short timings need not be monotone. Data: results/results.csv and raw_runs.csv; machine settings: environment.txt.',8.5)

page(4,'Boundary updates and priority processing','Figures 3-4 | Both W3 variants; W4 validates non-decreasing extraction')
image('W3.png',234)
para('Figure 3. W3 keeps the insertion/removal index fixed at 0 or original n/2. Each trial inserts all 1,000 values before removing them. Array shifts reflect temporary growth; a list reports pointer updates rather than zero moves. Middle-list steps include locating each predecessor.',8.5)
image('W4.png',204)
para('Figure 4. W4 includes both filling the heap and extracting all n values. The output is checked after timing. The dominant worst-case total is Θ(n log n); array growth adds only O(n) total copying across insertions.',8.5)
para('Why the contracts matter',10,bold=True)
para('List head updates require constant work, but indexed middle updates include traversal. A tail pointer makes append constant without making tail removal constant. MinHeap maintains parent ≤ child; peekMin reads the root in constant time, while extractMin replaces the root and repairs one downward path.',8.7)

page(5,'Memory, heap construction and discussion','Bonus A + Bonus B | Measured evidence and practical trade-offs')
global_y=y
# Put the two bonus panels side by side without distorting either image.
c.drawImage(str(ROOT/'results/plots/memory.png'),M,y-125,width=235,height=125,preserveAspectRatio=True,anchor='c')
c.drawImage(str(ROOT/'results/plots/build_heap.png'),M+242,y-125,width=275,height=125,preserveAspectRatio=True,anchor='c')
y-=135
para('Figure 5. JOL graph sizes include the structure, storage and Metrics. At n=100,000: array/heap 524,368 B each; list 2,400,072 B (4.58×). A node is 24 B: 12 B header, 4 B int, 4 B next and 4 B alignment. Array capacity is 131,072. VM layout uses 4 B references and 8 B alignment; attach-enabled JOL records are included.',8)
para('Figure 6. At n=100,000 descending input: Floyd uses 199,978 comparisons versus 1,468,946 for repeated insert. The sum of node heights is O(n): n·Σ(h/2^(h+1))=O(n), and copying n inputs supplies Ω(n). Random-input results and a separate plot are also included; smaller timing constants can favor insertion.',8)
para('Discussion (15 sentences)',9.8,bold=True)
para(' '.join(discussion),8.15)
para('Sources: [1] Sedgewick & Wayne, Priority Queues, algs4.cs.princeton.edu/24pq/ (heap order and linear bottom-up construction). [2] Bollobás & Simon, Repeated random insertion into a priority queue (1985), digitalcommons.memphis.edu/facpubs/5607/ (expected constant exchanges). [3] Assignment 2 specification; [4] included JOL 0.17 measurements and raw benchmark CSVs. Detailed clickable references are in REPORT.md.',7.2)
c.save()

md=['# DAA Assignment 2 - Data Structures','', '**Yerzhan Moldir | SE-2509 | 04 October 2026**', '',
    'Five-page submission: REPORT.pdf. This Markdown version provides the same analysis and full-size figures.', '',
    '## Complexity', '', '| '+' | '.join(headers)+' |','|'+ '|'.join(['---']*6)+'|']
md += ['| '+' | '.join(row)+' |' for row in complexities]
md += ['',notes,'','## Loop-invariant proofs','']
for title,proof in [('DynamicArray.contains(x)',proof1),('DynamicArray.remove(k)',proof2)]:
    md += ['### '+title,'']+[f'**{label}:** {text}\n' for label,text in proof]
md += ['## Methodology and validation','',protocol,'','See README.md for exact counter conventions, timed boundaries, reproducible commands and limitations.','']
for w in ['W1','W2','W3','W4']:
    md += [f'## {w}', '', f'![{w} time and operation counts](results/plots/{w}.png)','']
md += ['## Bonus A: memory','', '![JOL memory](results/plots/memory.png)','',
       'At n=100,000 the array and heap occupy 524,368 bytes each, versus 2,400,072 bytes for the list. A 24-byte node holds a 12-byte header, int (4), reference (4) and alignment padding (4). The array capacity is 131,072. JOL includes the constant-size Metrics object. Attach-enabled layout details and object footprints are saved; these figures are JVM-specific, not universal.', '',
       '## Bonus B: Floyd buildHeap','', '![Descending build](results/plots/build_heap.png)', '', '![Random build](results/plots/build_heap_random.png)', '',
       'Floyd repairs parents from n/2-1 down to 0, after copying the input. At height h there are O(n/2^(h+1)) nodes, so total repair work is O(n) and copying gives a matching lower bound. Repeated insertion is Θ(n log n) on descending priorities but expected Θ(n) total on a random permutation [2]. Both orders are measured, including allocation and input copying. At n=100,000 descending input, comparison counts are 199,978 versus 1,468,946.', '',
       '## Discussion (15 sentences)','', ' '.join(discussion), '', '## Sources','',
       '1. [Sedgewick and Wayne: Priority Queues](https://algs4.cs.princeton.edu/24pq/).',
       '2. [Bollobás and Simon: Repeated random insertion into a priority queue (1985)](https://digitalcommons.memphis.edu/facpubs/5607/).',
       '3. DAA_Assignment2_Data_Structures.pdf, supplied assignment specification.',
       '4. JOL 0.17 measurements: results/memory.csv, jol_environment.txt and memory_footprints.txt; benchmark evidence: results/results.csv and raw_runs.csv.', '']
(ROOT/'REPORT.md').write_text('\n'.join(md),encoding='utf-8')
print('Created REPORT.pdf (5 pages) and REPORT.md')
