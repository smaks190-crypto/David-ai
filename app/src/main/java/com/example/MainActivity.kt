package com.example

import android.content.pm.ActivityInfo
import android.graphics.Paint
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ==========================================
// 1. DATA MODELS & ENUMS
// ==========================================

enum class GameScreen {
    INTRO,
    ROOM,
    ENDING_VEGETABLE,
    ENDING_DOCTOR
}

enum class BattlePhase {
    NONE,
    MENU,
    ATTACK_BAR,
    SLASH_ANIM,
    DODGE
}

enum class PortraitType {
    NONE,
    PLAYER,
    PLAYER_DOCTOR,
    MIKHALYCH,
    MIKHALYCH_ANGRY,
    PETROVICH
}

data class DialogLine(
    val speaker: String,
    val text: String,
    val portrait: PortraitType = PortraitType.NONE,
    val options: List<String> = emptyList(),
    val onOptionSelected: ((Int) -> Unit)? = null,
    val onLineStart: (() -> Unit)? = null,
    val onLineEnd: (() -> Unit)? = null
)

class SparkParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var life: Float,
    var particleRadius: Float
)

enum class BulletType {
    PILL,
    SYRINGE,
    DROPLET
}

class Bullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var type: BulletType,
    var bulletRadius: Float = 10f
)

// ==========================================
// 2. PROCEDURAL 8-BIT RETRO AUDIO ENGINE
// ==========================================

class RetroSoundEngine {
    private val sampleRate = 22050
    var isMuted by mutableStateOf(false)

    fun playTextBlip() {
        if (isMuted) return
        playProceduralTone(pitchHz = 480.0, durationMs = 26, wave = 0, volume = 0.25f)
    }

    fun playCursorBlip() {
        if (isMuted) return
        playProceduralTone(pitchHz = 620.0, durationMs = 32, wave = 0, volume = 0.32f)
    }

    fun playSelect() {
        if (isMuted) return
        playArpeggio(listOf(523.0, 659.0, 784.0), noteDurationMs = 35, volume = 0.35f)
    }

    fun playDoorSlam() {
        if (isMuted) return
        playNoiseBurst(durationMs = 450, baseFreq = 140.0, volume = 0.9f)
    }

    fun playBattleEncounter() {
        if (isMuted) return
        playArpeggio(listOf(220.0, 311.0, 220.0, 440.0), noteDurationMs = 70, volume = 0.65f)
    }

    fun playSlash() {
        if (isMuted) return
        playSweep(startHz = 1200.0, endHz = 180.0, durationMs = 150, volume = 0.6f)
    }

    fun playBossHurt() {
        if (isMuted) return
        playNoiseBurst(durationMs = 240, baseFreq = 200.0, volume = 0.75f)
    }

    fun playPlayerHurt() {
        if (isMuted) return
        playProceduralTone(pitchHz = 110.0, durationMs = 180, wave = 0, volume = 0.8f)
    }

    fun playHeal() {
        if (isMuted) return
        playArpeggio(listOf(440.0, 554.0, 659.0, 880.0), noteDurationMs = 50, volume = 0.45f)
    }

    fun playVictoryFanfare() {
        if (isMuted) return
        playArpeggio(listOf(392.0, 523.0, 659.0, 784.0, 1046.0), noteDurationMs = 90, volume = 0.55f)
    }

    private fun playProceduralTone(pitchHz: Double, durationMs: Int, wave: Int, volume: Float) {
        Thread {
            try {
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val sample = when (wave) {
                        0 -> {
                            val period = sampleRate / pitchHz
                            if ((i % period) < (period * 0.5)) 1.0 else -1.0
                        }
                        1 -> {
                            val phase = (t * pitchHz) % 1.0
                            if (phase < 0.5) 4.0 * phase - 1.0 else 3.0 - 4.0 * phase
                        }
                        else -> sin(2.0 * PI * pitchHz * t)
                    }
                    val env = 1.0 - (i.toDouble() / numSamples)
                    buffer[i] = (sample * env * 32767.0 * volume).toInt().toShort()
                }
                writeToAudioTrack(buffer)
            } catch (_: Exception) {}
        }.start()
    }

    private fun playArpeggio(notes: List<Double>, noteDurationMs: Int, volume: Float) {
        Thread {
            try {
                val noteSamples = (sampleRate * (noteDurationMs / 1000.0)).toInt()
                val totalSamples = noteSamples * notes.size
                val buffer = ShortArray(totalSamples)
                var offset = 0
                for (pitch in notes) {
                    for (i in 0 until noteSamples) {
                        val period = sampleRate / pitch
                        val sample = if ((i % period) < (period * 0.5)) 1.0 else -1.0
                        val env = 1.0 - (i.toDouble() / noteSamples * 0.4)
                        buffer[offset + i] = (sample * env * 32767.0 * volume).toInt().toShort()
                    }
                    offset += noteSamples
                }
                writeToAudioTrack(buffer)
            } catch (_: Exception) {}
        }.start()
    }

    private fun playSweep(startHz: Double, endHz: Double, durationMs: Int, volume: Float) {
        Thread {
            try {
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                var phase = 0.0
                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val currentHz = startHz + (endHz - startHz) * progress
                    phase += (2.0 * PI * currentHz) / sampleRate
                    val sample = sin(phase)
                    val noise = (Math.random() * 2.0 - 1.0) * 0.3
                    val env = 1.0 - progress
                    buffer[i] = ((sample + noise) * env * 32767.0 * volume).toInt().toShort()
                }
                writeToAudioTrack(buffer)
            } catch (_: Exception) {}
        }.start()
    }

    private fun playNoiseBurst(durationMs: Int, baseFreq: Double, volume: Float) {
        Thread {
            try {
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                val rand = Random()
                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val noise = (rand.nextDouble() * 2.0 - 1.0)
                    val lowThud = sin(2.0 * PI * baseFreq * (i.toDouble() / sampleRate)) * (1.0 - progress)
                    val env = (1.0 - progress) * (1.0 - progress)
                    val combined = (noise * 0.6 + lowThud * 0.4) * env
                    buffer[i] = (combined * 32767.0 * volume).toInt().toShort()
                }
                writeToAudioTrack(buffer)
            } catch (_: Exception) {}
        }.start()
    }

    private fun writeToAudioTrack(buffer: ShortArray) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        Thread {
            try {
                Thread.sleep((buffer.size * 1000L / sampleRate) + 50)
                audioTrack.release()
            } catch (_: Exception) {}
        }.start()
    }
}

// ==========================================
// 3. PIXEL SPRITE PATTERNS (UNDERTALE ART)
// ==========================================

object PixelSprites {
    val PLAYER_STAND = arrayOf(
        "..########......",
        ".#HHHHHHHH#.....",
        ".#HHHHHHHH#.....",
        "..#SSSKSS#......",
        "..#SKSSKS#......",
        "..#SSSSSS#......",
        "..#WWWWWW#......",
        ".#WBBBBBBW#.....",
        ".#WWWWWWWW#.....",
        ".#WBBBBBBW#.....",
        "..#WWWWWW#......",
        "..#GG##GG#......",
        "..#GG##GG#......",
        "..#GG##GG#......",
        "..#LL##LL#......",
        "................"
    )

    val PLAYER_WALK_1 = arrayOf(
        "..########......",
        ".#HHHHHHHH#.....",
        ".#HHHHHHHH#.....",
        "..#SSSKSS#......",
        "..#SKSSKS#......",
        "..#SSSSSS#......",
        "..#WWWWWW#......",
        ".#WBBBBBBW#.....",
        ".#WWWWWWWW#.....",
        ".#WBBBBBBW#.....",
        "..#WWWWWW#......",
        "..#GGG###.......",
        "..#GGG###.......",
        "...#LL###.......",
        ".....###........",
        "................"
    )

    val PLAYER_WALK_2 = arrayOf(
        "..########......",
        ".#HHHHHHHH#.....",
        ".#HHHHHHHH#.....",
        "..#SSSKSS#......",
        "..#SKSSKS#......",
        "..#SSSSSS#......",
        "..#WWWWWW#......",
        ".#WBBBBBBW#.....",
        ".#WWWWWWWW#.....",
        ".#WBBBBBBW#.....",
        "..#WWWWWW#......",
        "...###GGG#......",
        "...###GGG#......",
        "...###LL#.......",
        "....###.........",
        "................"
    )

    val PLAYER_DOCTOR = arrayOf(
        "..#CCCCCCC#.....",
        "..#C+R+C+C#.....",
        ".#HHHHHHHH#.....",
        "..#SSSKSS#......",
        "..#SKSSKS#......",
        "..#SSSSSS#......",
        "..#WWWWWW#......",
        ".#WWYYYYWW#.....",
        ".#WWYYYYWW#.....",
        ".#WWWWWWWW#.....",
        "..#WWWWWW#......",
        "..#DD##DD#......",
        "..#DD##DD#......",
        "..#DD##DD#......",
        "..#KK##KK#......",
        "................"
    )

