package com.masterpaper.ui.screens.preview

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.masterpaper.data.model.Wallpaper
import com.masterpaper.ui.components.ShimmerEffect
import com.masterpaper.viewmodel.PreviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    wallpaperId: String,
    onBackClick: () -> Unit,
    viewModel: PreviewViewModel = viewModel()
) {
    val wallpaper by viewModel.wallpaper.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val isSettingWallpaper by viewModel.isSettingWallpaper.collectAsState()
    val downloadSuccess by viewModel.downloadSuccess.collectAsState()
    val wallpaperSetSuccess by viewModel.wallpaperSetSuccess.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var showEffectOptions by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    var enableDepth by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(wallpaperId) {
        viewModel.loadWallpaper(wallpaperId)
    }

    LaunchedEffect(downloadSuccess) {
        downloadSuccess?.let {
            val message = if (it) "Descarga completada!" else "Error en la descarga"
            successMessage = message
            showSuccessMessage = true
            delay(2500)
            showSuccessMessage = false
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(wallpaperSetSuccess) {
        wallpaperSetSuccess?.let {
            val message = if (it) "Wallpaper aplicado correctamente!" else "Error al aplicar wallpaper"
            successMessage = message
            showSuccessMessage = true
            delay(2500)
            showSuccessMessage = false
            viewModel.clearMessages()
            showBottomSheet = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        wallpaper?.let { wp ->
            ZoomableImageWithDepthEffect(
                wallpaper = wp,
                enableDepth = enableDepth
            )
        } ?: run {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }

        IconButton(
            onClick = { showEffectOptions = !showEffectOptions },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Efectos",
                tint = if (enableDepth) MaterialTheme.colorScheme.primary else Color.White
            )
        }

        AnimatedVisibility(
            visible = showEffectOptions,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (enableDepth) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Efecto Profundidad",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = enableDepth,
                        onCheckedChange = { enableDepth = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        if (enableDepth) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Mueve el telefono para ver el efecto",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showSuccessMessage,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = successMessage,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            FilledIconButton(
                onClick = {
                    wallpaper?.let { wp ->
                        viewModel.setDownloading(true)
                        scope.launch {
                            val success = downloadImage(context, wp.url, wp.name)
                            viewModel.setDownloading(false)
                            viewModel.setDownloadSuccess(success)
                        }
                    }
                },
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                enabled = !isDownloading
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Descargar",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            FilledIconButton(
                onClick = { showBottomSheet = true },
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !isSettingWallpaper
            ) {
                if (isSettingWallpaper) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = "Establecer wallpaper",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            WallpaperOptionsSheet(
                onOptionSelected = { option ->
                    wallpaper?.let { wp ->
                        viewModel.setSettingWallpaper(true)
                        scope.launch {
                            val success = setWallpaper(context, wp.url, option)
                            viewModel.setSettingWallpaper(false)
                            viewModel.setWallpaperSetSuccess(success)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ZoomableImageWithDepthEffect(
    wallpaper: Wallpaper,
    enableDepth: Boolean
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    var gyroX by remember { mutableFloatStateOf(0f) }
    var gyroY by remember { mutableFloatStateOf(0f) }
    
    val context = LocalContext.current

    DisposableEffect(enableDepth) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        
        var isActive = true

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!isActive) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    
                    gyroX = (-orientation[1] * 40).coerceIn(-25f, 25f)
                    gyroY = (orientation[2] * 40).coerceIn(-25f, 25f)
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (enableDepth && rotationSensor != null) {
            sensorManager.registerListener(
                listener,
                rotationSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        
        onDispose {
            isActive = false
            gyroX = 0f
            gyroY = 0f
            sensorManager.unregisterListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 1.5f)
                    
                    val maxOffsetX = (size.width * (newScale - 1)) / 2
                    val maxOffsetY = (size.height * (newScale - 1)) / 2
                    
                    val newOffsetX = if (newScale > 1f) {
                        (offsetX + pan.x * 0.5f).coerceIn(-maxOffsetX, maxOffsetX)
                    } else {
                        0f
                    }
                    
                    val newOffsetY = if (newScale > 1f) {
                        (offsetY + pan.y * 0.5f).coerceIn(-maxOffsetY, maxOffsetY)
                    } else {
                        0f
                    }
                    
                    scale = newScale
                    offsetX = newOffsetX
                    offsetY = newOffsetY
                }
            }
    ) {
        if (enableDepth) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(wallpaper.url)
                    .crossfade(true)
                    .build(),
                contentDescription = "Background Depth",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.15f
                        scaleY = 1.15f
                        translationX = gyroX * 1.5f
                        translationY = gyroY * 1.5f
                    },
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ShimmerEffect()
                    }
                }
            )
        }

        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(wallpaper.url)
                .crossfade(true)
                .build(),
            contentDescription = wallpaper.name,
            contentScale = if (enableDepth) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (!enableDepth) {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
                },
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerEffect()
                }
            }
        )
    }
}

