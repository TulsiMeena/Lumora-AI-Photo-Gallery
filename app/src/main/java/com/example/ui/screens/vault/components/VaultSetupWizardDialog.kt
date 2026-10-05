package com.example.ui.screens.vault.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GalleryTheme

@Composable
fun VaultSetupWizardDialog(
  isBiometricHardwareAvailable: Boolean = false,
  onSetupCompleted: (pin: String, enableBiometrics: Boolean) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  var step by remember { mutableIntStateOf(1) } // 1 = Intro/Recovery, 2 = Create PIN, 3 = Confirm PIN, 4 = Biometrics/Final
  var createdPin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var enableBiometrics by remember { mutableStateOf(isBiometricHardwareAvailable) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = colors.surface,
      border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(16.dp)
        .testTag("dialog_vault_setup")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Step Indicator Dots
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(bottom = 16.dp)
        ) {
          for (s in 1..4) {
            Box(
              modifier = Modifier
                .height(4.dp)
                .width(if (step == s) 24.dp else 12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (step >= s) colors.accent else colors.surfaceVariant)
            )
          }
        }

        when (step) {
          1 -> {
            // STEP 1: Privacy Architecture & Recovery Warning
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.Security, contentDescription = null, tint = colors.accent, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "Set Up Private Vault",
              color = colors.primaryText,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = "True hardware-backed AES-256 encryption protects your confidential photos and videos on-device.",
              color = colors.secondaryText,
              fontSize = 12.sp,
              textAlign = TextAlign.Center,
              lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Recovery Warning Card
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = colors.warning.copy(alpha = 0.12f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.warning, modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Important Recovery Notice",
                    color = colors.warning,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Vault content is encrypted with your PIN and device Keystore. We never store your raw PIN or upload your photos to any cloud. If you lose your PIN, encrypted data cannot be recovered.",
                  color = colors.primaryText,
                  fontSize = 11.sp,
                  lineHeight = 15.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
              onClick = { step = 2 },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_setup_step1_next")
            ) {
              Text("I Understand • Create PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
              onClick = onDismiss,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Cancel", color = colors.secondaryText)
            }
          }

          2 -> {
            // STEP 2: Create PIN
            Text(
              text = "Create a Vault PIN",
              color = colors.primaryText,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Enter at least 4 digits (4 to 8 digits supported)",
              color = colors.secondaryText,
              fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PIN Dots
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
              for (i in 0 until maxOf(4, createdPin.length)) {
                val filled = createdPin.length > i
                Box(
                  modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (filled) colors.accent else colors.surfaceVariant)
                    .border(1.dp, if (filled) colors.accent else colors.border, CircleShape)
                )
              }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Numeric Keypad
            SetupKeypad(
              currentLength = createdPin.length,
              onDigit = {
                if (createdPin.length < 8) createdPin += it
              },
              onDelete = {
                if (createdPin.isNotEmpty()) createdPin = createdPin.dropLast(1)
              }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
              onClick = {
                if (createdPin.length >= 4) {
                  step = 3
                }
              },
              enabled = createdPin.length >= 4,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("btn_setup_step2_next")
            ) {
              Text("Continue to Confirm", fontWeight = FontWeight.Bold)
            }
          }

          3 -> {
            // STEP 3: Confirm PIN
            Text(
              text = "Confirm Your PIN",
              color = colors.primaryText,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Re-enter the same PIN to verify",
              color = colors.secondaryText,
              fontSize = 12.sp
            )

            if (errorMessage != null) {
              Text(
                text = errorMessage!!,
                color = colors.warning,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PIN Dots
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
              for (i in 0 until maxOf(4, createdPin.length)) {
                val filled = confirmPin.length > i
                Box(
                  modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (filled) colors.accent else colors.surfaceVariant)
                    .border(1.dp, if (filled) colors.accent else colors.border, CircleShape)
                )
              }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SetupKeypad(
              currentLength = confirmPin.length,
              onDigit = {
                if (confirmPin.length < 8) {
                  val next = confirmPin + it
                  confirmPin = next
                  errorMessage = null
                  if (next.length == createdPin.length) {
                    if (next == createdPin) {
                      step = 4
                    } else {
                      errorMessage = "PINs do not match. Try again."
                      confirmPin = ""
                    }
                  }
                }
              },
              onDelete = {
                if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
              }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
              onClick = {
                confirmPin = ""
                step = 2
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Back to Change PIN", color = colors.secondaryText)
            }
          }

          4 -> {
            // STEP 4: Optional Biometrics & Finish
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(34.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "PIN Successfully Set!",
              color = colors.primaryText,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )

            Text(
              text = "Your Private Vault is ready to encrypt media.",
              color = colors.secondaryText,
              fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Biometric Option Toggle
            if (isBiometricHardwareAvailable) {
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text("Enable Biometric Unlock", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                      Text("Fingerprint or Face authentication", color = colors.secondaryText, fontSize = 11.sp)
                    }
                  }

                  Switch(
                    checked = enableBiometrics,
                    onCheckedChange = { enableBiometrics = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent)
                  )
                }
              }
              Spacer(modifier = Modifier.height(18.dp))
            }

            Button(
              onClick = {
                onSetupCompleted(createdPin, enableBiometrics)
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_setup_finish")
            ) {
              Text("Enter Private Vault", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SetupKeypad(
  currentLength: Int,
  onDigit: (String) -> Unit,
  onDelete: () -> Unit
) {
  val colors = GalleryTheme.colors
  val rows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("", "0", "DEL")
  )

  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    rows.forEach { row ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        row.forEach { key ->
          if (key.isEmpty()) {
            Box(modifier = Modifier.size(56.dp))
          } else {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant)
                .clickable {
                  if (key == "DEL") onDelete() else onDigit(key)
                },
              contentAlignment = Alignment.Center
            ) {
              if (key == "DEL") {
                Icon(Icons.Rounded.Backspace, contentDescription = "Delete", tint = colors.primaryText, modifier = Modifier.size(18.dp))
              } else {
                Text(text = key, color = colors.primaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
