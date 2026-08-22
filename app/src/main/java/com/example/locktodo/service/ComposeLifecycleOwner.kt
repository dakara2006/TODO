package com.example.locktodo.service

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * ComposeView는 기본적으로 Activity/Fragment가 제공하는
 * LifecycleOwner / ViewModelStoreOwner / SavedStateRegistryOwner에 의존합니다.
 * Service에서 WindowManager로 직접 View를 추가할 때는 이 세 가지를
 * 직접 구현해서 ComposeView에 연결해줘야 합니다.
 */
class ComposeLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun performRestore() {
        savedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
