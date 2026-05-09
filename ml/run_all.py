"""
run_all.py  —  Master pipeline script
Runs: generate_dataset → train_distilbert → train_indicbert → copy_to_assets
"""
import subprocess, sys, time, os

STEPS = [
    ("Generating dataset",          ["ml/generate_dataset.py"]),
    ("Training DistilBERT",         ["ml/train_distilbert.py"]),
    ("Training IndicBERT",          ["ml/train_indicbert.py"]),
    ("Copying files to assets",     ["ml/copy_to_assets.py"]),
]

def run_step(name, script_args):
    print(f"\n{'#'*60}")
    print(f"#  STEP: {name}")
    print(f"{'#'*60}")
    t0 = time.time()
    result = subprocess.run(
        [sys.executable] + script_args,
        capture_output=False
    )
    elapsed = time.time() - t0
    if result.returncode != 0:
        print(f"\n❌  Step '{name}' FAILED (exit {result.returncode})")
        sys.exit(result.returncode)
    print(f"\n✅  '{name}' completed in {elapsed:.0f}s")

if __name__ == "__main__":
    os.chdir(os.path.dirname(os.path.abspath(__file__)) + "/..")
    total_t0 = time.time()
    print("[START] RakshakX ML Pipeline -- Quick Demo Run (INT8 Quantized)")
    for name, args in STEPS:
        run_step(name, args)
    total = time.time() - total_t0
    print(f"\n{'='*60}")
    print(f"  Pipeline complete in {total/60:.1f} minutes")
    print(f"  Next: Build & run the Android app, filter Logcat by 'ScamClassifierRouter'")
    print(f"{'='*60}\n")
