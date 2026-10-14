package com.example.ui.screens.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.editor.*

data class ToolTabItem(
  val category: EditorCategory,
  val label: String,
  val icon: ImageVector
)

@Composable
fun EditorBottomBar(
  state: PhotoEditorState,
  onSelectCategory: (EditorCategory) -> Unit,
  onUpdateState: (PhotoEditorState) -> Unit,
  // Retouch helpers
  retouchRadius: Float,
  onRetouchRadiusChange: (Float) -> Unit,
  onClearRetouch: () -> Unit,
  // Drawing helpers
  drawingColor: Long,
  drawingStrokeWidth: Float,
  onDrawingColorChange: (Long) -> Unit,
  onDrawingStrokeWidthChange: (Float) -> Unit,
  onUndoDrawingStroke: () -> Unit,
  onClearDrawing: () -> Unit,
  canUndoDrawing: Boolean,
  // Shapes & text helpers
  onAddShape: (EditorShapeType) -> Unit,
  onAddSticker: (String) -> Unit,
  onClearShapes: () -> Unit,
  onAddText: (String) -> Unit,
  onDeleteText: (String) -> Unit,
  onAddPrivacyRegion: (BlurMosaicType) -> Unit,
  onClearPrivacyRegions: () -> Unit,
  imageAspectRatio: Float = 1f,
  modifier: Modifier = Modifier
) {
  val toolTabs = listOf(
    ToolTabItem(EditorCategory.CROP, "Crop", Icons.Rounded.Crop),
    ToolTabItem(EditorCategory.LIGHT, "Light", Icons.Rounded.WbSunny),
    ToolTabItem(EditorCategory.COLOR, "Color", Icons.Rounded.Palette),
    ToolTabItem(EditorCategory.HSL, "HSL", Icons.Rounded.ColorLens),
    ToolTabItem(EditorCategory.CURVES, "Curves", Icons.Rounded.ShowChart),
    ToolTabItem(EditorCategory.DETAIL, "Detail", Icons.Rounded.Details),
    ToolTabItem(EditorCategory.EFFECTS, "Effects", Icons.Rounded.AutoFixHigh),
    ToolTabItem(EditorCategory.GRADING, "Grading", Icons.Rounded.Gradient),
    ToolTabItem(EditorCategory.RETOUCH, "Healing", Icons.Rounded.Healing),
    ToolTabItem(EditorCategory.PRIVACY, "Blur", Icons.Rounded.VisibilityOff),
    ToolTabItem(EditorCategory.DRAW, "Draw", Icons.Rounded.Brush),
    ToolTabItem(EditorCategory.TEXT, "Text", Icons.Rounded.TextFields),
    ToolTabItem(EditorCategory.SHAPES, "Shapes", Icons.Rounded.Category),
    ToolTabItem(EditorCategory.LAYERS, "Layers", Icons.Rounded.Layers)
  )

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = Color(0xFF14171F).copy(alpha = 0.98f)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
    ) {
      // 1. Tool Drawer for active category
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 280.dp)
          .background(Color(0xFF10131A))
      ) {
        when (state.activeCategory) {
          EditorCategory.CROP -> {
            CropTransformPanel(
              crop = state.cropTransform,
              imageAspectRatio = imageAspectRatio,
              onCropChange = { onUpdateState(state.copy(cropTransform = it)) },
              onResetCrop = { onUpdateState(state.copy(cropTransform = CropTransform())) }
            )
          }

          EditorCategory.LIGHT -> {
            LightAdjustPanel(
              light = state.lightAdjustments,
              onLightChange = { onUpdateState(state.copy(lightAdjustments = it)) },
              onReset = { onUpdateState(state.copy(lightAdjustments = LightAdjustments())) }
            )
          }

          EditorCategory.COLOR -> {
            ColorAdjustPanel(
              color = state.colorAdjustments,
              onColorChange = { onUpdateState(state.copy(colorAdjustments = it)) },
              onReset = { onUpdateState(state.copy(colorAdjustments = ColorAdjustments())) }
            )
          }

          EditorCategory.HSL -> {
            HslPanel(
              hsl = state.hslAdjustments,
              onHslChange = { onUpdateState(state.copy(hslAdjustments = it)) },
              onReset = { onUpdateState(state.copy(hslAdjustments = HslAdjustments())) }
            )
          }

          EditorCategory.CURVES -> {
            CurvesPanel(
              curves = state.curvesAdjustment,
              onCurvesChange = { onUpdateState(state.copy(curvesAdjustment = it)) },
              onReset = { onUpdateState(state.copy(curvesAdjustment = CurvesAdjustment())) }
            )
          }

          EditorCategory.DETAIL, EditorCategory.EFFECTS -> {
            DetailEffectsPanel(
              detail = state.detailAdjustments,
              vignette = state.vignetteEffects,
              onDetailChange = { onUpdateState(state.copy(detailAdjustments = it)) },
              onVignetteChange = { onUpdateState(state.copy(vignetteEffects = it)) },
              onReset = {
                onUpdateState(state.copy(detailAdjustments = DetailAdjustments(), vignetteEffects = VignetteEffects()))
              }
            )
          }

          EditorCategory.GRADING -> {
            ColorGradingPanel(
              grading = state.colorGrading,
              onGradingChange = { onUpdateState(state.copy(colorGrading = it)) },
              onReset = { onUpdateState(state.copy(colorGrading = ColorGrading())) }
            )
          }

          EditorCategory.RETOUCH -> {
            RetouchHealingPanel(
              retouchItems = state.retouchItems,
              currentRadius = retouchRadius,
              onRadiusChange = onRetouchRadiusChange,
              onClearAll = onClearRetouch
            )
          }

          EditorCategory.PRIVACY -> {
            BlurMosaicPanel(
              regions = state.blurMosaicRegions,
              onAddRegion = onAddPrivacyRegion,
              onClearRegions = onClearPrivacyRegions
            )
          }

          EditorCategory.DRAW -> {
            DrawingPanel(
              currentColor = drawingColor,
              strokeWidth = drawingStrokeWidth,
              onColorChange = onDrawingColorChange,
              onStrokeWidthChange = onDrawingStrokeWidthChange,
              onUndoStroke = onUndoDrawingStroke,
              onClearDrawing = onClearDrawing,
              canUndoStroke = canUndoDrawing
            )
          }

          EditorCategory.TEXT -> {
            TextLayerPanel(
              textLayers = state.textLayers,
              onAddText = onAddText,
              onDeleteText = onDeleteText
            )
          }

          EditorCategory.SHAPES -> {
            ShapesStickersPanel(
              shapes = state.shapes,
              onAddShape = onAddShape,
              onAddSticker = onAddSticker,
              onClearShapes = onClearShapes
            )
          }

          EditorCategory.LAYERS -> {
            LayersPanel(
              layers = state.layers,
              onToggleVisibility = { id ->
                val updated = state.layers.map {
                  if (it.id == id) it.copy(isVisible = !it.isVisible) else it
                }
                onUpdateState(state.copy(layers = updated))
              }
            )
          }
        }
      }

      Divider(color = Color(0xFF222834))

      // 2. Scrollable Category Tabs Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        toolTabs.forEach { tab ->
          val isSelected = state.activeCategory == tab.category
          Column(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color.Transparent)
              .clickable { onSelectCategory(tab.category) }
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .testTag("tab_${tab.label.lowercase()}"),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = tab.icon,
              contentDescription = tab.label,
              tint = if (isSelected) Color(0xFF60A5FA) else Color(0xFF94A3B8),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = tab.label,
              color = if (isSelected) Color.White else Color(0xFF94A3B8),
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
          }
        }
      }
    }
  }
}
