package com.burton.photos.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.PhotoFilter
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.photos.data.edit.CropAspect
import com.burton.photos.data.edit.CropMath
import com.burton.photos.data.edit.CropWindow
import com.burton.photos.data.edit.PhotoFilter
import com.burton.photos.ui.components.ScreenMessage
import com.burton.photos.ui.local.hasWriteImageAccess
import com.burton.photos.ui.local.writeImagePermissions
import com.burton.photos.ui.theme.BurtonBlack
import com.burton.photos.ui.theme.BurtonElevated
import com.burton.photos.ui.theme.BurtonIvory
import com.burton.photos.ui.theme.BurtonMute
import com.burton.photos.ui.theme.BurtonSand

private enum class EditTab { Crop, Adjust, Filters }

@Composable
fun EditScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: EditViewModel = hiltViewModel(),
) {
    val photo by viewModel.photo.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val edits by viewModel.edits.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var tab by remember { mutableStateOf(EditTab.Crop) }
    var aspect by remember { mutableStateOf(CropAspect.Original) }
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var cropSize by remember { mutableStateOf(IntSize.Zero) }
    val bitmap = preview
    LaunchedEffect(bitmap) {
        scale = 1f
        pan = Offset.Zero
    }
    val writeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val current = bitmap
        if (current != null && (granted.values.all { it } || hasWriteImageAccess(context))) {
            saveEdits(current, cropSize, scale, pan, viewModel, onSaved)
        }
    }
    if (bitmap == null) {
        ScreenMessage(error ?: "Loading…")
        return
    }
    val colorFilter = remember(edits) { ColorFilter.colorMatrix(ColorMatrix(edits.colorMatrix())) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = !saving) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = BurtonIvory)
            }
            Text("Edit", color = BurtonIvory)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::reset, enabled = !saving) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset", tint = BurtonIvory)
                }
                TextButton(
                    onClick = {
                        if (!hasWriteImageAccess(context)) {
                            writeLauncher.launch(writeImagePermissions())
                        } else {
                            saveEdits(bitmap, cropSize, scale, pan, viewModel, onSaved)
                        }
                    },
                    enabled = !saving,
                ) {
                    Text(if (saving) "Saving…" else "Save", color = BurtonSand)
                }
            }
        }
        error?.let {
            Text(it, color = BurtonMute, modifier = Modifier.padding(horizontal = 16.dp))
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current
            val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
            val cropAspect = when (aspect) {
                CropAspect.Original -> imageAspect
                CropAspect.Free -> (maxWidth / maxHeight).coerceAtLeast(0.05f)
                else -> aspect.ratio ?: imageAspect
            }
            val (boxW, boxH) = with(density) {
                val maxW = maxWidth.toPx()
                val maxH = maxHeight.toPx()
                val (w, h) = CropMath.cropSize(maxW, maxH, cropAspect)
                w.toDp() to h.toDp()
            }
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Box(
                Modifier
                    .size(boxW, boxH)
                    .clip(RectangleShape)
                    .border(1.dp, BurtonSand, RectangleShape)
                    .onSizeChanged { cropSize = it }
                    .pointerInput(bitmap, cropSize) {
                        detectTransformGestures { _, drag, zoom, _ ->
                            val nextScale = (scale * zoom).coerceIn(1f, 6f)
                            val nextPan = Offset(pan.x + drag.x, pan.y + drag.y)
                            val window = CropWindow(
                                viewWidth = cropSize.width.toFloat(),
                                viewHeight = cropSize.height.toFloat(),
                                scale = nextScale,
                                panX = nextPan.x,
                                panY = nextPan.y,
                            )
                            val clamped = CropMath.clampPan(
                                bitmap.width.toFloat(),
                                bitmap.height.toFloat(),
                                window,
                            )
                            scale = nextScale
                            pan = Offset(clamped.first, clamped.second)
                        }
                    },
            ) {
                Image(
                    bitmap = image,
                    contentDescription = photo?.title,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = pan.x
                            translationY = pan.y
                        },
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TabButton("Crop", Icons.Outlined.Crop, tab == EditTab.Crop) { tab = EditTab.Crop }
            TabButton("Adjust", Icons.Outlined.Tune, tab == EditTab.Adjust) { tab = EditTab.Adjust }
            TabButton("Filters", Icons.Outlined.PhotoFilter, tab == EditTab.Filters) { tab = EditTab.Filters }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(204.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            when (tab) {
                EditTab.Crop -> CropControls(
                    aspect = aspect,
                    onAspect = {
                        aspect = it
                        scale = 1f
                        pan = Offset.Zero
                    },
                    onRotateLeft = viewModel::rotateLeft,
                    onRotateRight = viewModel::rotateRight,
                    onFlipH = viewModel::flipHorizontal,
                    onFlipV = viewModel::flipVertical,
                )
                EditTab.Adjust -> AdjustControls(
                    brightness = edits.brightness,
                    contrast = edits.contrast,
                    saturation = edits.saturation,
                    warmth = edits.warmth,
                    onBrightness = viewModel::setBrightness,
                    onContrast = viewModel::setContrast,
                    onSaturation = viewModel::setSaturation,
                    onWarmth = viewModel::setWarmth,
                )
                EditTab.Filters -> FilterControls(
                    selected = edits.filter,
                    onSelect = viewModel::setFilter,
                )
            }
        }
        if (saving) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = BurtonSand,
                    modifier = Modifier.padding(bottom = 8.dp).size(24.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

private fun saveEdits(
    bitmap: Bitmap,
    cropSize: IntSize,
    scale: Float,
    pan: Offset,
    viewModel: EditViewModel,
    onSaved: (String) -> Unit,
) {
    val crop = if (cropSize.width < 2 || cropSize.height < 2) {
        CropWindow(bitmap.width.toFloat(), bitmap.height.toFloat())
    } else {
        CropWindow(
            viewWidth = cropSize.width.toFloat(),
            viewHeight = cropSize.height.toFloat(),
            scale = scale,
            panX = pan.x,
            panY = pan.y,
        )
    }
    viewModel.save(crop, onSaved)
}

@Composable
private fun TabButton(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) BurtonSand else BurtonMute)
        Text(label, color = if (selected) BurtonSand else BurtonMute)
    }
}

