# Branch `dyndevs-paper`: dynamic structure DEVS in xDEVS Java

Companion code of the paper *Dynamic structure DEVS in xDEVS: formal semantics, implementation and
performance evaluation* (B. Herguedas Pinedo, R. Cárdenas, P. Arroba, J. L. Risco-Martín). The branch
is based on commit `a1a182d` of `iscar-ucm/xdevs.java`; everything listed below is new or changed
with respect to it (32 files, about 2 300 lines). The experiment scripts, raw results and the paper
sources live in the separate repository `behergue/dyndevs-paper` (`experiments/`, `paper/`).

## What the paper describes and where it is in the code

| Paper | Code |
|---|---|
| Sec. 3, DS-coupled model: `structuralTransition()` returns whether the structure changed | `modeling/Coupled.java` — `structuralTransition()` (default: no change), `isDynamic()`, and `flatten()` now refuses dynamic models (flattening would drop the coupled models that own the structural transition functions). |
| Sec. 3, Algorithm 2 (structural phase after the transitions, bottom-up) and Sec. 4, Listing 1 | `simulation/dynamic/CoordinatorDynamic.java` — policies `eager` (evaluate δ_str every step) and `lazy` (only if a child transitioned, Lemma 1); reconciliation of simulators with a list scan or an `IdentityHashMap` (`present`); `exit()` on removed simulators; counters `structEvals`, `structChanges`, `added`, `removed`. |
| Sec. 5, factory case study (Table 3, Figs. 3–4) | `examples/dynamic/Factory.java`, `Machine.java`; `examples/dynamic/bench/FactoryStudy.java` (sweep and trace). |
| Sec. 6, benchmark models LI / HI / HO (DEVStone-like, no dependencies) and the dynamic DHO with churn | `examples/dynamic/bench/BenchLI.java`, `BenchHI.java`, `BenchHO.java`, `DynHO.java`, `BenchAtomic.java`, `BenchGenerator.java`, `BenchStone.java`. |
| Sec. 6, harness that runs one configuration and prints one CSV row (E1–E5, Tables 4–6, Figs. 5–6) | `examples/dynamic/bench/DynBench.java`. |
| Sec. 6.4, Table 7: reconciliation in isolation (list vs map) | `examples/dynamic/bench/ReconcileBench.java`. |
| Not in the paper yet: cyanobacteria case (DEVS-BLOOM-DS prototype) | `examples/dynamic/bloom/*` (`BloomScenario` entry point). |
| Not in the paper: offline verifier of DEVS invariants (I1–I9) with demo models that break them | `verification/*` (`demo/VerifyDemo` entry point). |

## Build

JDK 17 or newer. The `examples/devstone` package of upstream needs `commons-math3`, so it is excluded:

```bash
mkdir -p out
find src -name "*.java" -not -path "*/examples/devstone/*" > srcs.txt
javac -nowarn -d out @srcs.txt
```

## Entry points

All print CSV to stdout; run with `java -Xms2g -Xmx2g -XX:+UseSerialGC -cp out <class> <args>`.

| Class | Arguments | Used for |
|---|---|---|
| `xdevs.core.examples.dynamic.bench.DynBench` | `model width depth maxEvents work coordinator reps [churnPeriod churnSize [idleCopies]]`; `model` ∈ LI, HI, HO, DHO; `coordinator` ∈ `static`, `eager-list`, `eager-map`, `lazy-list`, `lazy-map` | E1 overhead on static models, E2 idle subsystems, E3 cost per structural change, E4 list vs map end to end, E5 with work in the transitions. Two warm-up runs + `reps` measured runs in one JVM; the row holds median/min/max. |
| `xdevs.core.examples.dynamic.bench.ReconcileBench` | `w s calls reps` — pool of `w` idle atomics, `s` removed/added per change, `calls` structural transitions timed | Table 7. |
| `xdevs.core.examples.dynamic.bench.FactoryStudy` | `sweep period processingTime observationTime` or `trace …` | Table 3 (sweep over m_max), Fig. 4 (trace). |
| `xdevs.core.examples.dynamic.bloom.BloomScenario` | `[hours] [fleetSize] [maxSimultaneousUsvs] [nutrients]` | DEVS-BLOOM-DS prototype. |
| `xdevs.core.verification.demo.VerifyDemo` | none | Verifier on Factory, HO, DHO and the three broken demo models. |

Examples:

```bash
java -cp out xdevs.core.examples.dynamic.bench.DynBench HO 20 10 100 0 lazy-map 11            # E1 row
java -cp out xdevs.core.examples.dynamic.bench.DynBench DHO 20 10 2000 0 lazy-map 11 10 5     # E3 row (churn every 10 events, 5 atomics)
java -cp out xdevs.core.examples.dynamic.bench.DynBench HO 20 10 100 0 lazy-map 11 0 0 50     # E2 row (50 idle copies)
java -cp out xdevs.core.examples.dynamic.bench.ReconcileBench 1000 2 200 11                   # Table 7 row
java -cp out xdevs.core.examples.dynamic.bench.FactoryStudy sweep 1 1 1000                    # Table 3
```

## Reproducing every table and figure

In `behergue/dyndevs-paper`, with this repository cloned (and built) as `xdevs.java/` next to `experiments/`:

| Script | Produces | Paper |
|---|---|---|
| `experiments/run_final.sh` | `results/final_e1.csv` … `final_e5.csv` (11 repetitions) | Fig. 5, Tables 4–6, Fig. 6 |
| `experiments/run_e4b.sh` | `results/e4b_reconcile.csv`, `e4b_dho.csv` | Table 7 |
| `experiments/run_adevs.sh` | `results/adevs_*.csv` (aDEVS 4.x port in `experiments/adevs/adevs_bench.cpp`) | Table 8 |
| `experiments/make_fig_data.py`, `make_adevs_table.py` | pgfplots data files and LaTeX rows | all figures |
| `experiments/cluster/brigit.sbatch`, `brigit_e4b.sbatch` | the same on a Slurm cluster; the numbers in the paper come from Brigit (UCM), jobs 80845 and 81995, OpenJDK 17, 2× Xeon Gold 5418Y | `results_brigit/` |

Correctness checks: for the static models the harness prints `theoryDeltExts` next to the measured
`deltExts` (they must be equal); for the dynamic models the counters must be identical across the five
coordinators, and `eager` must equal `lazy` (Lemma 1). The verifier (`VerifyDemo`) checks the same
invariants plus timing (ta ≥ 0, 0 ≤ e ≤ ta, monotone clock) and the stability assumption of Sec. 3.
