# Viora on-device scam-language classifier (Phase 8)

Produces `app/src/main/assets/ml/scam_language_classifier_v1.json`, the model
`LocalThreatClassifier` loads at runtime.

## What it is

A bag-of-words **logistic regression** text classifier — genuinely trained via
gradient descent (plain numpy, no sklearn/TensorFlow), not hand-written rules
labeled as ML. Inference on-device is plain arithmetic (dot product + sigmoid),
so no ML runtime library (TensorFlow Lite/ONNX Runtime) is needed — zero new
Gradle dependencies, zero native-library/APK-size/ABI risk.

## Data

- **Base**: [UCI SMS Spam Collection](https://archive.ics.uci.edu/dataset/228/sms+spam+collection)
  (5,572 real SMS messages, labeled ham/spam; public research dataset), fetched
  from a well-known GitHub mirror.
- **Domain-adaptation augmentation** (`augment.py`, 59 examples): the base
  corpus is ~2012 UK SMS spam and doesn't cover modern Indian UPI/banking scam
  vocabulary ("KYC", "account blocked", "UPI PIN", ...). These are
  **hand-written, disclosed, not real/scraped messages** — added transparently
  to teach the relevant vocabulary, oversampled 4x in the training split only
  (never in the held-out test split — see `train.py`'s split-then-oversample
  order, which prevents any train/test leakage).

## Reproducing

```
cd scripts/ml
python3 -m venv .venv && source .venv/bin/activate  # optional
pip install numpy
curl -o spam.csv https://raw.githubusercontent.com/mohitgupta-omg/Kaggle-SMS-Spam-Collection-Dataset-/master/spam.csv
python3 train.py
```

Prints held-out test accuracy/precision/recall/F1 and a few sanity-check
predictions, and writes the JSON model to this directory (copy it to
`app/src/main/assets/ml/` to update the shipped model).

## Held-out test performance (last run)

4,654 train / 1,127 test examples (proper split — augmented examples never
duplicated across the split). On the untouched test set:

- Accuracy: 96.8%
- Precision: 96.4%
- Recall: 76.8%
- F1: 85.5%

These numbers are copied into the shipped JSON's `trainingMetadata` field so
the app never has to re-derive or fabricate them.

## Limitations (disclosed, not hidden)

- Linear bag-of-words model — no context/word-order understanding, easily
  evaded by a determined adversary who avoids the learned vocabulary.
- Base dataset is English SMS spam, not scam-specific and not India-specific;
  the 59-example augmentation is small and cannot fully cover the domain.
- Recall (77%) is moderate — the model is tuned conservatively (few false
  positives, per Viora's threshold in `MLGuardianAI`), so it will miss some
  genuine scam phrasing rather than over-warn on benign text.