@Composable
private fun CropControls(
    aspect: CropAspect,
    onAspect: (CropAspect) -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = onRotateLeft) {
                Icon(Icons.Outlined.Rotate90DegreesCcw, contentDescription = "Rotate left", tint = BurtonIvory)
            }
            IconButton(onClick = onRotateRight) {
                Icon(Icons.Outlined.Rotate90DegreesCw, contentDescription = "Rotate right", tint = BurtonIvory)
            }
            IconButton(onClick = onFlipH) {
                Icon(Icons.Outlined.Flip, contentDescription = "Flip horizontal", tint = BurtonIvory)
            }
            IconButton(onClick = onFlipV) {
                Icon(Icons.Outlined.SwapVert, contentDescription = "Flip vertical", tint = BurtonIvory)
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CropAspect.entries.forEach { option ->
                Chip(option.label, selected = option == aspect) { onAspect(option) }
            }
        }
    }
}

@Composable
private fun AdjustControls(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    onBrightness: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onWarmth: (Float) -> Unit,
) {
    Column {
        EditSlider("Brightness", brightness, onBrightness)
        EditSlider("Contrast", contrast, onContrast)
        EditSlider("Saturation", saturation, onSaturation)
        EditSlider("Warmth", warmth, onWarmth)
    }
}

@Composable
private fun FilterControls(selected: PhotoFilter, onSelect: (PhotoFilter) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhotoFilter.entries.forEach { filter ->
            Chip(filter.label, selected = filter == selected) { onSelect(filter) }
        }
    }
}

@Composable
private fun EditSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = BurtonMute, modifier = Modifier.width(88.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = -1f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = BurtonSand,
                activeTrackColor = BurtonSand,
                inactiveTrackColor = BurtonElevated,
            ),
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) BurtonBlack else BurtonIvory,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) BurtonSand else BurtonElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
