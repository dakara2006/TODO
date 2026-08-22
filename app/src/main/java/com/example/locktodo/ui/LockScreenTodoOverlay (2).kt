package com.example.locktodo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locktodo.data.TodoItem

// iOS 느낌의 반투명 글래스모피즘 컬러
private val GlassCardBackground = Color.Black.copy(alpha = 0.45f)
private val GlassInputBackground = Color.Black.copy(alpha = 0.40f)
private val GlassRowBackground = Color.Gray.copy(alpha = 0.40f)
private val CardShape = RoundedCornerShape(22.dp)
private val InputShape = RoundedCornerShape(16.dp)
private val RowShape = RoundedCornerShape(16.dp)

/**
 * 잠금화면 중앙에 뜨는 빠른 할 일 추가 카드.
 * enter: fade + scale in (bouncy spring)
 * exit : fade + scale out (부드럽게, 바운스 없이)
 */
@Composable
fun LockScreenTodoOverlay(
    visible: Boolean,
    todos: List<TodoItem>,
    onAddTodo: (String) -> Unit,
    onRemoveTodo: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 카드 바깥 영역을 탭하면 닫히도록
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(220)) +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
            exit = fadeOut(animationSpec = tween(180)) +
                scaleOut(
                    targetScale = 0.85f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                )
        ) {
            QuickAddCard(
                todos = todos,
                onAddTodo = onAddTodo,
                onRemoveTodo = onRemoveTodo,
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    // 카드 내부 탭은 dismiss로 전파되지 않도록 별도로 소비
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* consume */ }
            )
        }
    }
}

@Composable
private fun QuickAddCard(
    todos: List<TodoItem>,
    onAddTodo: (String) -> Unit,
    onRemoveTodo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .clip(CardShape)
            .background(GlassCardBackground)
            .padding(20.dp)
    ) {
        Text(
            text = "빠른 할 일 추가",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(14.dp))

        TextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(InputShape),
            placeholder = {
                Text("할 일을 입력하세요", color = Color.White.copy(alpha = 0.6f))
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GlassInputBackground,
                unfocusedContainerColor = GlassInputBackground,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (inputText.isNotBlank()) {
                    onAddTodo(inputText)
                    inputText = ""
                }
            })
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                if (inputText.isNotBlank()) {
                    onAddTodo(inputText)
                    inputText = ""
                }
            }) {
                Text("추가", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }

        if (todos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                items(todos, key = { it.id }) { todo ->
                    TodoRow(todo = todo, onRemove = { onRemoveTodo(todo.id) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TodoRow(todo: TodoItem, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(GlassRowBackground)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = todo.text,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "삭제",
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
