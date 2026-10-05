package com.example.utils.vault

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object VaultBiometricHelper {

  /**
   * Checks if biometric hardware is present and user has enrolled biometrics.
   */
  fun isBiometricAvailable(context: Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
      BiometricManager.BIOMETRIC_SUCCESS -> true
      else -> false
    }
  }

  /**
   * Prompts the user with standard Android BiometricPrompt on FragmentActivity.
   */
  fun authenticateWithBiometrics(
    activity: FragmentActivity,
    title: String = "Unlock Private Vault",
    subtitle: String = "Verify biometric identity to access protected photos",
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        onSuccess()
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        onError(errString.toString())
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        onError("Biometric authentication failed. Try again or use PIN.")
      }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setNegativeButtonText("Use PIN")
      .build()

    val prompt = BiometricPrompt(activity, executor, callback)
    prompt.authenticate(promptInfo)
  }
}
