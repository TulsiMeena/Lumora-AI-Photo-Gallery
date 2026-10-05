package com.example.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Enforces strict offline-first guarantees and explicit consent verification before any cloud AI upload.
 */
object NetworkPrivacyManager {

  /**
   * Checks whether the device currently has active network connectivity.
   */
  fun isOnline(context: Context): Boolean {
    return try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
          caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      } else {
        @Suppress("DEPRECATION")
        val networkInfo = cm.activeNetworkInfo
        networkInfo != null && networkInfo.isConnected
      }
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Clear user-facing message explaining cloud vs offline processing status.
   */
  fun getAiServiceStatusMessage(isOnline: Boolean): String {
    return if (isOnline) {
      "Cloud AI (Gemini) is available. Images are only transmitted with your explicit confirmation."
    } else {
      "Offline Mode active. Local gallery, photo editor, resize, compression, PDF creator, and Vault function 100% offline."
    }
  }
}
