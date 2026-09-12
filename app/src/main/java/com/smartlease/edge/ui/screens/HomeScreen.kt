package com.smartlease.edge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onStartWalkthrough: () -> Unit,
    onOpenSelfTest: () -> Unit,
    onOpenTapCapture: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Which models actually shipped in this APK. Listing the assets directory is cheap —
    // deliberately not loading them here, because loading the segmentation model is a
    // 13.7 MB copy plus a TorchScript parse and that belongs off the home screen.
    val assets = remember {
        runCatching { context.assets.list("")?.toSet() ?: emptySet() }.getOrDefault(emptySet())
    }
    val hasVisionModel = "yolov8n_seg.ptl" in assets
    val hasTapModel = "acoustic_tap_model.json" in assets

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SmartLease Edge", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Offline property verification — pose tracking, on-device diagnostics, " +
                    "IR appliance trigger, timestamped local report. No cloud call " +
                    "at any stage.",
            fontSize = 14.sp
        )
        Spacer(Modifier.height(28.dp))

        Button(onClick = onStartWalkthrough, modifier = Modifier.fillMaxWidth()) {
            Text("Start Walkthrough")
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenSelfTest, modifier = Modifier.fillMaxWidth()) {
            Text("Device & Model Check")
        }

        // Debug builds only: MainActivity passes null in release, so this never renders.
        onOpenTapCapture?.let { open ->
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = open, modifier = Modifier.fillMaxWidth()) {
                Text("Tap capture (debug)")
            }
        }

        Spacer(Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("What runs in this build", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (hasVisionModel)
                        "Vision — 4-class YOLOv8n-Seg, on the CPU via PyTorch Lite. Area is reported as a share of the frame, not a physical measurement."
                    else
                        "Vision — colour/contrast heuristic. No segmentation model in this build; findings are labelled as heuristic.",
                    fontSize = 12.sp
                )
                Text(
                    if (hasTapModel)
                        "Acoustics — 36-feature logistic regression on the CPU, reported with its cross-validated accuracy."
                    else
                        "Acoustics — decay-time heuristic. No trained model in this build.",
                    fontSize = 12.sp
                )
                Text("Report — rule-based text, rendered offline with Android PdfDocument.", fontSize = 12.sp)
                Text("IR — transmit only. Android cannot receive IR, so a trigger is logged as sent, not as verified.", fontSize = 12.sp)
                Text("No network permission. Backups disabled.", fontSize = 12.sp)
            }
        }
    }
}
