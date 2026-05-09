"""
train_distilbert.py
Fine-tunes distilbert-base-uncased for 3-class scam detection.
Exports to ONNX with INT8 dynamic quantization.
"""
import os, shutil, time, warnings
warnings.filterwarnings("ignore")
import pandas as pd
import numpy as np
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader
from transformers import DistilBertTokenizerFast, DistilBertForSequenceClassification
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report

MODEL_NAME = "distilbert-base-uncased"
DATA_PATH  = "ml/data/distilbert_data.csv"
OUT_DIR    = "ml/output/distilbert"
MAX_LEN    = 128
BATCH_SIZE = 16
EPOCHS     = 2
LR         = 2e-5
LABELS     = ["SAFE", "SCAM", "SUSPICIOUS"]
DEVICE     = torch.device("cuda" if torch.cuda.is_available() else "cpu")


class ScamDataset(Dataset):
    def __init__(self, texts, labels, tokenizer):
        self.encodings = tokenizer(texts, truncation=True, padding="max_length",
                                   max_length=MAX_LEN, return_tensors="pt")
        self.labels = torch.tensor(labels, dtype=torch.long)
    def __len__(self):
        return len(self.labels)
    def __getitem__(self, idx):
        return {
            "input_ids":      self.encodings["input_ids"][idx],
            "attention_mask": self.encodings["attention_mask"][idx],
            "labels":         self.labels[idx],
        }




def train():
    os.makedirs(OUT_DIR, exist_ok=True)
    print(f"\n{'='*60}")
    print(f"  DistilBERT Training  |  device={DEVICE}")
    print(f"{'='*60}")

    df = pd.read_csv(DATA_PATH)
    texts, labels = df["text"].tolist(), df["label"].tolist()
    print(f"[1/6] Dataset: {len(df)} samples")

    tokenizer = DistilBertTokenizerFast.from_pretrained(MODEL_NAME)
    print(f"[2/6] Tokenizer loaded")

    tr_t, va_t, tr_l, va_l = train_test_split(
        texts, labels, test_size=0.15, stratify=labels, random_state=42)
    tr_dl = DataLoader(ScamDataset(tr_t, tr_l, tokenizer), batch_size=BATCH_SIZE, shuffle=True)
    va_dl = DataLoader(ScamDataset(va_t, va_l, tokenizer), batch_size=BATCH_SIZE)
    print(f"[3/6] Split: train={len(tr_t)}, val={len(va_t)}")

    # Force eager attention so TorchScript tracing works
    model = DistilBertForSequenceClassification.from_pretrained(
        MODEL_NAME, num_labels=3, attn_implementation="eager"
    ).to(DEVICE)
    optimizer = torch.optim.AdamW(model.parameters(), lr=LR)

    print(f"[4/6] Fine-tuning {EPOCHS} epochs...")
    for epoch in range(EPOCHS):
        model.train()
        total_loss = correct = total = 0
        t0 = time.time()
        for batch in tr_dl:
            ids  = batch["input_ids"].to(DEVICE)
            mask = batch["attention_mask"].to(DEVICE)
            lbl  = batch["labels"].to(DEVICE)
            optimizer.zero_grad()
            out = model(input_ids=ids, attention_mask=mask, labels=lbl)
            out.loss.backward(); optimizer.step()
            total_loss += out.loss.item()
            correct += (out.logits.argmax(-1) == lbl).sum().item()
            total += lbl.size(0)
        print(f"  Epoch {epoch+1}/{EPOCHS}  loss={total_loss/len(tr_dl):.4f}  "
              f"acc={correct/total*100:.1f}%  ({time.time()-t0:.0f}s)")

    model.eval()
    preds, trues = [], []
    with torch.no_grad():
        for b in va_dl:
            o = model(input_ids=b["input_ids"].to(DEVICE),
                      attention_mask=b["attention_mask"].to(DEVICE))
            preds.extend(o.logits.argmax(-1).cpu().tolist())
            trues.extend(b["labels"].tolist())
    print("\n  Validation:")
    print(classification_report(trues, preds, target_names=LABELS))

    # ── ONNX Export ───────────────────────────────────────────────────────────
    print("[5/6] ONNX export...")
    model.eval().cpu()
    raw_path   = os.path.join(OUT_DIR, "model_raw.onnx")
    final_path = os.path.join(OUT_DIR, "model.onnx")

    dummy_input = tokenizer(
        "test message",
        return_tensors="pt",
        max_length=MAX_LEN,
        padding="max_length",
        truncation=True
    )

    with torch.no_grad():
        torch.onnx.export(
            model,
            (dummy_input["input_ids"], dummy_input["attention_mask"]),
            raw_path,
            input_names=["input_ids", "attention_mask"],
            output_names=["logits"],
            dynamic_axes={"input_ids": {0: "batch_size"}, "attention_mask": {0: "batch_size"},
                          "logits": {0: "batch_size"}},
            opset_version=13,
            do_constant_folding=True,
            dynamo=False,
        )
    size_mb = os.path.getsize(raw_path) / 1e6
    print(f"  Raw ONNX: {size_mb:.1f} MB")

    # INT8 quantization with fallback
    try:
        from onnxruntime.quantization import quantize_dynamic, QuantType
        quantize_dynamic(raw_path, final_path, weight_type=QuantType.QInt8)
        os.remove(raw_path)
        print(f"  Quantized (INT8): {os.path.getsize(final_path)/1e6:.1f} MB")
    except Exception as e:
        print(f"  Quantization skipped ({e}), using fp32")
        shutil.move(raw_path, final_path)

    # vocab.txt
    tmp = os.path.join(OUT_DIR, "_tok")
    tokenizer.save_pretrained(tmp)
    shutil.copy(os.path.join(tmp, "vocab.txt"), os.path.join(OUT_DIR, "vocab.txt"))
    shutil.rmtree(tmp)
    print(f"  vocab.txt saved")

    print("[6/6] Done!")
    _verify(final_path, tokenizer)


def _verify(path, tokenizer):
    import onnxruntime as ort
    print("\n  [Verify] ONNX sanity check...")
    sess = ort.InferenceSession(path)
    print(f"  Inputs:  {[i.name for i in sess.get_inputs()]}")
    print(f"  Outputs: {[o.name for o in sess.get_outputs()]}")
    for text, exp in [("Your OTP is 123456", "SAFE"),
                      ("You won lottery! Call NOW!", "SCAM"),
                      ("Send money urgently please", "SUSPICIOUS")]:
        enc = tokenizer(text, return_tensors="np", truncation=True,
                        padding="max_length", max_length=MAX_LEN)
        logits = sess.run(["logits"], {
            "input_ids": enc["input_ids"].astype(np.int64),
            "attention_mask": enc["attention_mask"].astype(np.int64),
        })[0][0]
        pred = LABELS[int(np.argmax(logits))]
        e = np.exp(logits - logits.max())
        conf = float(e[np.argmax(logits)] / e.sum() * 100)
        ok = "[OK]" if pred == exp else "[??]"
        print(f"    {ok} '{text}' -> {pred} ({conf:.0f}%)")
    print()


if __name__ == "__main__":
    train()
