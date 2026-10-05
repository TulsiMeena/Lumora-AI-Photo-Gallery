package com.example.ui.screens.vault.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

@Composable
fun VaultPinKeypadDialog(
  expectedPinLength: Int = 4,
  isBiometricAvailable: Boolean = false,
  isBiometricEnabled: Boolean = false,
  lockoutRemainingSeconds: Int = 0,
  errorMessage: String? = null,
  onPinEntered: (String) -> Unit,
  onBiometricClick: () -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  var enteredPin by remember { mutableStateOf("") }
  var countdown by remember(lockoutRemainingSeconds) { mutableIntStateOf(lockoutRemainingSeconds) }

  LaunchedEffect(countdown) {
    if (countdown > 0) {
      delay(1000)
      countdown -= 1
    }
  }

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
        .testTag("dialog_vault_pin_keypad")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Vault Lock Icon Header
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(28.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Unlock Private Vault",
          color = colors.primaryText,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "Hardware-backed AES-256 GCM Protection",
          color = colors.secondaryText,
          fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Lockout warning
        if (countdown > 0) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(colors.warning.copy(alpha = 0.15f))
              .padding(10.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.warning, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Too many failed attempts. Try again in $countdown seconds.",
                color = colors.warning,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Error message if wrong PIN
        if (errorMessage != null && countdown <= 0) {
          Text(
            text = errorMessage,
            color = colors.warning,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }

        // PIN Indicator Dots
        val displayDotsCount = maxOf(4, expectedPinLength)
        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier.padding(vertical = 12.dp)
        ) {
          for (i in 0 until displayDotsCount) {
            val isFilled = enteredPin.length > i
            Box(
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (isFilled) colors.accent else colors.surfaceVariant)
                .border(
                  width = 1.dp,
                  color = if (isFilled) colors.accent else colors.border,
                  shape = CircleShape
                )
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Numeric Keypad Grid
        val keypadRows = listOf(
          listOf("1", "2", "3"),
          listOf("4", "5", "6"),
          listOf("7", "8", "9"),
          listOf("BIO", "0", "DEL")
        )

        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          keypadRows.forEach { row ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              row.forEach { key ->
                val isBioButton = key == "BIO"
                val isDelButton = key == "DEL"
                val isBioVisible = isBioButton && isBiometricAvailable && isBiometricEnabled

                Box(
                  modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(
                      when {
                        isBioButton && !isBioVisible -> Color.Transparent
                        isDelButton -> colors.surfaceVariant.copy(alpha = 0.6f)
                        isBioButton -> colors.accent.copy(alpha = 0.15f)
                        else -> colors.surfaceVariant
                      }
                    )
                    .clickable(enabled = countdown <= 0 && (!isBioButton || isBioVisible)) {
                      when (key) {
                        "DEL" -> {
                          if (enteredPin.isNotEmpty()) {
                            com.example.utils.sound.AppSoundManager.playKeyClick()
                            enteredPin = enteredPin.dropLast(1)
                          }
                        }
                        "BIO" -> {
                          if (isBioVisible) {
                            com.example.utils.sound.AppSoundManager.playKeyClick()
                            onBiometricClick()
                          }
                        }
                        else -> {
                          if (enteredPin.length < 8) {
                            com.example.utils.sound.AppSoundManager.playKeyClick()
                            val nextPin = enteredPin + key
                            enteredPin = nextPin
                            if (nextPin.length >= expectedPinLength) {
                              onPinEntered(nextPin)
                              enteredPin = ""
                            }
                          }
                        }
                      }
                    }
                    .testTag("btn_pin_$key"),
                  contentAlignment = Alignment.Center
                ) {
                  when {
                    isDelButton -> {
                      Icon(
                        imageVector = Icons.Rounded.Backspace,
                        contentDescription = "Delete",
                        tint = colors.primaryText,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                    isBioButton -> {
                      if (isBioVisible) {
                        Icon(
                          imageVector = Icons.Rounded.Fingerprint,
                          contentDescription = "Biometric Unlock",
                          tint = colors.accent,
                          modifier = Modifier.size(26.dp)
                        )
                      }
                    }
                    else -> {
                      Text(
                        text = key,
                        color = colors.primaryText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Cancel / Dismiss
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("btn_cancel_pin")
        ) {
          Text("Cancel", color = colors.secondaryText, fontSize = 13.sp)
        }
      }
    }
  }
}
