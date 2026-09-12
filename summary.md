Listed directory IQOO%20Hackathon
Listed directory YOLOV8
Listed directory smartlease-edge-chennai-master
Viewed README.md:1-250
Viewed README.md:1-105
Listed directory main
Listed directory assets
Listed directory ml
Listed directory java
Listed directory com
Listed directory smartlease
Listed directory edge
Listed directory iqoo
Listed directory multimodal
Viewed AndroidManifest.xml:1-52
Listed directory vision
Viewed DefectSegmenterFactory.kt:1-28
Viewed YoloSegDefectSegmenter.kt:1-75
Viewed YoloSegDefectSegmenter.kt:76-140
Listed directory qnn_android_bundle
Listed directory docs
Listed directory pitch
Listed directory guides
Listed directory tools
Listed directory models
Viewed app.py:1-60
Viewed build.gradle.kts:1-91
Viewed PITCH_DECK_PROMPT.md:1-60

### Project Overview: **SmartLease Edge**

**Event & Track:** [iQOO Hackathon 2026](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/README.md#L5) · Smart Living Track · Chennai City Battle  
**Tagline:** *"Offline property verification for rental move-outs using on-device AI"*  
**Core Problem Solved:** Resolves landlord-tenant security deposit disputes in India (where deposits often equal 6–10 months of rent) by providing a 100% offline, timestamped, multi-modal property inspection tool directly on the user's smartphone.

---

### Key Workspace Structure

The project root directory contains two main components (with matching `.zip` archives):

```
IQOO Hackathon/
├── smartlease-edge-chennai-master/       # Complete Android app + Python ML pipeline + pitch deck
│   ├── app/                              # Android Native App (Jetpack Compose + PyTorch Lite + CameraX)
│   ├── ml/                               # Unified Gradio demo app, acoustic & vision predictors
│   ├── docs/                             # Pitch presentation (.pptx), guides, onboarding docs
│   └── tools/                            # Dataset scrapers, acoustic trackers, data validators
└── YOLOV8/                               # YOLOv8 segmentation model, Colab notebook & QNN NPU export
    ├── best.pt                           # Fine-tuned YOLOv8n-Seg PyTorch weights (6.78 MB)
    ├── smartlease_edge_yolov8_seg.ipynb  # End-to-end Google Colab training notebook
    ├── convert_yolov8_seg_qnn_pte.py     # Qualcomm QNN / ExecuTorch Hexagon NPU lowering script
    ├── qnn_android_bundle/               # Compiled NPU assets: yolov8n_seg_htp.pte + libQnn*.so
    └── datasets & prep scripts           # concrete, paint-peel, calibration data & balance scripts
```

---

### Detailed Component Breakdown

#### 1. Android Application ([`smartlease-edge-chennai-master/app`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/app))
- **Tech Stack:** Kotlin, Jetpack Compose, CameraX, PyTorch Mobile Lite (`arm64-v8a`), Google ML Kit (offline OCR), Room Database, Android `PdfDocument`.
- **Target Hardware:** iQOO 15. The **IR blaster** is the only component used that a commodity phone lacks. All inference is on the **CPU** — the Hexagon NPU is not used, and the Sensing Hub has no third-party API.
- **Single Entry Point:**
  - [MainActivity.kt](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/app/src/main/java/com/smartlease/edge/MainActivity.kt): The user inspection journey (pose baseline, camera defect segmentation, acoustic wall tapping, IR appliance trigger, timestamped offline PDF report).
- **On-Device Models in Assets ([`app/src/main/assets`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/app/src/main/assets)):**
  - `yolov8n_seg.ptl` (13.7 MB): the 4-class YOLOv8n-Seg defect segmenter, run on the **CPU** via PyTorch Lite.
  - `acoustic_tap_model.json` (73 KB): the tap classifier — a 36-feature logistic regression with its mel filterbank and DCT basis shipped as data.
  - Removed: `vision_best.ptl` (a 5-class *detection* export, incompatible with the segmentation decoder) and `acoustic_cnn.ptl` (loaded only by the deleted bench activity, whose audio path returned a hardcoded string).

---

#### 2. YOLOv8 Segmentation & Qualcomm NPU Acceleration ([`YOLOV8`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/YOLOV8))
- **Weights:** [best.pt](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/YOLOV8/best.pt) — trained YOLOv8n-Seg model for surface cracks, peeling paint, and structural distress.
- **Pinhole Metric Engine:** Calculates real-world defect area ($\text{sq ft}$) directly from segmentation mask pixels using camera focal length ($f_x, f_y$) and distance ($Z$):
  $$\text{Area}_{\text{sq ft}} = \left(\frac{\text{Mask Pixels} \times Z^2}{f_x \times f_y}\right) \times 10.7639$$
  and matches it against local Chennai plastering/painting repair rates.
- **Colab Training Pipeline:** [smartlease_edge_yolov8_seg.ipynb](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/YOLOV8/smartlease_edge_yolov8_seg.ipynb) (dataset loading, augmentations, training for 50 epochs, evaluation, export to ONNX/TFLite/ExecuTorch).
- **Snapdragon Hexagon HTP Deployment:**
  - [convert_yolov8_seg_qnn_pte.py](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/YOLOV8/convert_yolov8_seg_qnn_pte.py): Quantizes PyTorch model to INT8 (PT2E QNN Quantizer 8a8w).
  - [qnn_android_bundle/](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/YOLOV8/qnn_android_bundle): Pre-compiled [`yolov8n_seg_htp.pte`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/YOLOV8/qnn_android_bundle/yolov8n_seg_htp.pte) (~3.7 MB INT8 model targeting $<30\text{ ms}$ latency on Snapdragon Hexagon NPU) and Qualcomm native runtime libraries (`libQnnHtp.so`, `libQnnSystem.so`).

---

#### 3. Python ML & Interactive Demo ([`smartlease-edge-chennai-master/ml`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/ml))
- **Gradio Interactive App ([`app.py`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/ml/app.py)): A unified browser interface to demonstrate the vision defect segmentation, audio tap classifier, and report generation outside of the phone.
- Submodules for acoustic signal analysis (`ml/acoustic/`), vision prediction (`ml/vision/`), and offline narrative report generation (`ml/report/`).

---

#### 4. Pitch Deck & Hackathon Presentation ([`docs/pitch`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/docs/pitch))
- [SmartLease-Edge-iQOO-Hackathon-2026.pptx](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/docs/pitch/SmartLease-Edge-iQOO-Hackathon-2026.pptx): 17-slide presentation deck designed specifically against the iQOO 2026 judging rubric.
- [PITCH_DECK_PROMPT.md](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/docs/pitch/PITCH_DECK_PROMPT.md): Speaker notes, timing breakdown (3-minute demo script), and hardware mapping (NPU, IR blaster, OIS, Vivo Office Kit).

---

### Multi-Modal Hardware & Diagnostic Workflow

```
┌─────────────────────────────────────────────────────────────┐
│  INPUT LAYER (iQOO 15 Hardware)                             │
│  📷 Camera (AR + Vision) · 🎤 Mic (Acoustics) · 📡 IR Blaster│
└─────────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────┐
│  ON-DEVICE PROCESSING (host CPU — the NPU is not used)      │
│  - Pose baseline: SensorManager rotation delta (in memory)  │
│  - Vision Segmentation: YOLOv8n-Seg (crack/peel/spall)      │
│  - Acoustic Tap Test: AudioRecord + FFT (solid vs. hollow)  │
│  - Appliance: ConsumerIrManager transmit (no receive path)  │
└─────────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────┐
│  REPORT SYNTHESIS & STORAGE (100% Offline)                  │
│  - Room Database: local inspection history (not encrypted)  │
│  - Output: timestamped PDF + SHA-256 of its findings        │
└─────────────────────────────────────────────────────────────┘
```

