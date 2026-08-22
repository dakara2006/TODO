package com.example.locktodo.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * 볼륨 업 버튼의 "빠른 2연타"를 감지하는 접근성 서비스.
 *
 * MediaSession 방식은 (a) 화면이 잠긴 상태에서 미디어 세션이 활성화돼
 * 있어야 볼륨 키 이벤트를 가로챌 수 있고, (b) 다른 미디어 앱과 세션
 * 우선순위 경쟁이 발생할 수 있어 신뢰성이 떨어집니다.
 * AccessibilityService.onKeyEvent()는 시스템 레벨에서 키 이벤트를
 * 필터링하기 때문에 잠금화면 상태에서도 훨씬 안정적으로 동작합니다.
 */
class VolumeKeyAccessibilityService : AccessibilityService() {

    private var lastVolumeUpTimestamp = 0L
    private val doublePressThresholdMs = 400L

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val isLocked = keyguardManager.isKeyguardLocked

            val now = SystemClock.elapsedRealtime()
            val isDoublePress = (now - lastVolumeUpTimestamp) in 1..doublePressThresholdMs
            lastVolumeUpTimestamp = now

            if (isLocked && isDoublePress) {
                // 다음 입력과 헷갈리지 않도록 타임스탬프 초기화
                lastVolumeUpTimestamp = 0L

                val intent = Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_SHOW_OVERLAY
                }
                startForegroundService(intent)

                // true를 반환하면 시스템 볼륨이 실제로 변경되지 않도록 이벤트를 소비합니다.
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 키 이벤트 필터링만 사용하므로 window/view 이벤트 처리는 필요 없습니다.
    }

    override fun onInterrupt() {
        // no-op
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // serviceInfo를 통해 런타임에 플래그를 재확인/보강할 수도 있습니다.
        val info = serviceInfo
        info.flags = info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
    }
}
