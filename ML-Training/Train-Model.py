"""
train_model.py
─────────────────────────────────────────────────────────
This is a ONE-TIME offline script. It is NOT part of the
running Java application. Think of it as the "model factory" —
a data scientist runs this once, produces model.onnx,
and hands it to the backend team (you, in Java).

WHAT THIS SCRIPT DOES:
1. Generates a realistic synthetic dataset of transactions
   (since we don't have real bank data, we simulate patterns
   that mirror real fraud: velocity spikes, odd hours,
   high-risk countries, amount anomalies)
2. Trains an XGBoost classifier to predict fraud probability
3. Exports the trained model to ONNX format
   (ONNX = Open Neural Network Exchange — a universal format
   that ANY language can run, including Java)

Run this with: python train_model.py
Output: model.onnx (copy this into your Java resources folder)
"""

import numpy as np
import pandas as pd
from xgboost import XGBClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, roc_auc_score
import onnxmltools
from onnxmltools.convert.common.data_types import FloatTensorType

# ─────────────────────────────────────────────────────────
# STEP 1: Generate synthetic dataset
# ─────────────────────────────────────────────────────────
np.random.seed(42)
N_SAMPLES = 20000

print("Generating synthetic transaction dataset...")

data = {
    # Amount normalized 0-1 (relative to a typical ₹50,000 max)
    "amount_normalized": np.random.beta(2, 8, N_SAMPLES),

    # How many transactions in last 10 minutes (0-30)
    "txn_count_10min": np.random.poisson(2, N_SAMPLES),

    # Ratio of current amount vs sender's average (1.0 = normal)
    "amount_vs_avg_ratio": np.random.gamma(2, 1.5, N_SAMPLES),

    # Is this a high-risk country? (0 or 1)
    "is_high_risk_country": np.random.binomial(1, 0.08, N_SAMPLES),

    # Is this transaction between 1am-5am? (0 or 1)
    "is_night_time": np.random.binomial(1, 0.15, N_SAMPLES),

    # Is the device unknown/new? (0 or 1)
    "is_unknown_device": np.random.binomial(1, 0.12, N_SAMPLES),

    # How many days since the sender's account was created
    "account_age_days": np.random.exponential(180, N_SAMPLES),

    # How many distinct countries this sender used in last 7 days
    "unique_countries_7days": np.random.poisson(1.2, N_SAMPLES),
}

df = pd.DataFrame(data)

# Clip values to realistic ranges
df["amount_normalized"] = df["amount_normalized"].clip(0, 1)
df["txn_count_10min"] = df["txn_count_10min"].clip(0, 30)
df["amount_vs_avg_ratio"] = df["amount_vs_avg_ratio"].clip(0, 20)
df["account_age_days"] = df["account_age_days"].clip(0, 2000)
df["unique_countries_7days"] = df["unique_countries_7days"].clip(0, 8)

# ─────────────────────────────────────────────────────────
# STEP 2: Create fraud labels based on realistic rules
# This simulates what REAL fraud patterns look like
# ─────────────────────────────────────────────────────────
fraud_score = (
    df["amount_normalized"] * 0.15 +
    (df["txn_count_10min"] > 10).astype(int) * 0.30 +
    (df["amount_vs_avg_ratio"] > 8).astype(int) * 0.25 +
    df["is_high_risk_country"] * 0.40 +
    df["is_night_time"] * 0.10 +
    df["is_unknown_device"] * 0.15 +
    (df["account_age_days"] < 7).astype(int) * 0.20 +
    (df["unique_countries_7days"] > 3).astype(int) * 0.25 +
    np.random.normal(0, 0.05, N_SAMPLES)  # noise
)

# Convert to binary label: fraud if score > 0.5
df["is_fraud"] = (fraud_score > 0.5).astype(int)

print(f"Dataset generated: {N_SAMPLES} samples")
print(f"Fraud rate: {df['is_fraud'].mean():.2%}")

# ─────────────────────────────────────────────────────────
# STEP 3: Train XGBoost model
# ─────────────────────────────────────────────────────────
FEATURE_COLUMNS = [
    "amount_normalized",
    "txn_count_10min",
    "amount_vs_avg_ratio",
    "is_high_risk_country",
    "is_night_time",
    "is_unknown_device",
    "account_age_days",
    "unique_countries_7days",
]

X = df[FEATURE_COLUMNS].to_numpy()  # plain numpy array - XGBoost will name features f0, f1, f2...
y = df["is_fraud"].to_numpy()

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

print("\nTraining XGBoost classifier...")
model = XGBClassifier(
    n_estimators=100,
    max_depth=5,
    learning_rate=0.1,
    eval_metric="logloss",
    random_state=42,
)
model.fit(X_train, y_train)

# ─────────────────────────────────────────────────────────
# STEP 4: Evaluate model
# ─────────────────────────────────────────────────────────
y_pred = model.predict(X_test)
y_pred_proba = model.predict_proba(X_test)[:, 1]

print("\n─── Model Performance ───")
print(classification_report(y_test, y_pred))
print(f"ROC-AUC Score: {roc_auc_score(y_test, y_pred_proba):.4f}")

# Feature importance — which features matter most
print("\n─── Feature Importance ───")
importance = sorted(
    zip(FEATURE_COLUMNS, model.feature_importances_),
    key=lambda x: x[1], reverse=True
)
for name, score in importance:
    print(f"{name}: {score:.4f}")

# ─────────────────────────────────────────────────────────
# STEP 5: Export to ONNX (the format Java will load)
# ─────────────────────────────────────────────────────────
print("\nExporting model to ONNX format...")

initial_type = [("input", FloatTensorType([None, len(FEATURE_COLUMNS)]))]
onnx_model = onnxmltools.convert_xgboost(model, initial_types=initial_type)

with open("model.onnx", "wb") as f:
    f.write(onnx_model.SerializeToString())

print("\n✅ DONE! model.onnx created.")
print("Copy this file to: src/main/resources/ml/model.onnx")