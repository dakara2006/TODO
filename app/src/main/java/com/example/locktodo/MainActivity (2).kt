package com.example.locktodo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.locktodo.service.VolumeKeyAccessibilityService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PermissionSetupScreen()
            }
        }
    }
}

@Composable
private fun PermissionSetupScreen() {
    val context = LocalContext.current
    var overlayGranted by remember { mutableStateOf(canDrawOverlays(context)) }
    var accessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    // 설정 화면에 다녀온 뒤 ON_RESUME 시점에 권한 상태를 다시 체크
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = canDrawOverlays(context)
                accessibilityGranted = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("잠금화면 빠른 할 일", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        Text("잠금화면에서 볼륨 업 버튼을 빠르게 두 번 누르면 할 일 추가 창이 나타납니다. 사용을 위해 아래 두 권한이 필요합니다.")
        Spacer(modifier = Modifier.height(24.dp))

        PermissionRow(
            title = "다른 앱 위에 표시 권한",
            granted = overlayGranted
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        PermissionRow(
            title = "접근성 서비스 (볼륨 키 감지)",
            granted = accessibilityGranted
        ) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(
                text = if (granted) "허용됨" else "허용 필요",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(onClick = onRequest, enabled = !granted) {
            Text(if (granted) "완료" else "설정으로 이동")
        }
    }
}

private fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = "${context.packageName}/${VolumeKeyAccessibilityService::class.java.name}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    while (splitter.hasNext()) {
        if (splitter.next().equals(expectedComponentName, ignoreCase = true)) return true
    }
    return false
}