    val MIKHALYCH_STAND = arrayOf(
        "...########.....",
        "..#CCCCCCCC#....",
        "..#CCC+RCCC#....",
        "..#SSSSSSSS#....",
        "..#SKSSSKSS#....",
        "..#SBSSSSBS#....",
        "..#SSSSSSSS#....",
        ".#WWWWWWWWWW#...",
        ".#WWWWWWWWWW#...",
        ".#WW##WW##WW#...",
        ".#WWKYWWWWWW#...",
        "..#WWWWWWWW#....",
        "..#PPPPPPPP#....",
        "..#PP####PP#....",
        "..#BB####BB#....",
        "................"
    )

    val MIKHALYCH_TELNYASHKA = arrayOf(
        "...########.....",
        "..#SSSSSSSS#....",
        "..#SKSSSKSS#....",
        "..#SBSSSSBS#....",
        "..#SSSSSSSS#....",
        ".#WWWWWWWWWW#...",
        ".#UUUUUUUUUU#...",
        ".#WWWWWWWWWW#...",
        ".#UUUUUUUUUU#...",
        ".#WWWWWWWWWW#...",
        "..#PPPPPPPP#....",
        "..#PP####PP#....",
        "..#PP####PP#....",
        "..#BB####BB#....",
        "................"
    )

    val PETROVICH_STAND = arrayOf(
        "....######......",
        "...#HHHHHH#.....",
        "...#SSGGSS#.....",
        "...#SSMMSS#.....",
        "...#TTTTTT#.....",
        "..#TTTTTTTT#....",
        "..#TT#YY#TT#....",
        "..#TTTTTTTT#....",
        "...#TTTTTT#.....",
        "...#DD##DD#.....",
        "...#DD##DD#.....",
        "...#KK##KK#.....",
        "................",
        "................",
        "................",
        "................"
    )

    val BOSS_MIKHALYCH = arrayOf(
        "........############........",
        "......##CCCCCCCCCCCC##......",
        ".....#CCCCCCCCCCCCCCCC#.....",
        ".....#CCCCCC+RR+CCCCCC#.....",
        ".....#CCCCCC+RR+CCCCCC#.....",
        "......#SSSSSSSSSSSSSS#......",
        ".....#SKKKSSSSSSSSKKKS#.....",
        ".....#SKKKSSSSSSSSKKKS#.....",
        ".....#SSSSBBSSSSBBSSSS#.....",
        ".....#SSSSBBBBBBBBSSSS#.....",
        "......#SSSSSSBBSSSSSS#......",
        "....##WWWWWWWWWWWWWWWW##....",
        "...#WWWWWWWWWWWWWWWWWWWW#...",
        "..#WWWWWWWWWWWWWWWWWWWWWW#..",
        ".#WW##WWWWWWWWWWWWWWWW##WW#.",
        "#WW#..#WWWWWWWWWWWWWW#..#WW#",
        "#YY#..#WWWWWWWWWWWWWW#..#YY#",
        "#YY#..#WWWWWWWWWWWWWW#..#YY#",
        "......#WWWWWWWWWWWWWW#......",
        ".......#PPPPPPPPPPPP#.......",
        ".......#PPPPPPPPPPPP#.......",
        ".......#PPPP####PPPP#.......",
        ".......#BBBB####BBBB#.......",
        "............................"
    )

    val PALETTE = mapOf(
        '#' to Color(0xFF111111),
        'H' to Color(0xFF3E2723),
        'S' to Color(0xFFFFCC80),
        'K' to Color(0xFF212121),
        'W' to Color(0xFFEEEEEE),
        'B' to Color(0xFF8D6E63),
        'G' to Color(0xFF757575),
        'L' to Color(0xFFBDBDBD),
        'C' to Color(0xFFFAFAFA),
        'R' to Color(0xFFD32F2F),
        'Y' to Color(0xFFFFD54F),
        'D' to Color(0xFF455A64),
        'P' to Color(0xFF37474F),
        'U' to Color(0xFF1976D2),
        'T' to Color(0xFF80CBC4),
        'M' to Color(0xFFE0F2F1)
    )
}

fun DrawScope.drawPixelSprite(
    pattern: Array<String>,
    palette: Map<Char, Color>,
    x: Float,
    y: Float,
    pixelSize: Float,
    flipX: Boolean = false
) {
    val rows = pattern.size
    for (r in 0 until rows) {
        val line = pattern[r]
        val cols = line.length
        for (c in 0 until cols) {
            val char = if (flipX) line[cols - 1 - c] else line[c]
            if (char != '.') {
                val color = palette[char] ?: Color.White
                drawRect(
                    color = color,
                    topLeft = Offset(x + c * pixelSize, y + r * pixelSize),
                    size = Size(pixelSize, pixelSize)
                )
            }
        }
    }
}

fun DrawScope.drawSoul(x: Float, y: Float, size: Float, color: Color = Color(0xFFFF1744)) {
    val p = size / 8f
    val heartPattern = arrayOf(
        ".##.##.",
        "#######",
        "#######",
        "#######",
        ".#####.",
        "..###..",
        "...#..."
    )
    for (r in heartPattern.indices) {
        for (c in heartPattern[r].indices) {
            if (heartPattern[r][c] == '#') {
                drawRect(
                    color = color,
                    topLeft = Offset(x - size / 2f + c * p, y - size / 2f + r * p),
                    size = Size(p, p)
                )
            }
        }
    }
}

// ==========================================
// 4. MAIN ACTIVITY & ENTRY POINT
// ==========================================

class MainActivity : ComponentActivity() {
    private val soundEngine = RetroSoundEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()

        setContent {
            DurotaleGame(soundEngine)
        }
    }
}

// ==========================================
// 5. GAME COMPOSABLE & ARCHITECTURE
// ==========================================

