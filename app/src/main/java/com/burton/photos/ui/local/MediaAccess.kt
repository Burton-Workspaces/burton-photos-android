package com.burton.photos.ui.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.burton.photos.ui.theme.BurtonIvory
import com.burton.photos.ui.theme.BurtonMute

fun imagePermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

fun writeImagePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT <= 28) arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    else emptyArray()

fun hasWriteImageAccess(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= 29) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    ) == PackageManager.PERMISSION_GRANTED
}

fun hasImageAccess(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= 33) {
        val images = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES,
        ) == PackageManager.PERMISSION_GRANTED
        if (images) return true
        if (Build.VERSION.SDK_INT >= 34) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return false
    }
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun MediaAccessGate(
    required: Boolean,
    onConnect: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!required) {
        content()
        return
    }
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasImageAccess(context)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted = hasImageAccess(context) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = hasImageAccess(context)
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    if (granted) {
        content()
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Photos on this phone", color = BurtonIvory)
            Spacer(Modifier.height(8.dp))
            Text(
                "Allow access to browse your camera roll, or connect a Burton Photos server.",
                color = BurtonMute,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { launcher.launch(imagePermissions()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Allow access")
            }
            if (onConnect != null) {
                TextButton(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                    Text("Connect to a server")
                }
            }
        }
    }
}
