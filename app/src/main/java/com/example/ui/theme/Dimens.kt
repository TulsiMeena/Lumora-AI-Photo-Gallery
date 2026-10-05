package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class GalleryDimens(
  val spaceXxs: Dp = 2.dp,
  val spaceXs: Dp = 4.dp,
  val spaceSm: Dp = 8.dp,
  val spaceMd: Dp = 12.dp,
  val spaceLg: Dp = 16.dp,
  val spaceXl: Dp = 20.dp,
  val space2xl: Dp = 24.dp,
  val space3xl: Dp = 32.dp,
  val space4xl: Dp = 48.dp,

  val minTouchTarget: Dp = 48.dp,
  val iconSm: Dp = 18.dp,
  val iconMd: Dp = 24.dp,
  val iconLg: Dp = 32.dp,

  val bottomNavHeight: Dp = 68.dp,
  val topBarHeight: Dp = 64.dp,
  val thumbnailCornerRadius: Dp = 12.dp,
  val cardCornerRadius: Dp = 16.dp,
  val sheetCornerRadius: Dp = 24.dp,
  val dialogCornerRadius: Dp = 24.dp
)

val DefaultGalleryDimens = GalleryDimens()
