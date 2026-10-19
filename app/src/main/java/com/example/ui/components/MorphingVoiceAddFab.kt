package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.ParsedVoiceOperation
import com.example.utils.VoiceInputManager
import com.example.utils.VoiceParserHelper
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MorphingVoiceAddFab(
    voiceInputManager: VoiceInputManager,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSaveOperations: (List<ParsedVoiceOperation>) -> Unit,
    onPermissionRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening by voiceInputManager.isListening.collectAsState()
    val voiceLevel by voiceInputManager.voiceLevel.collectAsState()
    val partialText by voiceInputManager.partialText.collectAsState()
    val recognizedOps by voiceInputManager.recognizedOperations.collectAsState()

    // Pulsing animation for active microphone
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Wave animation phase
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = if (isExpanded) 16.dp else 20.dp, vertical = 16.dp),
        contentAlignment = if (isExpanded) Alignment.BottomCenter else Alignment.BottomEnd
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it / 3 }
                )).togetherWith(
                    fadeOut(animationSpec = tween(200)) + slideOutVertically(
                        animationSpec = tween(200),
                        targetOffsetY = { it / 3 }
                    )
                )
            },
            label = "fabMorphing"
        ) { expanded ->
            if (!expanded) {
                // Collapsed State: Gradient FAB with Plus and Voice Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(12.dp, CircleShape, spotColor = NeonIndigo)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NeonIndigo, Color(0xFF8B5CF6)))
                        )
                        .border(2.dp, Brush.linearGradient(listOf(NeonCyan, NeonIndigo)), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onPermissionRequired()
                            onExpandChange(true)
                            voiceInputManager.startListening()
                        }
                        .testTag("voice_fab_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Добавить операцию голосом",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )

                    // Mini Mic Badge in corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(NeonEmerald)
                            .border(1.dp, DarkCard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                // Expanded State: Dynamic Morphing Bottom Sheet Capsule
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = DarkCard,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // 1. Header with Status & Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .scale(if (isListening) pulseScale else 1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (isListening) Brush.radialGradient(
                                                listOf(NeonRose, NeonIndigo)
                                            ) else Brush.radialGradient(
                                                listOf(DarkSurface, DarkBorder)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                                        contentDescription = null,
                                        tint = if (isListening) Color.White else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = if (isListening) "Слушаю вас..." else "Голосовой ввод",
                                        color = if (isListening) NeonEmerald else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                    Text(
                                        text = if (recognizedOps.isEmpty()) "Диктуйте подряд: «Кофе 200, такси 450»" else "Распознано позиций: ${recognizedOps.size}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Close Button ✕
                            IconButton(
                                onClick = {
                                    voiceInputManager.cancelListening()
                                    voiceInputManager.clearOperations()
                                    onExpandChange(false)
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = DarkSurface
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // 2. Recognized Operations List (Grows dynamically upwards)
                        AnimatedVisibility(
                            visible = recognizedOps.isNotEmpty(),
                            enter = fadeIn(tween(300)) + slideInVertically(spring()),
                            exit = fadeOut(tween(200))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                            ) {
                                Text(
                                    text = "ГОТОВО К ДОБАВЛЕНИЮ:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(
                                        items = recognizedOps,
                                        key = { it.id }
                                    ) { op ->
                                        ParsedOperationItemCard(
                                            operation = op,
                                            onRemove = {
                                                voiceInputManager.removeOperation(op.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Live Transcription Text (Partial Results)
                        AnimatedVisibility(
                            visible = partialText.isNotBlank(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, NeonIndigo.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = NeonRose,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = partialText,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // 4. Neon Voice Sine Wave Visualizer
                        Spacer(modifier = Modifier.height(12.dp))
                        NeonWaveVisualizer(
                            voiceLevel = voiceLevel,
                            isListening = isListening,
                            phase = wavePhase,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        )

                        // 5. Quick Test Prompts (for testing on emulators or quick demo)
                        if (recognizedOps.isEmpty() && partialText.isBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Быстрый пример для проверки:",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                QuickPromptChip("«Такси 450 и кофе 220»") {
                                    voiceInputManager.injectSimulatedPhrase("Такси 450 и кофе 220")
                                }
                                QuickPromptChip("«Зарплата 65000»") {
                                    voiceInputManager.injectSimulatedPhrase("Зарплата 65000")
                                }
                                QuickPromptChip("«Хлеб 60 молоко 110 бензин 1500»") {
                                    voiceInputManager.injectSimulatedPhrase("Хлеб 60 молоко 110 бензин 1500")
                                }
                            }
                        }

                        // 6. Action Footer: Save Button & Toggle Mic
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mic Toggle
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurface)
                                    .border(1.dp, if (isListening) NeonEmerald else DarkBorder, CircleShape)
                                    .clickable {
                                        if (isListening) {
                                            voiceInputManager.stopListening()
                                        } else {
                                            onPermissionRequired()
                                            voiceInputManager.startListening()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.PlayArrow,
                                    contentDescription = if (isListening) "Остановить" else "Продолжить слушать",
                                    tint = if (isListening) NeonRose else NeonEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Save Button (Only when operations exist)
                            if (recognizedOps.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        val opsToSave = recognizedOps.toList()
                                        voiceInputManager.clearOperations()
                                        voiceInputManager.stopListening()
                                        onSaveOperations(opsToSave)
                                        onExpandChange(false)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .background(
                                            Brush.horizontalGradient(listOf(NeonEmerald, NeonCyan)),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .testTag("save_voice_operations_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Сохранить (${recognizedOps.size})",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSurface)
                                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Говорите операции в микрофон...",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParsedOperationItemCard(
    operation: ParsedVoiceOperation,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = VoiceParserHelper.getCategoryColor(operation.category, operation.isIncome)
    val categoryIcon = VoiceParserHelper.getCategoryIcon(operation.category)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f))
                        .border(1.dp, categoryColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = operation.title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = operation.category,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = VoiceParserHelper.formatAmount(operation.amount, operation.isIncome),
                    color = if (operation.isIncome) NeonEmerald else NeonRose,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить операцию",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3 layered neon sine waves responding in real time to voice level (rmsDb)
 */
@Composable
fun NeonWaveVisualizer(
    voiceLevel: Float,
    isListening: Boolean,
    phase: Float,
    modifier: Modifier = Modifier
) {
    val animatedAmp by animateFloatAsState(
        targetValue = if (isListening) (0.2f + voiceLevel * 0.8f) else 0.05f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "animatedAmp"
    )

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface.copy(alpha = 0.5f))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val maxWaveHeight = (height / 2f - 4f) * animatedAmp

        // Wave 1: Neon Emerald (Fast, Background)
        val path1 = Path().apply {
            moveTo(0f, centerY)
            var x = 0f
            while (x <= width) {
                val progress = x / width
                val y = centerY + sin(progress * 4 * PI + phase) * maxWaveHeight * 0.8f
                lineTo(x, y.toFloat())
                x += 4f
            }
        }
        drawPath(
            path = path1,
            color = NeonEmerald.copy(alpha = 0.35f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Wave 2: Neon Rose (Offset phase, Midground)
        val path2 = Path().apply {
            moveTo(0f, centerY)
            var x = 0f
            while (x <= width) {
                val progress = x / width
                val y = centerY + sin(progress * 5 * PI - phase * 1.3f) * maxWaveHeight * 0.95f
                lineTo(x, y.toFloat())
                x += 4f
            }
        }
        drawPath(
            path = path2,
            color = NeonRose.copy(alpha = 0.45f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Wave 3: Neon Indigo -> Cyan Gradient (Foreground Sharp)
        val path3 = Path().apply {
            moveTo(0f, centerY)
            var x = 0f
            while (x <= width) {
                val progress = x / width
                val y = centerY + sin(progress * 3 * PI + phase * 0.8f) * maxWaveHeight
                lineTo(x, y.toFloat())
                x += 4f
            }
        }
        drawPath(
            path = path3,
            brush = Brush.horizontalGradient(listOf(NeonIndigo, NeonCyan, NeonEmerald)),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun QuickPromptChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, NeonIndigo.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = NeonCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
