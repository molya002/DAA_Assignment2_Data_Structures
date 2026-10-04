"""Audit coverage, exact counters, recorded medians and bonus evidence."""
import csv
import statistics
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def read(name):
    with (ROOT/'results'/name).open(newline='') as f:
        return list(csv.DictReader(f))
def key(row): return tuple(row[k] for k in ('workload','variant','structure','n'))

def audit(summary, raw, count):
    medians=read(summary); samples=read(raw)
    assert len(medians)==count and len(samples)==5*count
    assert len({key(r) for r in medians})==count
    for row in medians:
        group=[r for r in samples if key(r)==key(row)]
        assert len(group)==5 and {r['run'] for r in group}=={'1','2','3','4','5'}
        assert abs(float(row['time_ms'])-statistics.median(float(r['time_ms']) for r in group))<1e-6
        assert float(row['time_ms'])>0
        for metric in ('steps','moves','comparisons'):
            assert all(r[metric]==row[metric] for r in group)
            assert int(row[metric])>=0
    return medians

rows=audit('results.csv','raw_runs.csv',36)
expected=set()
for n in (100,1000,10000,100000):
    for w in ('W1','W2','W3'):
        for v in (('head','middle') if w=='W3' else ('-',)):
            for s in ('DynamicArray','MyLinkedList'): expected.add((w,v,s,str(n)))
    expected.add(('W4','-','MinHeap',str(n)))
assert {key(r) for r in rows}==expected
for r in rows:
    if r['workload']=='W1':
        assert r['moves']=='0' and r['comparisons']=='0'
        if r['structure']=='DynamicArray': assert r['steps']=='10000'
    if r['workload']=='W2':
        other=next(q for q in rows if q['workload']=='W2' and q['n']==r['n'] and q['structure']!=r['structure'])
        assert other['comparisons']==r['comparisons']
        assert int(r['moves']) == 0
        # Each successful list search stops before following its final next link.
        assert int(r['steps']) == int(r['comparisons']) - (500 if r['structure']=='MyLinkedList' else 0)
    if r['workload']=='W3':
        assert int(r['comparisons']) == 0
        if r['structure']=='DynamicArray':
            n=int(r['n']); k=1000
            index=0 if r['variant']=='head' else n//2
            capacity=8
            while capacity<n: capacity*=2
            copies=0
            while capacity<n+k:
                copies+=capacity; capacity*=2
            shifts=2*k*(n-index)+k*(k-1)
            assert int(r['moves'])==shifts+copies
            assert int(r['steps'])==shifts+copies+k
    if r['workload']=='W3' and r['structure']=='MyLinkedList':
        assert int(r['moves'])==3000
        assert int(r['steps'])==(1000 if r['variant']=='head' else 1000*int(r['n'])+1000)
bonus=audit('build_heap.csv','build_heap_raw.csv',16)
assert {key(r) for r in bonus} == {('BUILD',v,s,str(n)) for n in (100,1000,10000,100000)
    for v in ('random','descending') for s in ('Floyd','RepeatedInsert')}
mem=read('memory.csv')
assert len(mem)==12
assert len({(r['structure'],r['n']) for r in mem})==12
assert all(int(r['bytes'])>0 for r in mem)
for name in ('W1','W2','W3','W4','memory','build_heap','build_heap_random'):
    assert (ROOT/'results/plots'/f'{name}.png').is_file()
print('PASS: 36 cases, 180 raw trials, 16 bonus cases, 12 memory rows, 7 plots; counters and medians verified.')
