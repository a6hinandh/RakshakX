"""
copy_to_assets.py
Copies trained ONNX models and vocab files into the Android asset directories.
"""
import os, shutil

ASSET_BASE = os.path.join(
    os.path.dirname(__file__), "..",
    "app", "src", "main", "assets", "rakshakx_model"
)

COPIES = [
    ("ml/output/distilbert/model.onnx",  "distilbert/model.onnx"),
    ("ml/output/distilbert/vocab.txt",   "distilbert/vocab.txt"),
    ("ml/output/indicbert/model.onnx",   "indicbert/model.onnx"),
    ("ml/output/indicbert/vocab.txt",    "indicbert/vocab.txt"),
]

def main():
    print("\n" + "="*60)
    print("  Copying model files -> Android assets")
    print("="*60)

    all_ok = True
    for src, rel_dst in COPIES:
        dst = os.path.normpath(os.path.join(ASSET_BASE, rel_dst))
        os.makedirs(os.path.dirname(dst), exist_ok=True)

        if not os.path.exists(src):
            print(f"  [MISSING] {src}  — run training first!")
            all_ok = False
            continue

        shutil.copy2(src, dst)
        size_mb = os.path.getsize(dst) / 1e6
        print(f"  [OK] {src} -> {dst}  ({size_mb:.1f} MB)")

    print()
    if all_ok:
        print("  [DONE] All files copied. Build the app and check Logcat:")
        print("     filter: ScamClassifierRouter")
        print("     expect: 'DistilBERT initialized successfully from rakshakx_model/distilbert/model.onnx'")
        print("     expect: 'IndicBERT initialized successfully from rakshakx_model/indicbert/model.onnx'")
    else:
        print("  [WARN] Some files are missing. Run run_all.py to train first.")
    print()

if __name__ == "__main__":
    main()
