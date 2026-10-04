"""Create publication-ready figures directly from recorded CSV files."""
from pathlib import Path
import csv
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'results' / 'plots'
OUT.mkdir(parents=True, exist_ok=True)
plt.rcParams.update({'font.size': 9, 'axes.spines.top': False, 'axes.spines.right': False,
                     'figure.facecolor': 'white', 'axes.titleweight': 'bold'})
COLORS = {'DynamicArray': '#2166ac', 'MyLinkedList': '#c05b20', 'MinHeap': '#25866e',
          'Floyd': '#2166ac', 'RepeatedInsert': '#c05b20'}

def read(name):
    with (ROOT / 'results' / name).open(newline='') as f:
        return list(csv.DictReader(f))

def panel(ax, rows, metric, title):
    for series, structure in enumerate(dict.fromkeys(r['structure'] for r in rows)):
        selected = sorted((r for r in rows if r['structure'] == structure), key=lambda r: int(r['n']))
        ax.plot([int(r['n']) for r in selected], [float(r[metric]) for r in selected],
                'o-' if series == 0 else 'x--', color=COLORS[structure], label=structure, markersize=4, linewidth=1.7)
    ax.set_xscale('log')
    if all(float(r[metric]) > 0 for r in rows):
        ax.set_yscale('log')
    else:
        ax.set_yscale('symlog', linthresh=1)
        ax.set_ylim(bottom=0)
        if all(float(r[metric]) == 0 for r in rows):
            ax.set_ylim(0, 1)
            ax.set_yticks([0, 1])
    ax.set_xlabel('n (initial elements)')
    ax.set_ylabel('Time (ms)' if metric == 'time_ms' else 'Memory (MB, decimal)' if metric == 'megabytes' else metric.title() + ' (events)')
    ax.set_title(title, fontsize=10)
    ax.grid(True, alpha=.2, which='major')
    ax.legend(fontsize=7, loc='best')

def workload_figure(workload, rows):
    variants = ['head', 'middle'] if workload == 'W3' else ['-']
    if workload == 'W3':
        fig, axes = plt.subplots(2, 4, figsize=(13.2, 5.4), squeeze=False)
    else:
        fig, axes = plt.subplots(2, 2, figsize=(8, 4.6), squeeze=False)
    names = {'W1': 'Random access: 10,000 get calls', 'W2': 'Search: 500 hits + 500 misses',
             'W3': '1,000 insertions followed by 1,000 removals', 'W4': 'Priority processing: n inserts + n extracts'}
    for i, variant in enumerate(variants):
        group = [r for r in rows if r['workload'] == workload and r['variant'] == variant]
        for j, metric in enumerate(('time_ms', 'steps', 'moves', 'comparisons')):
            ax = axes[i, j] if workload == 'W3' else axes[j // 2, j % 2]
            panel(ax, group, metric, (variant + ': ' if variant != '-' else '') + ('Median time' if j == 0 else metric.title()))
    fig.suptitle(workload + ' | ' + names[workload], fontsize=13, fontweight='bold')
    fig.tight_layout(rect=(0, 0, 1, .92 if len(variants) == 1 else .95))
    fig.savefig(OUT / (workload + '.png'), dpi=220)
    plt.close(fig)

def main():
    rows = read('results.csv')
    for workload in ('W1', 'W2', 'W3', 'W4'):
        workload_figure(workload, rows)
    fig, ax = plt.subplots(figsize=(5.8, 3.0))
    panel(ax, read('memory.csv'), 'megabytes', 'Bonus A | Reachable memory, JOL')
    # Identical array/heap storage: show distinct markers instead of hiding one line.
    ax.lines[2].set_linestyle('--'); ax.lines[2].set_marker('x')
    fig.tight_layout(); fig.savefig(OUT / 'memory.png', dpi=220); plt.close(fig)
    fig, axes = plt.subplots(1, 2, figsize=(8, 3))
    rows = [r for r in read('build_heap.csv') if r['variant'] == 'descending']
    panel(axes[0], rows, 'time_ms', 'Bonus B | Descending input: time')
    panel(axes[1], rows, 'comparisons', 'Descending input: comparisons')
    fig.tight_layout(); fig.savefig(OUT / 'build_heap.png', dpi=220); plt.close(fig)
    fig, axes = plt.subplots(1, 2, figsize=(8, 3))
    rows = [r for r in read('build_heap.csv') if r['variant'] == 'random']
    panel(axes[0], rows, 'time_ms', 'Bonus B | Random input: time')
    panel(axes[1], rows, 'comparisons', 'Random input: comparisons')
    fig.tight_layout(); fig.savefig(OUT / 'build_heap_random.png', dpi=220); plt.close(fig)
    print('Created 7 figures in', OUT)

if __name__ == '__main__':
    main()
