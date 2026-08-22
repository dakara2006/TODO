package com.example.locktodo.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 아주 단순한 SharedPreferences 기반 저장소.
 * 실제 프로젝트라면 Room/DataStore로 교체하는 것을 권장합니다.
 */
object TodoRepository {
    private const val PREFS_NAME = "lock_todo_prefs"
    private const val KEY_TODOS = "todos"

    // OverlayService(Compose)와 MainActivity(Compose)가 함께 관찰하는 상태 리스트
    val todos = mutableStateListOf<TodoItem>()

    @Synchronized
    fun load(context: Context) {
        if (todos.isNotEmpty()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TODOS, null) ?: return
        runCatching {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                todos.add(
                    TodoItem(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        createdAt = obj.getLong("createdAt")
                    )
                )
            }
        }
    }

    fun addTodo(context: Context, text: String) {
        if (text.isBlank()) return
        todos.add(0, TodoItem(text = text.trim()))
        persist(context)
    }

    fun removeTodo(context: Context, id: String) {
        todos.removeAll { it.id == id }
        persist(context)
    }

    private fun persist(context: Context) {
        val array = JSONArray()
        todos.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("createdAt", item.createdAt)
            array.put(obj)
        }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TODOS, array.toString()).apply()
    }
}
