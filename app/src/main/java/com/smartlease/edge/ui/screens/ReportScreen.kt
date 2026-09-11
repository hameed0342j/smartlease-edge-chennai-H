package com.smartlease.edge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartlease.edge.report.FindingsDigest
import com.smartlease.edge.report.InspectionReport

@Composable
fun ReportScreen(report: InspectionReport?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Text("< Back") }
        Text("Inspection Report", fontWeight = FontWeight.Bold, fontSize = 22.sp)

        if (report == null) {
            Spacer(Modifier.height(16.dp))
            Text("No report generated yet for this session.")
            return@Column
        }

        Spacer(Modifier.height(4.dp))
        Text(report.propertyLabel)
        Text("Session ${report.sessionId}", fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Text(report.overallVerdict, Modifier.padding(12.dp))
        }

        // Shown on screen as well as in the PDF footer so both parties can read the same
        // digest off the same phone and check it against the document afterwards.
        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "SHA-256 of ${report.findingCount} finding(s)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(FindingsDigest.grouped(report.findingsSha256), fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(report.findingsSha256, fontSize = 9.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Change one character of one finding and this digest changes. It is not a " +
                            "signature — it does not identify who recorded them.",
                    fontSize = 10.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(report.sections) { section ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(section.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(section.body, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
