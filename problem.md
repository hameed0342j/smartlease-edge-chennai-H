

Based on the hackathon documentation and pitch deck in the repository, here is the official event track and the problem statement addressed:

---

### 1. Official iQOO Hackathon Theme & Track
* **Hackathon:** **iQOO Hackathon 2026 — Chennai City Battle** (Organized by iQOO India & Reskilll)
* **Official Track:** **Smart Living**
* **Core Challenge Mandate:** Build phone-first, hardware-integrated solutions targeting modern living and daily life challenges by leveraging on-device AI (Snapdragon Hexagon NPU), device hardware (camera, audio, IR blaster, sensors), and Vivo Office Kit—with a strong emphasis on running **100% offline on-device** rather than cloud APIs.

---

### 2. Project Problem Statement (SmartLease Edge)

> **"Move-out day in India is a memory contest, with up to ten months' rent at stake."**
> 
> * **The Core Problem:** Security deposit disputes account for over 40% of all landlord–tenant conflicts in India, primarily rooted in the lack of objective, timestamped move-in/move-out documentation.
> * **The Operational Gap:** Move-outs occur in vacated flats where Wi-Fi is often disconnected and cellular signals can be weak. Existing solutions rely on expensive human inspectors (₹1,649–₹5,999) or cloud-dependent SaaS platforms that fail offline and do not functionally test appliances.
> * **The Stated Objective (as originally framed):** a 100% offline, multi-modal property verification system that turns visual damage into verifiable square footage, acoustically classifies wall/tile integrity, verifies appliance functionality via IR, and outputs a tamper-evident, signed PDF with zero internet dependency.
>
> * **What was actually built, as of 2026-09-12:** a 100% offline, multi-modal **single-session condition record**. It segments four defect classes on-device and reports each as a share of the frame (not square footage — there is no depth or focal length on device); classifies a knuckle tap hollow/solid with its cross-validated accuracy; fires an IR code and records that it was **sent** (Android cannot receive IR, so appliance function is not verified); and renders an offline PDF carrying a **SHA-256 of exactly its findings** on every page — which detects alteration but is **not** a signature and identifies nobody.
>
> * **Not built:** the move-in ↔ move-out comparison the problem statement implies. There is no stored baseline, no retained photograph, no phone-to-phone transport, no diff, no wear-and-tear rules and no fixture inventory. Pitch the single-session record, which is real, rather than the comparison, which is not.
