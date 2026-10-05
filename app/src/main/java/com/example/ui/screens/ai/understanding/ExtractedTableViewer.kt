package com.example.ui.screens.ai.understanding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ai.understanding.ExtractedTableData
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractedTableViewer(
  tableData: ExtractedTableData,
  onSavePdf: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // Editable local state
  var currentHeaders by remember(tableData) { mutableStateOf(tableData.headers) }
  var currentRows by remember(tableData) { mutableStateOf(tableData.rows) }
  var uncertainCells by remember(tableData) { mutableStateOf(tableData.uncertainCells) }

  // Cell Edit Dialog State
  var editingCellCoords by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (row, col) - row -1 for header
  var editingCellValue by remember { mutableStateOf("") }

  fun copyToClipboard() {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val tsv = ExtractedTableData(headers = currentHeaders, rows = currentRows).toTsvString()
    val clip = ClipData.newPlainText("Extracted Table", tsv)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Table copied to clipboard (TSV / Excel format)", Toast.LENGTH_SHORT).show()
  }

  fun exportCsv() {
    try {
      val csvText = ExtractedTableData(headers = currentHeaders, rows = currentRows).toCsvString()
      val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      val file = File(downloadsDir, "MyGalleryAI_Table_${System.currentTimeMillis()}.csv")
      FileOutputStream(file).use { out ->
        out.write(csvText.toByteArray())
      }
      Toast.makeText(context, "Saved to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
      Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun shareTable() {
    val csvText = ExtractedTableData(headers = currentHeaders, rows = currentRows).toCsvString()
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, csvText)
      type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Table Data"))
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_extracted_table"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330)),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      // Header & Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.TableChart, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Extracted Table (${currentRows.size} rows, ${currentHeaders.size} cols)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Row {
          IconButton(onClick = { copyToClipboard() }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Table", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
          }
          IconButton(onClick = { exportCsv() }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Rounded.FileDownload, contentDescription = "Export CSV", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
          }
          IconButton(onClick = { shareTable() }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
          }
        }
      }

      if (uncertainCells.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Amber cells are uncertain — tap to verify or edit values",
            color = Color(0xFFFBBF24),
            fontSize = 10.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Scrollable Table Grid
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF131722))
          .horizontalScroll(rememberScrollState())
          .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
      ) {
        Column {
          // Headers row
          if (currentHeaders.isNotEmpty()) {
            Row(
              modifier = Modifier
                .background(Color(0xFF283145))
                .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.15f))
            ) {
              currentHeaders.forEachIndexed { colIdx, header ->
                Box(
                  modifier = Modifier
                    .widthIn(min = 90.dp, max = 220.dp)
                    .clickable {
                      editingCellCoords = Pair(-1, colIdx)
                      editingCellValue = header
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                  Text(
                    text = header,
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }

          // Data rows
          currentRows.forEachIndexed { rowIdx, rowCells ->
            Row(
              modifier = Modifier
                .background(if (rowIdx % 2 == 0) Color(0xFF131722) else Color(0xFF1A1F2C))
                .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
            ) {
              rowCells.forEachIndexed { colIdx, cellValue ->
                val isUncertain = uncertainCells.contains(Pair(rowIdx, colIdx))
                Box(
                  modifier = Modifier
                    .widthIn(min = 90.dp, max = 220.dp)
                    .then(
                      if (isUncertain) Modifier.background(Color(0x3DFBBF24)) else Modifier
                    )
                    .clickable {
                      editingCellCoords = Pair(rowIdx, colIdx)
                      editingCellValue = cellValue
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isUncertain) {
                      Icon(
                        imageVector = Icons.Rounded.HelpOutline,
                        contentDescription = "Uncertain value",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(12.dp)
                      )
                      Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                      text = cellValue.ifBlank { "—" },
                      color = if (isUncertain) Color(0xFFFDE68A) else Color.White,
                      fontSize = 11.sp,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Edit Cell Dialog
  if (editingCellCoords != null) {
    val (rIdx, cIdx) = editingCellCoords!!
    val isHeader = rIdx == -1

    AlertDialog(
      onDismissRequest = { editingCellCoords = null },
      title = {
        Text(if (isHeader) "Edit Column Header" else "Edit Cell (Row ${rIdx + 1}, Col ${cIdx + 1})")
      },
      text = {
        OutlinedTextField(
          value = editingCellValue,
          onValueChange = { editingCellValue = it },
          singleLine = true,
          label = { Text("Value") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (isHeader) {
              val updated = currentHeaders.toMutableList()
              if (cIdx in updated.indices) {
                updated[cIdx] = editingCellValue
                currentHeaders = updated
              }
            } else {
              val updatedRows = currentRows.map { it.toMutableList() }.toMutableList()
              if (rIdx in updatedRows.indices && cIdx in updatedRows[rIdx].indices) {
                updatedRows[rIdx][cIdx] = editingCellValue
                currentRows = updatedRows
                // Remove from uncertain once edited
                uncertainCells = uncertainCells - setOf(Pair(rIdx, cIdx))
              }
            }
            editingCellCoords = null
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingCellCoords = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
