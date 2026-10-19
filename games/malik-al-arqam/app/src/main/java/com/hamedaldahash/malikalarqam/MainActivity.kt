package com.hamedaldahash.malikalarqam

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.random.Random

private val Ink = Color(0xFF111126)
private val Panel = Color(0xFF20203D)
private val Violet = Color(0xFF7C4DFF)
private val Cyan = Color(0xFF35D7FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MalikGame() }
    }
}

private data class Snapshot(val board: List<Int>, val score: Int)
private data class GameState(
    val size: Int,
    val board: List<Int>,
    val score: Int,
    val best: Int,
    val undo: Snapshot?,
    val dark: Boolean,
    val vibration: Boolean
)

@Composable
private fun MalikGame() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("malik_game", Context.MODE_PRIVATE) }
    var size by remember { mutableIntStateOf(prefs.getInt("size", 4).coerceIn(3, 5)) }
    var board by remember { mutableStateOf(loadBoard(prefs.getString("board", null), size)) }
    var score by remember { mutableIntStateOf(prefs.getInt("score", 0)) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
    var undo by remember { mutableStateOf<Snapshot?>(null) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    var vibration by remember { mutableStateOf(prefs.getBoolean("vibration", true)) }
    var settingsOpen by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }

    fun persist() {
        prefs.edit().putInt("size", size).putString("board", JSONArray(board).toString())
            .putInt("score", score).putInt("best", best)
            .putBoolean("dark", dark).putBoolean("vibration", vibration).apply()
    }
    fun newGame(newSize: Int = size) {
        size = newSize
        board = List(size * size) { 0 }.let { addRandom(addRandom(it)) }
        score = 0
        undo = null
        won = false
        gameOver = false
        persist()
    }
    fun move(direction: Int) {
        if (gameOver) return
        val result = slide(board, size, direction)
        if (result.first == board) return
        undo = Snapshot(board, score)
        board = addRandom(result.first)
        score += result.second
        if (score > best) best = score
        if (board.any { it >= 2048 }) won = true
        gameOver = !hasMoves(board, size)
        if (vibration) vibrate(context)
        persist()
    }

    val bg = if (dark) Ink else Color(0xFFF3F3FC)
    val fg = if (dark) Color.White else Color(0xFF20203D)
    val sub = if (dark) Color(0xFFB8B7D4) else Color(0xFF696987)

    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Violet, background = Ink, surface = Panel)
        else lightColorScheme(primary = Violet, background = Color(0xFFF3F3FC), surface = Color.White)) {
        CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Box(Modifier.fillMaxSize().background(bg)) {
                Column(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("👑  ملك الأرقام", color = fg, fontSize = 27.sp, fontWeight = FontWeight.Black)
                            Text("لعبة 2048 المطوّرة", color = sub, fontSize = 14.sp)
                        }
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = fg)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ScoreCard("النقاط", score, Modifier.weight(1f))
                        ScoreCard("أفضل نتيجة", best, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { newGame() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("لعبة جديدة")
                        }
                        OutlinedButton(
                            onClick = {
                                undo?.let { snap ->
                                    board = snap.board; score = snap.score; undo = null
                                    gameOver = false; won = false; persist()
                                }
                            },
                            enabled = undo != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("تراجع")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF28284B), Color(0xFF19192F))))
                            .padding(8.dp)
                            .pointerInput(size, board, gameOver) {
                                var dx = 0f
                                var dy = 0f
                                detectDragGestures(
                                    onDragStart = { dx = 0f; dy = 0f },
                                    onDrag = { change, drag ->
                                        dx += drag.x; dy += drag.y; change.consume()
                                    },
                                    onDragEnd = {
                                        if (maxOf(abs(dx), abs(dy)) > 24f) {
                                            val dir = if (abs(dx) > abs(dy)) { if (dx > 0) 1 else 3 }
                                            else { if (dy > 0) 2 else 0 }
                                            move(dir)
                                        }
                                    }
                                )
                            }
                    ) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(size) { row ->
                                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    repeat(size) { col ->
                                        val value = board[row * size + col]
                                        Box(
                                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(11.dp))
                                                .background(tileColor(value)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AnimatedContent(value, transitionSpec = { tween(100) togetherWith tween(100) }, label = "tile") { n ->
                                                if (n > 0) Text(
                                                    n.toString(),
                                                    color = if (n <= 4) Color(0xFF37334E) else Color.White,
                                                    fontSize = (if (size == 5) 19 else 25).sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (gameOver || won) {
                            Column(
                                Modifier.fillMaxSize().background(Color(0xDD111126), RoundedCornerShape(18.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(if (won) "إنجاز رائع! 👑" else "انتهت اللعبة", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                Text(if (won) "وصلت إلى 2048 أو أكثر" else "جرّب مرة أخرى لتحقق نتيجة أعلى", color = Color(0xFFD3D1F0))
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { newGame() }) { Text("العب مجددًا") }
                            }
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    Text("اسحب اللوحة للأعلى أو الأسفل أو الجانبين لتحريك الأرقام", color = sub, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.weight(1f))
                    Text("برمجة حامد الدهاش", color = sub, fontSize = 12.sp)
                }
            }
        }
    }

    if (settingsOpen) {
        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text("إعدادات اللعبة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("حجم اللوحة")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 4, 5).forEach { n ->
                            FilterChip(selected = size == n, onClick = { newGame(n) }, label = { Text("${n}×${n}") })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("الوضع الداكن", modifier = Modifier.weight(1f))
                        Switch(checked = dark, onCheckedChange = { dark = it; persist() })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("الاهتزاز عند الحركة", modifier = Modifier.weight(1f))
                        Switch(checked = vibration, onCheckedChange = { vibration = it; persist() })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { settingsOpen = false }) { Text("تم") } }
        )
    }
}

@Composable
private fun ScoreCard(title: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF302653), Color(0xFF20203D)))).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color(0xFFC8C2E8), fontSize = 12.sp)
        Text(value.toString(), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun tileColor(n: Int): Color = when (n) {
    0 -> Color(0xFF2A2947)
    2 -> Color(0xFFE9E5FF)
    4 -> Color(0xFFD2C8FF)
    8 -> Color(0xFFB6A0FF)
    16 -> Color(0xFF9577FF)
    32 -> Color(0xFF7955F5)
    64 -> Color(0xFF6542E8)
    128 -> Color(0xFF2879E8)
    256 -> Color(0xFF199DD8)
    512 -> Color(0xFF10B7D4)
    1024 -> Color(0xFF00BFA5)
    else -> Color(0xFFFFB547)
}

private fun loadBoard(raw: String?, size: Int): List<Int> {
    return try {
        val arr = JSONArray(raw ?: "")
        if (arr.length() != size * size) emptyBoard(size) else List(arr.length()) { arr.getInt(it) }
    } catch (_: Exception) { addRandom(addRandom(emptyBoard(size))) }
}
private fun emptyBoard(size: Int) = List(size * size) { 0 }
private fun addRandom(board: List<Int>): List<Int> {
    val empty = board.indices.filter { board[it] == 0 }
    if (empty.isEmpty()) return board
    val idx = empty.random()
    return board.toMutableList().also { it[idx] = if (Random.nextFloat() < .9f) 2 else 4 }
}

/** direction: 0 up, 1 right, 2 down, 3 left */
private fun slide(board: List<Int>, size: Int, direction: Int): Pair<List<Int>, Int> {
    val out = MutableList(size * size) { 0 }
    var gained = 0
    for (line in 0 until size) {
        val indices = when (direction) {
            0 -> (0 until size).map { it * size + line }
            1 -> (0 until size).map { line * size + (size - 1 - it) }
            2 -> (0 until size).map { (size - 1 - it) * size + line }
            else -> (0 until size).map { line * size + it }
        }
        val values = indices.map { board[it] }.filter { it != 0 }.toMutableList()
        val merged = mutableListOf<Int>()
        var i = 0
        while (i < values.size) {
            if (i + 1 < values.size && values[i] == values[i + 1]) {
                val v = values[i] * 2
                merged.add(v); gained += v; i += 2
            } else { merged.add(values[i]); i++ }
        }
        indices.forEachIndexed { pos, idx -> out[idx] = merged.getOrElse(pos) { 0 } }
    }
    return out to gained
}
private fun hasMoves(board: List<Int>, size: Int): Boolean {
    if (board.any { it == 0 }) return true
    for (r in 0 until size) for (c in 0 until size) {
        val v = board[r * size + c]
        if (c + 1 < size && board[r * size + c + 1] == v) return true
        if (r + 1 < size && board[(r + 1) * size + c] == v) return true
    }
    return false
}
private fun vibrate(context: Context) {
    try {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        if (android.os.Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(22, 55))
        else @Suppress("DEPRECATION") vibrator.vibrate(22)
    } catch (_: Exception) { }
}