@Composable
private fun WallpaperOptionsSheet(
    onOptionSelected: (WallpaperOption) -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Establecer como",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        WallpaperOptionButton(
            icon = Icons.Default.Home,
            text = "Home Screen",
            onClick = { onOptionSelected(WallpaperOption.HOME_SCREEN) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        WallpaperOptionButton(
            icon = Icons.Default.Lock,
            text = "Lock Screen",
            onClick = { onOptionSelected(WallpaperOption.LOCK_SCREEN) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        WallpaperOptionButton(
            icon = Icons.Default.Image,
            text = "Both",
            onClick = { onOptionSelected(WallpaperOption.BOTH) }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun WallpaperOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

enum class WallpaperOption {
    HOME_SCREEN,
    LOCK_SCREEN,
    BOTH
}

private suspend fun downloadImage(context: Context, url: String, name: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = URL(url).openStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val filename = "${name}_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Masterpaper")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                val outputStream: OutputStream? = resolver.openOutputStream(it)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

private suspend fun setWallpaper(context: Context, url: String, option: WallpaperOption): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = URL(url).openStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val wallpaperManager = WallpaperManager.getInstance(context)
            
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            val scaledBitmap = scaleBitmapToScreen(bitmap, screenWidth, screenHeight)

            when (option) {
                WallpaperOption.HOME_SCREEN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(
                            scaledBitmap,
                            null,
                            true,
                            WallpaperManager.FLAG_SYSTEM
                        )
                    } else {
                        wallpaperManager.setBitmap(scaledBitmap)
                    }
                }
                WallpaperOption.LOCK_SCREEN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(
                            scaledBitmap,
                            null,
                            true,
                            WallpaperManager.FLAG_LOCK
                        )
                    } else {
                        wallpaperManager.setBitmap(scaledBitmap)
                    }
                }
                WallpaperOption.BOTH -> {
                    wallpaperManager.setBitmap(scaledBitmap)
                }
            }
            
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

private fun scaleBitmapToScreen(original: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
    val originalWidth = original.width
    val originalHeight = original.height
    
    val widthRatio = targetWidth.toFloat() / originalWidth
    val heightRatio = targetHeight.toFloat() / originalHeight
    
    val scale = maxOf(widthRatio, heightRatio)
    
    val scaledWidth = (originalWidth * scale).toInt()
    val scaledHeight = (originalHeight * scale).toInt()
    
    val scaledBitmap = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true)
    
    val xOffset = (scaledWidth - targetWidth) / 2
    val yOffset = (scaledHeight - targetHeight) / 2
    
    val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)
    canvas.drawBitmap(
        scaledBitmap,
        (-xOffset).toFloat(),
        (-yOffset).toFloat(),
        null
    )
    
    if (scaledBitmap != original) {
        scaledBitmap.recycle()
    }
    
    return result
}