@Composable
fun DurotaleGame(soundEngine: RetroSoundEngine) {
    var gameScreen by remember { mutableStateOf(GameScreen.INTRO) }
    var isInBattle by remember { mutableStateOf(false) }
    var battlePhase by remember { mutableStateOf(BattlePhase.NONE) }

    var playerHp by remember { mutableIntStateOf(20) }
    val playerMaxHp = 20
    var playerLv by remember { mutableIntStateOf(1) }
    var endingsFound by remember { mutableIntStateOf(0) }
    var doctorEndingUnlocked by remember { mutableStateOf(false) }
    var vegetableEndingUnlocked by remember { mutableStateOf(false) }

    var playerX by remember { mutableFloatStateOf(320f) }
    var playerY by remember { mutableFloatStateOf(340f) }
    var playerFacingLeft by remember { mutableStateOf(false) }
    var walkFrame by remember { mutableIntStateOf(0) }
    var isPlayerDoctor by remember { mutableStateOf(false) }

    var orderly1X by remember { mutableFloatStateOf(280f) }
    var orderly1Y by remember { mutableFloatStateOf(340f) }
    var orderly2X by remember { mutableFloatStateOf(360f) }
    var orderly2Y by remember { mutableFloatStateOf(340f) }

    var doorOpen by remember { mutableStateOf(true) }
    var screenShake by remember { mutableFloatStateOf(0f) }
    val particles = remember { mutableStateListOf<SparkParticle>() }

    val cipherSequence = remember { mutableStateListOf<String>() }
    val targetCipher = listOf("Тарабанить", "Орать", "Тарабанить", "Тарабанить", "Орать")

    var bossHp by remember { mutableIntStateOf(100) }
    val bossMaxHp = 100
    var bossShake by remember { mutableFloatStateOf(0f) }
    var bossDefReduced by remember { mutableIntStateOf(0) }

    var attackBarProgress by remember { mutableFloatStateOf(0f) }
    var attackBarDirection by remember { mutableFloatStateOf(1f) }
    var slashAnimProgress by remember { mutableFloatStateOf(0f) }
    var lastDamageDealt by remember { mutableIntStateOf(0) }
    var isCriticalHit by remember { mutableStateOf(false) }

    var soulX by remember { mutableFloatStateOf(320f) }
    var soulY by remember { mutableFloatStateOf(260f) }
    var soulInvincibleTimer by remember { mutableFloatStateOf(0f) }
    val bullets = remember { mutableStateListOf<Bullet>() }
    var dodgeTimer by remember { mutableFloatStateOf(0f) }

    var currentDialogue by remember { mutableStateOf<DialogLine?>(null) }
    val cutsceneQueue = remember { mutableStateListOf<DialogLine>() }

    var typedCharacters by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableIntStateOf(0) }
    var isTypingComplete by remember { mutableStateOf(false) }

    var joystickVector by remember { mutableStateOf(Offset.Zero) }
    var lastMenuJoystickMoveTime by remember { mutableLongStateOf(0L) }
    var showHelpModal by remember { mutableStateOf(false) }

    val battleMenuItems = listOf("⚔ БИТВА", "🔊 ДЕЙСТВИЕ", "💊 ВЕЩИ", "✖ ПОЩАДА")
    val inventoryItems = remember { mutableStateListOf("Аскорбинка (+10)", "Мятный леденец (+15)", "Чайный гриб (+20)") }

    fun startDialog(dialog: DialogLine) {
        currentDialogue = dialog
        typedCharacters = 0
        isTypingComplete = false
        selectedOptionIndex = 0
        dialog.onLineStart?.invoke()
    }

    fun showNextCutsceneLine() {
        if (cutsceneQueue.isNotEmpty()) {
            val nextLine = cutsceneQueue.removeAt(0)
            startDialog(nextLine)
        } else {
            currentDialogue = null
        }
    }

    fun playCutscene(lines: List<DialogLine>) {
        cutsceneQueue.clear()
        cutsceneQueue.addAll(lines)
        showNextCutsceneLine()
    }

    fun advanceDialog() {
        val diag = currentDialogue ?: return
        if (!isTypingComplete) {
            typedCharacters = diag.text.length
            isTypingComplete = true
            soundEngine.playSelect()
            return
        }

        if (diag.options.isNotEmpty()) {
            soundEngine.playSelect()
            diag.onOptionSelected?.invoke(selectedOptionIndex)
        } else {
            soundEngine.playSelect()
            diag.onLineEnd?.invoke()
            showNextCutsceneLine()
        }
    }

    fun initiateBattle() {
        isInBattle = true
        battlePhase = BattlePhase.MENU
        selectedOptionIndex = 0
        soulX = 320f
        soulY = 265f
        soundEngine.playBattleEncounter()
        screenShake = 15f
        startDialog(
            DialogLine(
                speaker = "Михалыч",
                portrait = PortraitType.MIKHALYCH_ANGRY,
                text = "* Михалыч грозно щелкает ногтем по гигантскому шприцу с галоперидолом!",
                options = battleMenuItems,
                onOptionSelected = { idx ->
                    when (idx) {
                        0 -> {
                            battlePhase = BattlePhase.ATTACK_BAR
                            attackBarProgress = 0f
                            attackBarDirection = 1f
                            currentDialogue = null
                            soundEngine.playSelect()
                        }
                        1 -> {
                            startDialog(
                                DialogLine(
                                    speaker = "ДЕЙСТВИЕ",
                                    portrait = PortraitType.NONE,
                                    text = "Выберите психологическое воздействие на санитара:",
                                    options = listOf(
                                        "1. Оценить противника",
                                        "2. Угрожать жалобой в Минздрав",
                                        "3. Симулировать сердечный приступ",
                                        "4. Предложить взятку 500 руб"
                                    ),
                                    onOptionSelected = { actIdx ->
                                        when (actIdx) {
                                            0 -> {
                                                startDialog(
                                                    DialogLine(
                                                        speaker = "Инфо",
                                                        text = "* МИХАЛЫЧ — АТК 10, ЗАЩ 12. Старший санитар. Слабость: гордость и плохая реакция.",
                                                        onLineEnd = {
                                                            battlePhase = BattlePhase.DODGE
                                                            dodgeTimer = 4.5f
                                                        }
                                                    )
                                                )
                                            }
                                            1 -> {
                                                bossDefReduced += 5
                                                startDialog(
                                                    DialogLine(
                                                        speaker = "Вы",
                                                        portrait = PortraitType.PLAYER,
                                                        text = "* Вы грозитесь жалобой! Михалыч нервно дергает глазом (-5 к защите босса).",
                                                        onLineEnd = {
                                                            battlePhase = BattlePhase.DODGE
                                                            dodgeTimer = 4.5f
                                                        }
                                                    )
                                                )
                                            }
                                            2 -> {
                                                startDialog(
                                                    DialogLine(
                                                        speaker = "Вы",
                                                        portrait = PortraitType.PLAYER,
                                                        text = "* Вы хватаетесь за сердце и падаете! Михалыч в растерянности опускает шприц.",
                                                        onLineEnd = {
                                                            battlePhase = BattlePhase.DODGE
                                                            dodgeTimer = 4.5f
                                                        }
                                                    )
                                                )
                                            }
                                            3 -> {
                                                startDialog(
                                                    DialogLine(
                                                        speaker = "Михалыч",
                                                        portrait = PortraitType.MIKHALYCH_ANGRY,
                                                        text = "* Михалыч: «Ты меня за кого держишь?! Сейчас удвою дозировку!»",
                                                        onLineEnd = {
                                                            battlePhase = BattlePhase.DODGE
                                                            dodgeTimer = 4.5f
                                                        }
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            )
                        }
                        2 -> {
                            if (inventoryItems.isEmpty()) {
                                startDialog(
                                    DialogLine(
                                        speaker = "Вещи",
                                        text = "* В карманах смирительной рубашки пусто!",
                                        onLineEnd = {
                                            battlePhase = BattlePhase.DODGE
                                            dodgeTimer = 4.5f
                                        }
                                    )
                                )
                            } else {
                                startDialog(
                                    DialogLine(
                                        speaker = "Вещи",
                                        text = "Использовать предмет:",
                                        options = inventoryItems.toList(),
                                        onOptionSelected = { itemIdx ->
                                            val item = inventoryItems[itemIdx]
                                            val healAmt = when {
                                                item.contains("10") -> 10
                                                item.contains("15") -> 15
                                                else -> 20
                                            }
                                            playerHp = min(playerMaxHp, playerHp + healAmt)
                                            soundEngine.playHeal()
                                            inventoryItems.removeAt(itemIdx)
                                            startDialog(
                                                DialogLine(
                                                    speaker = "Лечение",
                                                    text = "* Вы съели $item! Восстановлено $healAmt ОЗ.",
                                                    onLineEnd = {
                                                        battlePhase = BattlePhase.DODGE
                                                        dodgeTimer = 4.5f
                                                    }
                                                )
                                            )
                                        }
                                    )
                                )
                            }
                        }
                        3 -> {
                            startDialog(
                                DialogLine(
                                    speaker = "Михалыч",
                                    portrait = PortraitType.MIKHALYCH,
                                    text = "* Михалыч ухмыляется: «Пощада? В нашем отделении пощады нет! Только строгий постельный режим!»",
                                    onLineEnd = {
                                        battlePhase = BattlePhase.DODGE
                                        dodgeTimer = 4.5f
                                    }
                                )
                            )
                        }
                    }
                }
            )
        )
    }

    fun triggerDoctorEnding() {
        isInBattle = false
        battlePhase = BattlePhase.NONE
        isPlayerDoctor = true
        doctorEndingUnlocked = true
        endingsFound = (if (vegetableEndingUnlocked) 1 else 0) + (if (doctorEndingUnlocked) 1 else 0)
        soundEngine.playVictoryFanfare()

        playCutscene(
            listOf(
                DialogLine(
                    speaker = "Михалыч",
                    portrait = PortraitType.MIKHALYCH_ANGRY,
                    text = "«Ох... Моя поясница... Всё, сдаюсь, не бей галоперидолом...»"
                ),
                DialogLine(
                    speaker = "Вы",
                    portrait = PortraitType.PLAYER_DOCTOR,
                    text = "* Вы снимаете с Михалыча белый халат и шапочку с красным крестом, а его самого в одной тельняшке заталкиваете в палату!",
                    onLineEnd = {
                        doorOpen = false
                        soundEngine.playDoorSlam()
                        screenShake = 18f
                        gameScreen = GameScreen.ENDING_DOCTOR
                    }
                ),
                DialogLine(
                    speaker = "Санитар Петрович",
                    portrait = PortraitType.PETROVICH,
                    text = "«Здравствуйте, доктор! Слышу шум... Этот новенький буйный в палате №6 совсем с катушек съехал, кричит, что он санитар!»",
                    options = listOf(
                        "1. В психиатрии кто первый надел халат — тот и врач! Удвойте ему дозу.",
                        "2. Отставить шприцы, я сам выписываюсь через парадный вход!"
                    ),
                    onOptionSelected = { choice ->
                        if (choice == 0) {
                            startDialog(
                                DialogLine(
                                    speaker = "Санитар Петрович",
                                    portrait = PortraitType.PETROVICH,
                                    text = "«Гениально, доктор! Мудрейшее решение. Держите ключи от парадной двери!»"
                                )
                            )
                        } else {
                            startDialog(
                                DialogLine(
                                    speaker = "Санитар Петрович",
                                    portrait = PortraitType.PETROVICH,
                                    text = "«Ха-ха, доктор, у вас отменное чувство юмора! Счастливого пути домой!»"
                                )
                            )
                        }
                    }
                )
            )
        )
    }

    fun startIntroCutscene() {
        gameScreen = GameScreen.INTRO
        isInBattle = false
        battlePhase = BattlePhase.NONE
        doorOpen = true
        playerX = 320f
        playerY = 340f
        orderly1X = 280f
        orderly1Y = 340f
        orderly2X = 360f
        orderly2Y = 340f
        cipherSequence.clear()
        bossHp = 100
        playerHp = 20
        isPlayerDoctor = false

        playCutscene(
            listOf(
                DialogLine(
                    speaker = "Санитар Михалыч",
                    portrait = PortraitType.MIKHALYCH,
                    text = "Давай, шевели копытами, голубчик! Палата номер 6, мягче не бывает!"
                ),
                DialogLine(
                    speaker = "Санитар Петрович",
                    portrait = PortraitType.PETROVICH,
                    text = "Осторожнее держи его, Михалыч... Взгляд у него больно хитрый."
                ),
                DialogLine(
                    speaker = "Санитар Михалыч",
                    portrait = PortraitType.MIKHALYCH,
                    text = "Ничего, до утра посидит в одиночке — как шёлковый станет! Толкай его на середину!",
                    onLineEnd = {
                        playerY = 165f
                    }
                ),
                DialogLine(
                    speaker = "Дверной засов",
                    text = "* ЛЯЗГ! Стальная дверь захлопнулась на тяжелый засов...",
                    onLineStart = {
                        doorOpen = false
                        screenShake = 22f
                        soundEngine.playDoorSlam()
                        for (i in 0..25) {
                            particles.add(
                                SparkParticle(
                                    x = 320f + (Math.random().toFloat() * 40f - 20f),
                                    y = 280f + (Math.random().toFloat() * 20f - 10f),
                                    vx = (Math.random().toFloat() * 12f - 6f),
                                    vy = -(Math.random().toFloat() * 10f + 2f),
                                    color = if (i % 2 == 0) Color(0xFFFFD54F) else Color(0xFFFF7043),
                                    life = 1f,
                                    particleRadius = 3f + (Math.random().toFloat() * 3f)
                                )
                            )
                        }
                    }
                ),
                DialogLine(
                    speaker = "Вы",
                    portrait = PortraitType.PLAYER,
                    text = "* Вы остались один в мягкой одиночной палате. Нужно найти способ выбраться на свободу...",
                    onLineEnd = {
                        gameScreen = GameScreen.ROOM
                    }
                )
            )
        )
    }

    fun triggerMikhalychEntryTrick() {
        playCutscene(
            listOf(
                DialogLine(
                    speaker = "Вы",
                    portrait = PortraitType.PLAYER,
                    text = "«Михалыч, беда! Вы выронили новенький сенсорный телефон у меня под подушкой!»"
                ),
                DialogLine(
                    speaker = "Голос за дверью",
                    portrait = PortraitType.MIKHALYCH,
                    text = "«Чего?! Мой новенький смартфон в кредит?! А ну отойди от двери в угол, сейчас проверю!»",
                    onLineEnd = {
                        doorOpen = true
                        soundEngine.playDoorSlam()
                        screenShake = 12f
                        playerY = 150f
                        orderly1X = 320f
                        orderly1Y = 300f
                    }
                ),
                DialogLine(
                    speaker = "Михалыч",
                    portrait = PortraitType.MIKHALYCH,
                    text = "* Санитар заходит в палату, шарит по углам... Но телефона нигде нет!"
                ),
                DialogLine(
                    speaker = "Михалыч",
                    portrait = PortraitType.MIKHALYCH_ANGRY,
                    text = "* Хлопает себя по карману... Телефон на месте! «Ах ты симулянт! Обмануть меня вздумал?!»",
                    onLineEnd = {
                        initiateBattle()
                    }
                )
            )
        )
    }

    fun checkCipherSequence() {
        while (cipherSequence.size > 5) {
            cipherSequence.removeAt(0)
        }
        if (cipherSequence.size == 5 && cipherSequence.toList() == targetCipher) {
            gameScreen = GameScreen.ENDING_VEGETABLE
            vegetableEndingUnlocked = true
            endingsFound = (if (vegetableEndingUnlocked) 1 else 0) + (if (doctorEndingUnlocked) 1 else 0)
            soundEngine.playBossHurt()
            screenShake = 30f
            currentDialogue = null
            cutsceneQueue.clear()
        }
    }

    fun handleZConfirm() {
        if (battlePhase == BattlePhase.ATTACK_BAR) {
            val dist = abs(attackBarProgress - 0.5f)
            val baseDmg = when {
                dist < 0.08f -> {
                    isCriticalHit = true
                    45
                }
                dist < 0.22f -> {
                    isCriticalHit = false
                    30
                }
                else -> {
                    isCriticalHit = false
                    15
                }
            }
            val totalDmg = baseDmg + bossDefReduced
            lastDamageDealt = totalDmg
            bossHp = max(0, bossHp - totalDmg)
            battlePhase = BattlePhase.SLASH_ANIM
            slashAnimProgress = 0f
            bossShake = 20f
            soundEngine.playSlash()
            return
        }

        if (currentDialogue != null) {
            advanceDialog()
            return
        }

        if (gameScreen == GameScreen.ROOM && !isInBattle) {
            if (playerX in 260f..380f && playerY > 230f) {
                startDialog(
                    DialogLine(
                        speaker = "Стальная дверь палаты",
                        text = "Тяжелая дверь на засов с бронеглазком. Что сделать?",
                        options = listOf(
                            "1. Постучать (обмануть про телефон)",
                            "2. Тарабанить кулаками [Кулак]",
                            "3. Орать во все горло [Рот]",
                            "4. Заглянуть в бронеглазок",
                            "5. Отойти"
                        ),
                        onOptionSelected = { idx ->
                            when (idx) {
                                0 -> triggerMikhalychEntryTrick()
                                1 -> {
                                    cipherSequence.add("Тарабанить")
                                    soundEngine.playDoorSlam()
                                    screenShake = 6f
                                    startDialog(
                                        DialogLine(
                                            speaker = "Дверь",
                                            text = "* БУМ-БУМ-БУМ! Вы яростно колотите кулаками по стальной двери!",
                                            onLineEnd = { checkCipherSequence() }
                                        )
                                    )
                                }
                                2 -> {
                                    cipherSequence.add("Орать")
                                    soundEngine.playPlayerHurt()
                                    startDialog(
                                        DialogLine(
                                            speaker = "Вы",
                                            portrait = PortraitType.PLAYER,
                                            text = "«А-А-А-А-А! ВЫПУСТИТЕ МЕНЯ ОТСЮДА! Я ЗДОРОВ!»",
                                            onLineEnd = { checkCipherSequence() }
                                        )
                                    )
                                }
                                3 -> {
                                    startDialog(
                                        DialogLine(
                                            speaker = "Глазок",
                                            text = "* В тусклом коридоре на стуле дремлет санитар Михалыч, положив ноги на тумбочку."
                                        )
                                    )
                                }
                                4 -> currentDialogue = null
                            }
                        }
                    )
                )
                return
            }

            if (playerX < 240f && playerY < 200f) {
                startDialog(
                    DialogLine(
                        speaker = "Подушка в углу",
                        text = "* Мягкая больничная подушка. Под ней спрятана пачка аскорбинок и мятный леденец! (+ОЗ в бою)",
                        onLineEnd = {
                            if (!inventoryItems.contains("Чайный гриб (+20)")) {
                                inventoryItems.add("Чайный гриб (+20)")
                            }
                        }
                    )
                )
                return
            }

            if (playerX > 430f && playerY < 210f) {
                startDialog(
                    DialogLine(
                        speaker = "Стена палаты",
                        text = "* На мягкой обшивке ногтем нацарапан шифр побега:\n'I: Кулак, II: Рот, III: Кулак, IV: Кулак, V: Рот'\nПохоже, эта комбинация у двери может вывести охрану из себя..."
                    )
                )
                return
            }
        }
    }

    fun handleXAction() {
        val diag = currentDialogue
        if (diag != null && !isTypingComplete) {
            typedCharacters = diag.text.length
            isTypingComplete = true
            soundEngine.playSelect()
        }
    }

    LaunchedEffect(Unit) {
        startIntroCutscene()
    }

    LaunchedEffect(currentDialogue) {
        val diag = currentDialogue ?: return@LaunchedEffect
        typedCharacters = 0
        isTypingComplete = false
        while (typedCharacters < diag.text.length) {
            delay(24)
            typedCharacters++
            if (diag.text[typedCharacters - 1] != ' ') {
                soundEngine.playTextBlip()
            }
        }
        isTypingComplete = true
    }

    LaunchedEffect(gameScreen, isInBattle, battlePhase) {
        var lastTime = System.nanoTime()
        val random = Random()

        while (true) {
            withFrameNanos { now ->
                val dt = min(0.05f, (now - lastTime) / 1_000_000_000f)
                lastTime = now

                if (screenShake > 0f) {
                    screenShake = max(0f, screenShake - dt * 28f)
                }
                if (bossShake > 0f) {
                    bossShake = max(0f, bossShake - dt * 24f)
                }

                var pIdx = 0
                while (pIdx < particles.size) {
                    val p = particles[pIdx]
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 0.35f
                    p.life -= dt * 2f
                    if (p.life <= 0f) {
                        particles.removeAt(pIdx)
                    } else {
                        pIdx++
                    }
                }

                if (gameScreen == GameScreen.INTRO) {
                    if (orderly1Y > 210f) {
                        orderly1Y -= dt * 65f
                        orderly2Y -= dt * 65f
                        playerY -= dt * 65f
                        walkFrame = ((now / 150_000_000L) % 4).toInt()
                    }
                }

                if (gameScreen == GameScreen.ROOM && !isInBattle && currentDialogue == null) {
                    val speed = 140f * dt
                    val vx = joystickVector.x
                    val vy = joystickVector.y
                    if (abs(vx) > 0.15f || abs(vy) > 0.15f) {
                        playerX = (playerX + vx * speed).coerceIn(120f, 520f)
                        playerY = (playerY + vy * speed).coerceIn(130f, 290f)
                        if (vx < -0.1f) playerFacingLeft = true
                        if (vx > 0.1f) playerFacingLeft = false
                        walkFrame = ((now / 140_000_000L) % 4).toInt()
                    } else {
                        walkFrame = 0
                    }
                }

                if (battlePhase == BattlePhase.ATTACK_BAR) {
                    attackBarProgress += attackBarDirection * dt * 1.35f
                    if (attackBarProgress >= 1f) {
                        attackBarProgress = 1f
                        attackBarDirection = -1f
                    } else if (attackBarProgress <= 0f) {
                        attackBarProgress = 0f
                        attackBarDirection = 1f
                    }
                }

                if (battlePhase == BattlePhase.SLASH_ANIM) {
                    slashAnimProgress += dt * 1.5f
                    if (slashAnimProgress >= 1f) {
                        slashAnimProgress = 1f
                        if (bossHp <= 0) {
                            triggerDoctorEnding()
                        } else {
                            battlePhase = BattlePhase.DODGE
                            dodgeTimer = 4.5f
                            bullets.clear()
                            soulX = 320f
                            soulY = 265f
                            soundEngine.playBossHurt()
                        }
                    }
                }

                if (battlePhase == BattlePhase.DODGE) {
                    dodgeTimer -= dt
                    if (soulInvincibleTimer > 0f) {
                        soulInvincibleTimer -= dt
                    }

                    val soulSpeed = 160f * dt
                    soulX = (soulX + joystickVector.x * soulSpeed).coerceIn(230f, 410f)
                    soulY = (soulY + joystickVector.y * soulSpeed).coerceIn(210f, 320f)

                    if (random.nextFloat() < dt * 7f) {
                        val side = random.nextInt(3)
                        val bulletType = when (random.nextInt(3)) {
                            0 -> BulletType.PILL
                            1 -> BulletType.SYRINGE
                            else -> BulletType.DROPLET
                        }
                        when (side) {
                            0 -> {
                                bullets.add(
                                    Bullet(
                                        x = 240f + random.nextFloat() * 160f,
                                        y = 200f,
                                        vx = (random.nextFloat() - 0.5f) * 60f,
                                        vy = 90f + random.nextFloat() * 50f,
                                        type = bulletType,
                                        bulletRadius = if (bulletType == BulletType.SYRINGE) 14f else 8f
                                    )
                                )
                            }
                            1 -> {
                                bullets.add(
                                    Bullet(
                                        x = 220f,
                                        y = 215f + random.nextFloat() * 95f,
                                        vx = 80f + random.nextFloat() * 60f,
                                        vy = (random.nextFloat() - 0.5f) * 40f,
                                        type = bulletType,
                                        bulletRadius = 9f
                                    )
                                )
                            }
                            else -> {
                                bullets.add(
                                    Bullet(
                                        x = 420f,
                                        y = 215f + random.nextFloat() * 95f,
                                        vx = -(80f + random.nextFloat() * 60f),
                                        vy = (random.nextFloat() - 0.5f) * 40f,
                                        type = bulletType,
                                        bulletRadius = 9f
                                    )
                                )
                            }
                        }
                    }

                    var bIdx = 0
                    while (bIdx < bullets.size) {
                        val b = bullets[bIdx]
                        b.x += b.vx * dt
                        b.y += b.vy * dt

                        val dist = sqrt((b.x - soulX) * (b.x - soulX) + (b.y - soulY) * (b.y - soulY))
                        if (dist < (b.bulletRadius + 6f) && soulInvincibleTimer <= 0f) {
                            playerHp = max(1, playerHp - 4)
                            soulInvincibleTimer = 0.65f
                            soundEngine.playPlayerHurt()
                            screenShake = 12f
                        }

                        if (b.x < 200f || b.x > 440f || b.y < 180f || b.y > 340f) {
                            bullets.removeAt(bIdx)
                        } else {
                            bIdx++
                        }
                    }

                    if (dodgeTimer <= 0f) {
                        bullets.clear()
                        battlePhase = BattlePhase.MENU
                        startDialog(
                            DialogLine(
                                speaker = "Михалыч",
                                portrait = PortraitType.MIKHALYCH,
                                text = "* Михалыч вытирает пот со лба. Что предпринять?",
                                options = battleMenuItems,
                                onOptionSelected = { idx ->
                                    when (idx) {
                                        0 -> {
                                            battlePhase = BattlePhase.ATTACK_BAR
                                            attackBarProgress = 0f
                                            attackBarDirection = 1f
                                            currentDialogue = null
                                            soundEngine.playSelect()
                                        }
                                        1 -> {
                                            startDialog(
                                                DialogLine(
                                                    speaker = "Вы",
                                                    portrait = PortraitType.PLAYER,
                                                    text = "* Вы продолжаете психологическое давление на санитара!",
                                                    onLineEnd = {
                                                        battlePhase = BattlePhase.DODGE
                                                        dodgeTimer = 4.5f
                                                    }
                                                )
                                            )
                                        }
                                        2 -> {
                                            if (inventoryItems.isNotEmpty()) {
                                                playerHp = min(playerMaxHp, playerHp + 12)
                                                soundEngine.playHeal()
                                                inventoryItems.removeAt(0)
                                                startDialog(
                                                    DialogLine(
                                                        speaker = "Лечение",
                                                        text = "* Вы восстановили 12 ОЗ!",
                                                        onLineEnd = {
                                                            battlePhase = BattlePhase.DODGE
                                                            dodgeTimer = 4.5f
                                                        }
                                                    )
                                                )
                                            }
                                        }
                                        3 -> {
                                            startDialog(
                                                DialogLine(
                                                    speaker = "Михалыч",
                                                    text = "* Михалыч не согласен на перемирие!",
                                                    onLineEnd = {
                                                        battlePhase = BattlePhase.DODGE
                                                        dodgeTimer = 4.5f
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(joystickVector.y, currentDialogue) {
        val diag = currentDialogue ?: return@LaunchedEffect
        if (diag.options.isEmpty()) return@LaunchedEffect

        val now = System.currentTimeMillis()
        if (now - lastMenuJoystickMoveTime > 220L) {
            if (joystickVector.y < -0.5f) {
                selectedOptionIndex = (selectedOptionIndex - 1 + diag.options.size) % diag.options.size
                soundEngine.playCursorBlip()
                lastMenuJoystickMoveTime = now
            } else if (joystickVector.y > 0.5f) {
                selectedOptionIndex = (selectedOptionIndex + 1) % diag.options.size
                soundEngine.playCursorBlip()
                lastMenuJoystickMoveTime = now
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(6.dp)
    ) {
        // COLUMN 1: LEFT STATUS & ANALOG JOYSTICK (140.dp)
        Column(
            modifier = Modifier
                .width(140.dp)
                .fillMaxHeight()
                .background(Color(0xFF0D0D14), RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFF33334D), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (isPlayerDoctor) "ДОКТОР #404" else "ПАЦИЕНТ #404",
                    color = if (isPlayerDoctor) Color(0xFF80D8FF) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "УР $playerLv",
                    color = Color.Yellow,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ОЗ ",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .background(Color(0xFFC62828))
                            .border(1.dp, Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(playerHp.toFloat() / playerMaxHp.toFloat())
                                .background(Color(0xFFFFEB3B))
                        )
                    }
                }
                Text(
                    text = "$playerHp / $playerMaxHp",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "КОНЦОВКИ: $endingsFound/2",
                    color = if (endingsFound > 0) Color(0xFF00E676) else Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .size(105.dp)
                    .background(Color(0xFF141420), CircleShape)
                    .border(3.dp, Color(0xFF5C6BC0), CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val maxRadius = 38f
                                val currentOffset = joystickVector * maxRadius + dragAmount
                                val dist = sqrt(currentOffset.x * currentOffset.x + currentOffset.y * currentOffset.y)
                                val clamped = if (dist > maxRadius) {
                                    Offset(
                                        currentOffset.x / dist * maxRadius,
                                        currentOffset.y / dist * maxRadius
                                    )
                                } else currentOffset
                                joystickVector = Offset(clamped.x / maxRadius, clamped.y / maxRadius)
                            },
                            onDragEnd = {
                                joystickVector = Offset.Zero
                            },
                            onDragCancel = {
                                joystickVector = Offset.Zero
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = size.width / 2f
                    drawLine(Color(0xFF282845), Offset(c, 10f), Offset(c, size.height - 10f), strokeWidth = 2f)
                    drawLine(Color(0xFF282845), Offset(10f, c), Offset(size.width - 10f, c), strokeWidth = 2f)
                }

                val thumbOffset = IntOffset(
                    (joystickVector.x * 32.dp.value).toInt(),
                    (joystickVector.y * 32.dp.value).toInt()
                )
                Box(
                    modifier = Modifier
                        .offset { thumbOffset }
                        .size(46.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFF5252), Color(0xFFB71C1C))
                            ),
                            CircleShape
                        )
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♥", color = Color.White, fontSize = 16.sp)
                }
            }

            Text(
                text = "АНАЛОГ",
                color = Color(0xFF7986CB),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // COLUMN 2: CENTER GAME CANVAS & UNDERTALE DIALOG (weight(1f))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .border(2.dp, Color(0xFF424242), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("game_canvas")
                ) {
                    val canvasW = size.width
                    val canvasH = size.height
                    val scale = min(canvasW / 640f, canvasH / 360f)
                    val ox = (canvasW - 640f * scale) / 2f + (if (screenShake > 0f) (Math.random().toFloat() - 0.5f) * screenShake else 0f)
                    val oy = (canvasH - 360f * scale) / 2f + (if (screenShake > 0f) (Math.random().toFloat() - 0.5f) * screenShake else 0f)

                    drawRect(Color.Black)

                    fun vx(x: Float) = ox + x * scale
                    fun vy(y: Float) = oy + y * scale
                    fun vs(s: Float) = s * scale

                    if (isInBattle) {
                        drawRect(
                            color = Color(0xFF08080C),
                            topLeft = Offset(vx(0f), vy(0f)),
                            size = Size(vs(640f), vs(360f))
                        )

                        val bShakeX = if (bossShake > 0f) (Math.random().toFloat() - 0.5f) * bossShake else 0f
                        val bossX = 320f - 12f * 4f + bShakeX
                        val bossY = 50f

                        drawPixelSprite(
                            pattern = PixelSprites.BOSS_MIKHALYCH,
                            palette = PixelSprites.PALETTE,
                            x = vx(bossX),
                            y = vy(bossY),
                            pixelSize = vs(4f)
                        )

                        val bossBarW = 200f
                        val bossBarH = 14f
                        val bossBarX = 320f - bossBarW / 2f
                        val bossBarY = 22f

                        drawContext.canvas.nativeCanvas.apply {
                            val paint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                isAntiAlias = false
                                typeface = Typeface.MONOSPACE
                                textSize = vs(14f)
                                textAlign = Paint.Align.CENTER
                            }
                            drawText("МИХАЛЫЧ (БОСС)", vx(320f), vy(16f), paint)
                        }

                        drawRect(
                            color = Color(0xFF550000),
                            topLeft = Offset(vx(bossBarX), vy(bossBarY)),
                            size = Size(vs(bossBarW), vs(bossBarH))
                        )
                        val currBossBar = bossBarW * (bossHp.toFloat() / bossMaxHp.toFloat())
                        drawRect(
                            color = Color(0xFF00E676),
                            topLeft = Offset(vx(bossBarX), vy(bossBarY)),
                            size = Size(vs(currBossBar), vs(bossBarH))
                        )
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(vx(bossBarX), vy(bossBarY)),
                            size = Size(vs(bossBarW), vs(bossBarH)),
                            style = Stroke(width = vs(2f))
                        )

                        val boxX = 220f
                        val boxY = 175f
                        val boxW = 200f
                        val boxH = 150f

                        drawRect(
                            color = Color(0xFF0A0A10),
                            topLeft = Offset(vx(boxX), vy(boxY)),
                            size = Size(vs(boxW), vs(boxH))
                        )
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(vx(boxX), vy(boxY)),
                            size = Size(vs(boxW), vs(boxH)),
                            style = Stroke(width = vs(3f))
                        )

                        if (battlePhase == BattlePhase.ATTACK_BAR) {
                            val centerTargetX = boxX + boxW / 2f
                            drawRect(
                                color = Color(0xFF1B5E20),
                                topLeft = Offset(vx(centerTargetX - 25f), vy(boxY + 10f)),
                                size = Size(vs(50f), vs(boxH - 20f))
                            )
                            drawRect(
                                color = Color(0xFF4CAF50),
                                topLeft = Offset(vx(centerTargetX - 10f), vy(boxY + 10f)),
                                size = Size(vs(20f), vs(boxH - 20f))
                            )

                            val lineX = boxX + boxW * attackBarProgress
                            drawLine(
                                color = Color.White,
                                start = Offset(vx(lineX), vy(boxY + 5f)),
                                end = Offset(vx(lineX), vy(boxY + boxH - 5f)),
                                strokeWidth = vs(4f)
                            )

                            drawContext.canvas.nativeCanvas.apply {
                                val paint = Paint().apply {
                                    color = android.graphics.Color.YELLOW
                                    isAntiAlias = false
                                    typeface = Typeface.MONOSPACE
                                    textSize = vs(14f)
                                    textAlign = Paint.Align.CENTER
                                }
                                drawText("НАЖМИТЕ [Z] ДЛЯ УДАРА!", vx(320f), vy(boxY + boxH / 2f), paint)
                            }
                        }

                        if (battlePhase == BattlePhase.SLASH_ANIM) {
                            val slashStartX = 320f - 40f + slashAnimProgress * 20f
                            val slashStartY = 70f + slashAnimProgress * 60f
                            drawLine(
                                color = Color(0xFFFF1744),
                                start = Offset(vx(slashStartX - 30f), vy(slashStartY - 30f)),
                                end = Offset(vx(slashStartX + 30f), vy(slashStartY + 30f)),
                                strokeWidth = vs(6f)
                            )
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = Paint().apply {
                                    color = if (isCriticalHit) android.graphics.Color.YELLOW else android.graphics.Color.RED
                                    isAntiAlias = false
                                    typeface = Typeface.MONOSPACE
                                    textSize = vs(if (isCriticalHit) 26f else 20f)
                                    textAlign = Paint.Align.CENTER
                                }
                                val text = if (isCriticalHit) "КРИТ! -$lastDamageDealt" else "-$lastDamageDealt"
                                drawText(text, vx(320f), vy(70f - slashAnimProgress * 30f), paint)
                            }
                        }

                        if (battlePhase == BattlePhase.DODGE) {
                            val soulColor = if (soulInvincibleTimer > 0f && (soulInvincibleTimer * 10).toInt() % 2 == 0) {
                                Color.Transparent
                            } else {
                                Color(0xFFFF1744)
                            }
                            drawSoul(vx(soulX), vy(soulY), vs(16f), soulColor)

                            var i = 0
                            while (i < bullets.size) {
                                val b = bullets[i]
                                when (b.type) {
                                    BulletType.PILL -> {
                                        drawCircle(
                                            color = Color(0xFF00E5FF),
                                            radius = vs(b.bulletRadius / 2f),
                                            center = Offset(vx(b.x), vy(b.y))
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = vs(b.bulletRadius / 4f),
                                            center = Offset(vx(b.x), vy(b.y))
                                        )
                                    }
                                    BulletType.SYRINGE -> {
                                        drawLine(
                                            color = Color(0xFFFF9100),
                                            start = Offset(vx(b.x), vy(b.y - 8f)),
                                            end = Offset(vx(b.x), vy(b.y + 8f)),
                                            strokeWidth = vs(4f)
                                        )
                                        drawRect(
                                            color = Color.White,
                                            topLeft = Offset(vx(b.x - 4f), vy(b.y - 2f)),
                                            size = Size(vs(8f), vs(4f))
                                        )
                                    }
                                    BulletType.DROPLET -> {
                                        drawCircle(
                                            color = Color(0xFF76FF03),
                                            radius = vs(b.bulletRadius / 2f),
                                            center = Offset(vx(b.x), vy(b.y))
                                        )
                                    }
                                }
                                i++
                            }

                            drawContext.canvas.nativeCanvas.apply {
                                val paint = Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    isAntiAlias = false
                                    typeface = Typeface.MONOSPACE
                                    textSize = vs(11f)
                                    textAlign = Paint.Align.CENTER
                                }
                                val sec = String.format("%.1f", max(0f, dodgeTimer))
                                drawText("УВОРОТ: $sec с", vx(320f), vy(boxY + boxH - 8f), paint)
                            }
                        }

                        if (battlePhase == BattlePhase.MENU) {
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = Paint().apply {
                                    color = android.graphics.Color.LTGRAY
                                    isAntiAlias = false
                                    typeface = Typeface.MONOSPACE
                                    textSize = vs(12f)
                                    textAlign = Paint.Align.CENTER
                                }
                                drawText("* Выберите действие в окне команд внизу", vx(320f), vy(boxY + boxH / 2f), paint)
                            }
                        }

                        return@Canvas
                    }

                    if (gameScreen == GameScreen.ENDING_VEGETABLE) {
                        val t = (System.currentTimeMillis() % 3000L) / 3000f
                        val c1 = Color(
                            (sin(t * 2.0 * PI) * 0.4 + 0.5).toFloat(),
                            (cos(t * 2.0 * PI) * 0.4 + 0.5).toFloat(),
                            0.8f
                        )
                        drawRect(
                            color = c1,
                            topLeft = Offset(vx(0f), vy(0f)),
                            size = Size(vs(640f), vs(360f))
                        )

                        drawContext.canvas.nativeCanvas.apply {
                            val pTitle = Paint().apply {
                                color = android.graphics.Color.WHITE
                                isAntiAlias = false
                                typeface = Typeface.MONOSPACE
                                textSize = vs(24f)
                                textAlign = Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            drawText("КОНЦОВКА 1/2: «ОВОЩ»", vx(320f), vy(120f), pTitle)

                            val pDesc = Paint().apply {
                                color = android.graphics.Color.WHITE
                                isAntiAlias = false
                                typeface = Typeface.MONOSPACE
                                textSize = vs(14f)
                                textAlign = Paint.Align.CENTER
                            }
                            drawText("Двойная доза галоперидола принесла вечный покой.", vx(320f), vy(170f), pDesc)
                            drawText("Никаких забот, никаких тревог. Только сладкий сон.", vx(320f), vy(200f), pDesc)
                            drawText("Нажмите [Z] или кнопку сброса для новой попытки.", vx(320f), vy(250f), pDesc)
                        }
                        return@Canvas
                    }

                    if (gameScreen == GameScreen.ENDING_DOCTOR) {
                        drawRect(
                            color = Color(0xFF263238),
                            topLeft = Offset(vx(0f), vy(0f)),
                            size = Size(vs(640f), vs(200f))
                        )
                        drawRect(
                            color = Color(0xFF004D40),
                            topLeft = Offset(vx(0f), vy(140f)),
                            size = Size(vs(640f), vs(60f))
                        )
                        for (r in 0 until 6) {
                            for (c in 0 until 16) {
                                val color = if ((r + c) % 2 == 0) Color(0xFFCFD8DC) else Color(0xFF90A4AE)
                                drawRect(
                                    color = color,
                                    topLeft = Offset(vx(c * 40f), vy(200f + r * 27f)),
                                    size = Size(vs(40f), vs(27f))
                                )
                            }
                        }

                        drawRect(
                            color = Color(0xFF37474F),
                            topLeft = Offset(vx(120f), vy(50f)),
                            size = Size(vs(80f), vs(150f))
                        )
                        drawRect(
                            color = Color(0xFF29B6F6),
                            topLeft = Offset(vx(135f), vy(70f)),
                            size = Size(vs(50f), vs(45f))
                        )
                        drawPixelSprite(
                            pattern = PixelSprites.MIKHALYCH_TELNYASHKA,
                            palette = PixelSprites.PALETTE,
                            x = vx(138f),
                            y = vy(72f),
                            pixelSize = vs(2.8f)
                        )

                        drawPixelSprite(
                            pattern = PixelSprites.PLAYER_DOCTOR,
                            palette = PixelSprites.PALETTE,
                            x = vx(270f),
                            y = vy(170f),
                            pixelSize = vs(4.5f)
                        )

                        drawPixelSprite(
                            pattern = PixelSprites.PETROVICH_STAND,
                            palette = PixelSprites.PALETTE,
                            x = vx(380f),
                            y = vy(170f),
                            pixelSize = vs(4.5f),
                            flipX = true
                        )

                        drawContext.canvas.nativeCanvas.apply {
                            val pTitle = Paint().apply {
                                color = android.graphics.Color.YELLOW
                                isAntiAlias = false
                                typeface = Typeface.MONOSPACE
                                textSize = vs(18f)
                                textAlign = Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            drawText("КОНЦОВКА 2/2: «ГЛАВВРАЧ»", vx(320f), vy(35f), pTitle)
                        }
                        return@Canvas
                    }

                    // Padded Cell Room
                    drawRect(
                        color = Color(0xFF303038),
                        topLeft = Offset(vx(80f), vy(40f)),
                        size = Size(vs(480f), vs(280f))
                    )

                    for (wx in 80..560 step 40) {
                        for (wy in 40..240 step 30) {
                            drawLine(
                                color = Color(0xFF23232A),
                                start = Offset(vx(wx.toFloat()), vy(wy.toFloat())),
                                end = Offset(vx((wx + 40).toFloat()), vy((wy + 30).toFloat())),
                                strokeWidth = vs(1.5f)
                            )
                            drawLine(
                                color = Color(0xFF23232A),
                                start = Offset(vx((wx + 40).toFloat()), vy(wy.toFloat())),
                                end = Offset(vx(wx.toFloat()), vy((wy + 30).toFloat())),
                                strokeWidth = vs(1.5f)
                            )
                            drawCircle(
                                color = Color(0xFF16161D),
                                radius = vs(2.5f),
                                center = Offset(vx(wx.toFloat() + 20f), vy(wy.toFloat() + 15f))
                            )
                        }
                    }

                    drawRect(
                        color = Color(0xFF212128),
                        topLeft = Offset(vx(80f), vy(240f)),
                        size = Size(vs(480f), vs(80f))
                    )
                    drawLine(
                        color = Color(0xFF15151A),
                        start = Offset(vx(80f), vy(240f)),
                        end = Offset(vx(560f), vy(240f)),
                        strokeWidth = vs(2.5f)
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        val path = android.graphics.Path().apply {
                            moveTo(vx(320f), vy(10f))
                            lineTo(vx(190f), vy(320f))
                            lineTo(vx(450f), vy(320f))
                            close()
                        }
                        val spotPaint = Paint().apply {
                            color = android.graphics.Color.argb(28, 255, 235, 59)
                            style = Paint.Style.FILL
                        }
                        drawPath(path, spotPaint)
                    }

                    drawRect(
                        color = Color(0xFFEEEEEE),
                        topLeft = Offset(vx(110f), vy(145f)),
                        size = Size(vs(55f), vs(35f))
                    )
                    drawRect(
                        color = Color(0xFFBDBDBD),
                        topLeft = Offset(vx(110f), vy(145f)),
                        size = Size(vs(55f), vs(35f)),
                        style = Stroke(width = vs(2f))
                    )
                    drawLine(
                        color = Color(0xFF9E9E9E),
                        start = Offset(vx(120f), vy(160f)),
                        end = Offset(vx(155f), vy(160f)),
                        strokeWidth = vs(1.5f)
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        val pScratch = Paint().apply {
                            color = android.graphics.Color.argb(160, 255, 255, 255)
                            isAntiAlias = false
                            typeface = Typeface.MONOSPACE
                            textSize = vs(9f)
                        }
                        drawText("I: ✊", vx(480f), vy(140f), pScratch)
                        drawText("II: 👄", vx(480f), vy(155f), pScratch)
                        drawText("III: ✊", vx(480f), vy(170f), pScratch)
                        drawText("IV: ✊", vx(480f), vy(185f), pScratch)
                        drawText("V: 👄", vx(480f), vy(200f), pScratch)
                    }

                    val doorX = 275f
                    val doorY = 240f
                    val doorW = 90f
                    val doorH = 80f

                    if (doorOpen) {
                        drawRect(
                            color = Color(0xFF0A0A0F),
                            topLeft = Offset(vx(doorX), vy(doorY)),
                            size = Size(vs(doorW), vs(doorH))
                        )
                        drawCircle(
                            color = Color(0xFFFF1744),
                            radius = vs(4f),
                            center = Offset(vx(320f), vy(260f))
                        )
                    } else {
                        drawRect(
                            color = Color(0xFF455A64),
                            topLeft = Offset(vx(doorX), vy(doorY)),
                            size = Size(vs(doorW), vs(doorH))
                        )
                        drawCircle(Color(0xFF263238), vs(3f), Offset(vx(doorX + 8f), vy(doorY + 10f)))
                        drawCircle(Color(0xFF263238), vs(3f), Offset(vx(doorX + doorW - 8f), vy(doorY + 10f)))
                        drawCircle(Color(0xFF263238), vs(3f), Offset(vx(doorX + 8f), vy(doorY + doorH - 10f)))
                        drawCircle(Color(0xFF263238), vs(3f), Offset(vx(doorX + doorW - 8f), vy(doorY + doorH - 10f)))

                        drawRect(
                            color = Color(0xFF212121),
                            topLeft = Offset(vx(doorX + 30f), vy(doorY + 20f)),
                            size = Size(vs(30f), vs(15f))
                        )
                        drawLine(
                            color = Color(0xFF78909C),
                            start = Offset(vx(doorX + 40f), vy(doorY + 20f)),
                            end = Offset(vx(doorX + 40f), vy(doorY + 35f)),
                            strokeWidth = vs(1.5f)
                        )
                        drawLine(
                            color = Color(0xFF78909C),
                            start = Offset(vx(doorX + 50f), vy(doorY + 20f)),
                            end = Offset(vx(doorX + 50f), vy(doorY + 35f)),
                            strokeWidth = vs(1.5f)
                        )

                        drawRect(
                            color = Color(0xFF90A4AE),
                            topLeft = Offset(vx(doorX + 68f), vy(doorY + 45f)),
                            size = Size(vs(16f), vs(8f))
                        )
                    }

                    if (gameScreen == GameScreen.INTRO) {
                        drawPixelSprite(
                            pattern = PixelSprites.MIKHALYCH_STAND,
                            palette = PixelSprites.PALETTE,
                            x = vx(orderly1X),
                            y = vy(orderly1Y),
                            pixelSize = vs(3f)
                        )
                        drawPixelSprite(
                            pattern = PixelSprites.PETROVICH_STAND,
                            palette = PixelSprites.PALETTE,
                            x = vx(orderly2X),
                            y = vy(orderly2Y),
                            pixelSize = vs(3f),
                            flipX = true
                        )
                    }

                    val playerSprite = when {
                        isPlayerDoctor -> PixelSprites.PLAYER_DOCTOR
                        walkFrame == 1 -> PixelSprites.PLAYER_WALK_1
                        walkFrame == 3 -> PixelSprites.PLAYER_WALK_2
                        else -> PixelSprites.PLAYER_STAND
                    }

                    drawPixelSprite(
                        pattern = playerSprite,
                        palette = PixelSprites.PALETTE,
                        x = vx(playerX - 24f),
                        y = vy(playerY - 24f),
                        pixelSize = vs(3.2f),
                        flipX = playerFacingLeft
                    )

                    var pi = 0
                    while (pi < particles.size) {
                        val p = particles[pi]
                        drawCircle(
                            color = p.color,
                            radius = vs(p.particleRadius * p.life),
                            center = Offset(vx(p.x), vy(p.y))
                        )
                        pi++
                    }

                    if (gameScreen == GameScreen.ROOM && !isInBattle && currentDialogue == null) {
                        var promptText: String? = null
                        var promptX = 320f
                        var promptY = 220f

                        if (playerX in 260f..380f && playerY > 230f) {
                            promptText = "[Z] ДВЕРЬ"
                            promptX = 320f
                            promptY = 230f
                        } else if (playerX < 240f && playerY < 200f) {
                            promptText = "[Z] ПОДУШКА"
                            promptX = 140f
                            promptY = 135f
                        } else if (playerX > 430f && playerY < 210f) {
                            promptText = "[Z] ШИФР НА СТЕНЕ"
                            promptX = 490f
                            promptY = 125f
                        }

                        if (promptText != null) {
                            drawContext.canvas.nativeCanvas.apply {
                                val pBubble = Paint().apply {
                                    color = android.graphics.Color.YELLOW
                                    isAntiAlias = false
                                    typeface = Typeface.MONOSPACE
                                    textSize = vs(11f)
                                    textAlign = Paint.Align.CENTER
                                    isFakeBoldText = true
                                }
                                drawText(promptText, vx(promptX), vy(promptY), pBubble)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // UNDERTALE DIALOGUE WINDOW (125.dp, 3.dp white border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .background(Color.Black, RoundedCornerShape(4.dp))
                    .border(3.dp, Color.White, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activePortrait = currentDialogue?.portrait ?: PortraitType.NONE
                    if (activePortrait != PortraitType.NONE) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF121218))
                                .border(2.dp, Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(56.dp)) {
                                val pattern = when (activePortrait) {
                                    PortraitType.PLAYER -> PixelSprites.PLAYER_STAND
                                    PortraitType.PLAYER_DOCTOR -> PixelSprites.PLAYER_DOCTOR
                                    PortraitType.MIKHALYCH, PortraitType.MIKHALYCH_ANGRY -> PixelSprites.MIKHALYCH_STAND
                                    PortraitType.PETROVICH -> PixelSprites.PETROVICH_STAND
                                    else -> PixelSprites.PLAYER_STAND
                                }
                                drawPixelSprite(
                                    pattern = pattern,
                                    palette = PixelSprites.PALETTE,
                                    x = 0f,
                                    y = 0f,
                                    pixelSize = size.width / 16f
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        val speaker = currentDialogue?.speaker
                        if (speaker != null) {
                            Text(
                                text = speaker.uppercase(),
                                color = Color(0xFFFFD54F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        val diag = currentDialogue
                        val displayText = if (diag != null) {
                            diag.text.take(typedCharacters)
                        } else if (isInBattle) {
                            "* Михалыч ждет вашего хода..."
                        } else {
                            "* Исследуйте палату. Управление: джойстик слева, [Z] — действие."
                        }

                        Text(
                            text = displayText,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp,
                            modifier = Modifier.weight(1f)
                        )

                        if (diag != null && isTypingComplete && diag.options.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (index in 0 until diag.options.size) {
                                    val opt = diag.options[index]
                                    val isSelected = (index == selectedOptionIndex)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedOptionIndex = index
                                                handleZConfirm()
                                            }
                                            .padding(vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (isSelected) "♥ " else "  ",
                                            color = Color(0xFFFF1744),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = opt,
                                            color = if (isSelected) Color.Yellow else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // COLUMN 3: RIGHT CONTROLS & ARCADE BUTTONS (110.dp)
        Column(
            modifier = Modifier
                .width(110.dp)
                .fillMaxHeight()
                .background(Color(0xFF0D0D14), RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFF33334D), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(if (soundEngine.isMuted) Color(0xFFB71C1C) else Color(0xFF1E88E5), CircleShape)
                        .clickable {
                            soundEngine.isMuted = !soundEngine.isMuted
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (soundEngine.isMuted) "🔇" else "🔊",
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFF455A64), CircleShape)
                        .clickable { showHelpModal = !showHelpModal },
                    contentAlignment = Alignment.Center
                ) {
                    Text("?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFF37474F), CircleShape)
                        .clickable { startIntroCutscene() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↺", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF00E676), Color(0xFF1B5E20))),
                            CircleShape
                        )
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { handleZConfirm() }
                        .testTag("action_z_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Z",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = if (battlePhase == BattlePhase.ATTACK_BAR) "УДАР!" else "ВЫБОР",
                    color = Color(0xFF69F0AE),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(4.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFFFF5252), Color(0xFFB71C1C))),
                            CircleShape
                        )
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { handleXAction() }
                        .testTag("action_x_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "X",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "ПРОПУСК",
                    color = Color(0xFFFF8A80),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    if (showHelpModal) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable { showHelpModal = false }
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color(0xFF1E1E2C), RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "«DUROTALE: ПОБЕГ ИЗ ДУРКИ» — ПАМЯТКА",
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Управление: аналоговый джойстик слева (ходьба, полет души ♥ в бою, выбор меню).\n" +
                                "• Кнопка [Z]: выбор, взаимодействие, удар по шкале.\n" +
                                "• Кнопка [X]: мгновенный пропуск печати текста.\n" +
                                "• Концовка 1/2 («Овощ»): подойдите к двери и введите шифр со стены: Тарабанить -> Орать -> Тарабанить -> Тарабанить -> Орать.\n" +
                                "• Концовка 2/2 («Главврач»): обманите Михалыча («Выронили телефон»), победите его в бою Undertale и примерьте белый халат!",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .background(Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                            .clickable { showHelpModal = false }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("ПОНЯТНО [X]", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
