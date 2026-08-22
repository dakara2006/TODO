package com.example.locktodo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.locktodo.R
import com.example.locktodo.data.TodoRepository
import com.example.locktodo.ui.LockScreenTodoOverlay

class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.example.locktodo.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.example.locktodo.action.HIDE_OVERLAY"
        private const val CHANNEL_ID = "lock_todo_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val EXIT_ANIMATION_DELAY_MS = 260L
    }

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: ComposeLifecycleOwner? = null
    private val isVisibleState = mutableStateOf(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        TodoRepository.load(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!Settings.canDrawOverlays(this)) {
            // 오버레이 권한이 없으면 아무것도 그리지 않고 즉시 종료합니다.
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_HIDE_OVERLAY -> hideOverlay()
            else -> showOverlay()
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_MIN
            )
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 실제 프로젝트에서는 전용 아이콘으로 교체하세요.
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun showOverlay() {
        if (composeView != null) {
            isVisibleState.value = true
            return
        }

        val owner = ComposeLifecycleOwner().also { it.performRestore() }
        lifecycleOwner = owner
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                // 잠금 상태에서도 이 윈도우가 화면에 표시되도록 함
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                // 필요 시 키가드 위에서 바로 보이도록(구형 API, 일부 OEM에서 동작)
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.35f
        }

        isVisibleState.value = true

        view.setContent {
            LockScreenTodoOverlay(
                visible = isVisibleState.value,
                todos = TodoRepository.todos,
                onAddTodo = { text -> TodoRepository.addTodo(applicationContext, text) },
                onRemoveTodo = { id -> TodoRepository.removeTodo(applicationContext, id) },
                onDismiss = { hideOverlay() }
            )
        }

        composeView = view
        runCatching { windowManager?.addView(view, params) }
    }

    private fun hideOverlay() {
        isVisibleState.value = false
        // exit 애니메이션(스프링)이 재생될 시간을 준 뒤 실제로 윈도우를 제거합니다.
        composeView?.postDelayed({
            removeOverlayView()
            stopSelf()
        }, EXIT_ANIMATION_DELAY_MS)
    }

    private fun removeOverlayView() {
        composeView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeView = null
        lifecycleOwner = null
    }

    override fun onDestroy() {
        removeOverlayView()
        super.onDestroy()
    }
}
