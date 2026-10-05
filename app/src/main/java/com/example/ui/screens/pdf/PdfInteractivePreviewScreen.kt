package com.example.ui.screens.pdf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.pdf.PdfStudioEngine
import com.example.model.pdf.PdfBackground
import com.example.model.pdf.PdfConfig
import com.example.model.pdf.PdfCreatorPage
import com.example.model.pdf.PdfImageFit
import com.example.model.pdf.PdfOrientation
import com.example.ui.theme.LocalGalleryColors
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfInteractivePreviewScreen(
  pages: List<PdfCreatorPage>,
  config: PdfConfig,
  onBackToEdit: () -> Unit,
  onGeneratePdf: () -> Unit
) {
  val colors = LocalGalleryColors.current
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })

  var zoomScale by remember { mutableFloatStateOf(1f) }
  var panOffset by remember { mutableFloatStateOf(0f) }

  val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
    zoomScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
  }

  val estimatedSize = remember(pages.size, config) {
    PdfStudioEngine.estimateFileSize(pages.size, config)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "PDF Preview",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = colors.textPrimary
            )
            Text(
              text = "${config.standardSize.displayName} • ${config.orientation.displayName} • $estimatedSize",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackToEdit,
            modifier = Modifier.testTag("preview_back_btn")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back to Editing",
              tint = colors.textPrimary
            )
          }
        },
        actions = {
          // Reset zoom button if zoomed
          if (zoomScale > 1.05f) {
            IconButton(onClick = { zoomScale = 1f }) {
              Icon(
                imageVector = Icons.Rounded.ZoomOut,
                contentDescription = "Reset Zoom",
                tint = colors.textPrimary
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = colors.surfaceBackground
        )
      )
    },
    bottomBar = {
      Surface(
        color = colors.surfaceCard,
        shadowElevation = 8.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          // Thumbnail carousel
          if (pages.size > 1) {
            LazyRow(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
              itemsIndexed(pages) { idx, page ->
                val isSelected = pagerState.currentPage == idx
                Box(
                  modifier = Modifier
                    .size(width = 46.dp, height = 62.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                      width = if (isSelected) 2.dp else 1.dp,
                      color = if (isSelected) MaterialTheme.colorScheme.primary else colors.divider,
                      shape = RoundedCornerShape(6.dp)
                    )
                    .background(colors.cardBackground)
                    .clickable {
                      coroutineScope.launch {
                        pagerState.animateScrollToPage(idx)
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  AsyncImage(
                    model = ImageRequest.Builder(context)
                      .data(page.thumbnailUri ?: page.sourceUri)
                      .crossfade(true)
                      .build(),
                    contentDescription = "Page ${idx + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                  )
                  // Page number badge
                  Box(
                    modifier = Modifier
                      .align(Alignment.BottomEnd)
                      .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topStart = 4.dp))
                      .padding(horizontal = 4.dp, vertical = 1.dp)
                  ) {
                    Text(
                      text = "${idx + 1}",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  }
                }
              }
            }
          }

          // Bottom Action Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${pagerState.currentPage + 1} of ${pages.size} Pages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
              )
              Text(
                text = "Target: ${if (config.effectiveTargetBytes > 0) config.targetSizeOption.displayName else "Auto-Balanced"}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
              )
            }

            Button(
              onClick = onGeneratePdf,
              modifier = Modifier
                .height(48.dp)
                .testTag("generate_pdf_btn"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
              )
            ) {
              Icon(
                imageVector = Icons.Rounded.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Create PDF",
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(Color(0xFF0F172A)) // Dark slate background to make paper pop
    ) {
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
      ) { pageIndex ->
        val page = pages[pageIndex]

        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          // Calculate paper aspect ratio
          val orientation = page.orientationOverride ?: config.orientation
          val isLandscape = orientation == PdfOrientation.LANDSCAPE
          val paperAspect = if (isLandscape) {
            config.effectiveHeightPt / config.effectiveWidthPt
          } else {
            config.effectiveWidthPt / config.effectiveHeightPt
          }

          val paperColor = when (config.background) {
            PdfBackground.WHITE -> Color.White
            PdfBackground.IVORY -> Color(0xFFFAF8F5)
            PdfBackground.SOFT_GRAY -> Color(0xFFF3F4F6)
            PdfBackground.DARK -> Color(0xFF1E293B)
            PdfBackground.CUSTOM -> Color(config.customBackgroundColor)
          }

          // Virtual Paper Container
          Card(
            modifier = Modifier
              .fillMaxWidth(0.92f)
              .aspectRatio(paperAspect.coerceIn(0.5f, 2.0f))
              .shadow(elevation = 16.dp, shape = RoundedCornerShape(4.dp))
              .graphicsLayer {
                scaleX = zoomScale
                scaleY = zoomScale
              }
              .transformable(state = transformableState),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = paperColor)
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(
                  all = when (config.margin) {
                    com.example.model.pdf.PdfMargin.NONE -> 0.dp
                    com.example.model.pdf.PdfMargin.SMALL -> 6.dp
                    com.example.model.pdf.PdfMargin.MEDIUM -> 14.dp
                    com.example.model.pdf.PdfMargin.LARGE -> 24.dp
                    com.example.model.pdf.PdfMargin.CUSTOM -> (config.customMarginValue * 0.8f).dp
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              val fit = page.fitOverride ?: config.imageFit
              val contentScale = when (fit) {
                PdfImageFit.FIT -> ContentScale.Fit
                PdfImageFit.FILL -> ContentScale.Crop
                PdfImageFit.CROP -> ContentScale.Crop
                PdfImageFit.STRETCH -> ContentScale.FillBounds
                PdfImageFit.ORIGINAL -> ContentScale.Inside
                PdfImageFit.CENTER -> ContentScale.None
              }

              AsyncImage(
                model = ImageRequest.Builder(context)
                  .data(page.sourceUri)
                  .crossfade(true)
                  .build(),
                contentDescription = "PDF Page ${pageIndex + 1}",
                contentScale = contentScale,
                modifier = Modifier
                  .fillMaxSize()
                  .graphicsLayer {
                    rotationZ = page.rotationDegrees.toFloat()
                  }
              )
            }
          }
        }
      }

      // Page Indicator Overlay Pill
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 16.dp)
          .background(Color.Black.copy(alpha = 0.65f), CircleShape)
          .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Text(
          text = "Page ${pagerState.currentPage + 1} of ${pages.size}",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  }
}
