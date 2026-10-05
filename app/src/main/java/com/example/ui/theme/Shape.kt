package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val GalleryShapes = Shapes(
  extraSmall = RoundedCornerShape(6.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(28.dp)
)

val ThumbnailShape = RoundedCornerShape(12.dp)
val CardShape = RoundedCornerShape(18.dp)
val PillShape = RoundedCornerShape(100.dp)
val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
