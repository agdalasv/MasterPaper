package com.masterpaper.ui.screens.ringtones

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterpaper.data.model.Ringtone
import com.masterpaper.data.model.RingtoneType
import com.masterpaper.viewmodel.RingtoneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneScreen(
    viewModel: RingtoneViewModel = viewModel()
) {
    val ringtones by viewModel.ringtones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playingRingtone by viewModel.playingRingtone.collectAsState()
    val setRingtoneSuccess by viewModel.setRingtoneSuccess.collectAsState()
    val needsWriteSettingsPermission by viewModel.needsWriteSettingsPermission.collectAsState()
    
    var selectedRingtone by remember { mutableStateOf<Ringtone?>(null) }
    var selectedRingtoneType by remember { mutableStateOf(RingtoneType.RINGTONE) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
    
    LaunchedEffect(playingRingtone) {
        playingRingtone?.let { ringtone ->
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(ringtone.uri))
                prepare()
                setOnCompletionListener {
                    viewModel.stopPlaying()
                }
                start()
            }
        } ?: run {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
    
    LaunchedEffect(setRingtoneSuccess) {
        setRingtoneSuccess?.let { success ->
            val message = if (success) "Establecido correctamente!" else "Error al establecer"
            successMessage = message
            showSuccessMessage = true
            kotlinx.coroutines.delay(2500)
            showSuccessMessage = false
            viewModel.clearSuccessMessage()
            selectedRingtone = null
        }
    }
    
    LaunchedEffect(needsWriteSettingsPermission) {
        if (needsWriteSettingsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission = viewModel.checkWriteSettingsPermission()
            if (hasPermission) {
                viewModel.onWriteSettingsPermissionResult(true)
                selectedRingtone?.let { ringtone ->
                    viewModel.setAsRingtone(ringtone, selectedRingtoneType)
                }
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        } else if (ringtones.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No hay ringtones disponibles",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Agrega archivos a assets/ring",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ringtones, key = { it.id }) { ringtone ->
                    RingtoneCard(
                        ringtone = ringtone,
                        isPlaying = playingRingtone?.id == ringtone.id,
                        onPlayClick = {
                            if (playingRingtone?.id == ringtone.id) {
                                viewModel.stopPlaying()
                            } else {
                                viewModel.playRingtone(ringtone)
                            }
                        },
                        onSetClick = { selectedRingtone = ringtone }
                    )
                }
            }
        }
        
        AnimatedVisibility(
            visible = showSuccessMessage,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
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
                    Text(
                        text = successMessage,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    selectedRingtone?.let { ringtone ->
        ModalBottomSheet(
            onDismissRequest = { selectedRingtone = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            RingtoneOptionsSheet(
                ringtoneName = ringtone.name,
                onOptionSelected = { type ->
                    selectedRingtoneType = type
                    viewModel.setAsRingtone(ringtone, type)
                }
            )
        }
    }
}

@Composable
private fun RingtoneCard(
    ringtone: Ringtone,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onSetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ringtone.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tono de llamada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Detener" else "Reproducir",
                    tint = if (isPlaying) Color.White else MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = onSetClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Establecer",
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RingtoneOptionsSheet(
    ringtoneName: String,
    onOptionSelected: (RingtoneType) -> Unit
) {
    Column(
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
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = ringtoneName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        RingtoneOptionButton(
            icon = Icons.Default.MusicNote,
            text = "Tono de llamada (Ringtone)",
            onClick = { onOptionSelected(RingtoneType.RINGTONE) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        RingtoneOptionButton(
            icon = Icons.Default.Notifications,
            text = "Notificación de mensaje",
            onClick = { onOptionSelected(RingtoneType.NOTIFICATION) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        RingtoneOptionButton(
            icon = Icons.Default.Notifications,
            text = "Notificación de alarma",
            onClick = { onOptionSelected(RingtoneType.ALARM) }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun RingtoneOptionButton(
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