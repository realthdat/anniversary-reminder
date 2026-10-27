package com.mine.reminder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.flow.collect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private data class Tone(
    val ink: Color,
    val paper: Color,
    val muted: Color,
    val line: Color,
    val action: Color,
    val button: Color,
    val delete: Color,
    val card: Color,
)

private val LightTone = Tone(
    ink = Color(0xFF233C46), paper = Color(0xFFFAF7F0), muted = Color(0xFF6F7775),
    line = Color(0xFFE8E2D6), action = Color(0xFF8B622C), button = Color(0xFF233F49),
    delete = Color(0xFFB91C1C), card = Color(0xFFFFFDF9),
)
private val DarkTone = Tone(
    ink = Color(0xFFF4EDDF), paper = Color(0xFF152730), muted = Color(0xFFAEBBBD),
    line = Color(0xFF354A52), action = Color(0xFFEDCA8D), button = Color(0xFF385C68),
    delete = Color(0xFFFCA5A5), card = Color(0xFF203640),
)

private val LocalTone = staticCompositionLocalOf { LightTone }

private object C {
    val ink: Color @Composable get() = LocalTone.current.ink
    val paper: Color @Composable get() = LocalTone.current.paper
    val muted: Color @Composable get() = LocalTone.current.muted
    val line: Color @Composable get() = LocalTone.current.line
    val action: Color @Composable get() = LocalTone.current.action
    val button: Color @Composable get() = LocalTone.current.button
    val delete: Color @Composable get() = LocalTone.current.delete
    val card: Color @Composable get() = LocalTone.current.card
}

private val ButtonShape = RoundedCornerShape(16.dp)

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

class MainActivity : ComponentActivity() {
    private var ticks by mutableIntStateOf(0)
    private var askedNotify = false
    private val askNotify = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        Alarms.reschedule(this)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Alarms.ensureChannel(this)
        setContent { App(ticks) }
    }

    override fun onResume() {
        super.onResume()
        ticks++
        if (!askedNotify && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            askedNotify = true
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        Alarms.reschedule(this)
    }
}

object Store {
    private const val PREF = "reminder"

    private fun prefs(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun events(context: Context): List<Event> = try {
        val raw = prefs(context).getString("events", "[]") ?: "[]"
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(Event(item.getLong("id"), item.getString("name"), LocalDate.parse(item.getString("start")), readCycles(item)))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun upsert(context: Context, event: Event) {
        save(context, events(context).filterNot { it.id == event.id } + event)
    }

    fun delete(context: Context, id: Long) {
        save(context, events(context).filterNot { it.id == id })
    }

    fun time(context: Context): Pair<Int, Int> {
        val prefs = prefs(context)
        return prefs.getInt("hour", 9) to prefs.getInt("minute", 0)
    }

    fun setTime(context: Context, hour: Int, minute: Int) {
        prefs(context).edit().putInt("hour", hour).putInt("minute", minute).commit()
    }

    fun earlyDays(context: Context): Int {
        val days = prefs(context).getInt("early", 0)
        return if (days == 1 || days == 3 || days == 7) days else 0
    }

    fun setEarlyDays(context: Context, days: Int) {
        prefs(context).edit().putInt("early", if (days == 1 || days == 3 || days == 7) days else 0).commit()
    }

    fun autostart(context: Context) = prefs(context).getBoolean("autostart", false)

    fun setAutostart(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("autostart", on).commit()
    }

    fun language(context: Context) = prefs(context).getString("language", "") ?: ""

    fun setLanguage(context: Context, code: String) {
        prefs(context).edit().putString("language", code).commit()
    }

    fun theme(context: Context) = prefs(context).getString("theme", "") ?: ""

    fun setTheme(context: Context, mode: String) {
        prefs(context).edit().putString("theme", mode).commit()
    }

    fun wasCheered(context: Context, id: Long, date: LocalDate): Boolean =
        prefs(context).getStringSet("cheered", emptySet())!!.contains("$id|$date")

    fun markCheered(context: Context, id: Long, date: LocalDate) {
        val prefs = prefs(context)
        val next = prefs.getStringSet("cheered", emptySet())!!.toMutableSet()
        next += "$id|$date"
        prefs.edit().putStringSet("cheered", next).commit()
    }

    fun wasNotified(context: Context, id: Long, date: LocalDate, early: Boolean = false): Boolean =
        prefs(context).getStringSet("notified", emptySet())!!.contains(notifiedKey(id, date, early))

    fun markNotified(context: Context, id: Long, date: LocalDate, early: Boolean = false) {
        val prefs = prefs(context)
        val next = prefs.getStringSet("notified", emptySet())!!.toMutableSet()
        next += notifiedKey(id, date, early)
        prefs.edit().putStringSet("notified", next).commit()
    }

    private fun notifiedKey(id: Long, date: LocalDate, early: Boolean) = if (early) "$id|$date|e" else "$id|$date"

    private fun readCycles(item: JSONObject): List<DayCycle> {
        if (!item.has("cycles")) return defaultCycles()
        val array = item.getJSONArray("cycles")
        return buildList {
            for (i in 0 until array.length()) {
                val cycle = array.getJSONObject(i)
                val days = cycle.getInt("n")
                if (days > 0) add(DayCycle(days, cycle.getBoolean("on")))
            }
        }.distinctBy { it.days }.sortedBy { it.days }
    }

    private fun save(context: Context, events: List<Event>) {
        val array = JSONArray()
        for (event in events) {
            val cycles = JSONArray()
            for (cycle in event.cycles) cycles.put(JSONObject().put("n", cycle.days).put("on", cycle.on))
            array.put(JSONObject().put("id", event.id).put("name", event.name).put("start", event.start.toString()).put("cycles", cycles))
        }
        prefs(context).edit().putString("events", array.toString()).commit()
    }
}

fun Context.chosenLanguage(): String {
    val saved = Store.language(this)
    if (saved.isNotEmpty()) return saved
    val device = resources.configuration.locales[0].language
    return if (device == "vi" || device == "zh" || device == "ko") device else "en"
}

fun Context.withAppLocale(): Context {
    val code = Store.language(this)
    if (code.isEmpty()) return this
    val locale = Locale.forLanguageTag(code)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

private sealed interface Screen {
    data object List : Screen
    data class Edit(val event: Event?) : Screen
    data object Events : Screen
    data object Setup : Screen
}

@Composable
private fun App(resumeTick: Int) {
    val context = LocalContext.current
    var themeChoice by remember { mutableStateOf(Store.theme(context)) }
    val dark = when (themeChoice) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    SideEffect {
        val window = (context as Activity).window
        val bg = if (dark) 0xFF152730.toInt() else 0xFFFAF7F0.toInt()
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(if (dark) 0 else flags, flags)
    }
    val tone = if (dark) DarkTone else LightTone
    val scheme = if (dark) {
        darkColorScheme(primary = tone.button, onPrimary = Color.White, background = tone.paper, surface = tone.card, onBackground = tone.ink, onSurface = tone.ink, error = tone.delete, outline = tone.line)
    } else {
        lightColorScheme(primary = tone.button, onPrimary = Color.White, background = tone.paper, surface = tone.card, onBackground = tone.ink, onSurface = tone.ink, error = tone.delete, outline = tone.line)
    }
    CompositionLocalProvider(LocalTone provides tone) {
    MaterialTheme(colorScheme = scheme.copy(
        primary = tone.action, onPrimary = if (dark) tone.paper else Color.White,
        primaryContainer = tone.button, onPrimaryContainer = Color(0xFFFFF8E9),
        secondary = tone.action, onSecondary = if (dark) tone.paper else Color.White,
        secondaryContainer = tone.button, onSecondaryContainer = Color(0xFFFFF8E9),
        surfaceVariant = tone.card, onSurfaceVariant = tone.muted,
        surfaceContainerLowest = tone.paper, surfaceContainerLow = tone.card,
        surfaceContainer = tone.card, surfaceContainerHigh = tone.card, surfaceContainerHighest = tone.card,
        outline = if (dark) Color(0xFF617780) else Color(0xFFB9B6AA), outlineVariant = tone.line,
        inverseSurface = if (dark) LightTone.card else DarkTone.card,
        inverseOnSurface = if (dark) LightTone.ink else DarkTone.ink,
    )) {
        var openSetup by rememberSaveable { mutableStateOf(false) }
        var screen by remember { mutableStateOf<Screen>(if (openSetup) Screen.Setup else Screen.List) }
        var editReturn by remember { mutableStateOf<Screen>(Screen.List) }
        BackHandler(enabled = screen !is Screen.List) {
            screen = when (screen) { is Screen.Edit -> editReturn; Screen.Events -> Screen.Setup; else -> Screen.List }
            openSetup = screen == Screen.Setup || screen == Screen.Events
        }
        Scaffold(containerColor = C.paper) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                when (val current = screen) {
                    Screen.List -> ListScreen(
                        resumeTick,
                        onAdd = { editReturn = Screen.List; screen = Screen.Edit(null) },
                        onEdit = { editReturn = Screen.List; screen = Screen.Edit(it) },
                        onSetup = { openSetup = true; screen = Screen.Setup },
                        dark = dark,
                        onToggleTheme = {
                            val mode = if (dark) "light" else "dark"
                            Store.setTheme(context, mode)
                            themeChoice = mode
                        },
                    )
                    is Screen.Edit -> key(current.event?.id ?: 0L) { EditScreen(current.event) { screen = editReturn } }
                    Screen.Events -> EventsScreen(
                        onBack = { screen = Screen.Setup },
                        onAdd = { editReturn = Screen.Events; screen = Screen.Edit(null) },
                        onEdit = { editReturn = Screen.Events; screen = Screen.Edit(it) },
                    )
                    Screen.Setup -> SetupScreen(
                        resumeTick,
                        onEvents = { screen = Screen.Events },
                        language = context.chosenLanguage(),
                        onLanguage = { code ->
                            val current = context.chosenLanguage()
                            Store.setLanguage(context, code)
                            if (code != current) (context as Activity).recreate()
                        },
                        onBack = { openSetup = false; screen = Screen.List },
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun ListScreen(resumeTick: Int, onAdd: () -> Unit, onEdit: (Event) -> Unit, onSetup: () -> Unit, dark: Boolean, onToggleTheme: () -> Unit) {
    val context = LocalContext.current
    val today = remember(resumeTick) { LocalDate.now() }
    val (hour, minute) = remember(resumeTick) { Store.time(context) }
    var revision by remember { mutableIntStateOf(0) }
    var dropId by remember { mutableStateOf<Long?>(null) }
    val events = remember(resumeTick, revision) { Store.events(context) }
    val includeToday = stillAhead(today, hour, minute, LocalDateTime.now())
    val early = remember(resumeTick) { Store.earlyDays(context) }
    val ranked = events.mapNotNull { event ->
        nextHit(event.start, today, includeToday, event.cycles)?.let { event to it }
    }.sortedBy { it.second.date }
    val elapsed = events.map { it to ChronoUnit.DAYS.between(it.start, today) }.filter { it.second >= 0 }.sortedByDescending { it.second }
    val cheers = remember(resumeTick) {
        events.mapNotNull { event ->
            val labels = labelsOn(event.start, today, event.cycles)
            if (labels.isEmpty() || Store.wasCheered(context, event.id, today)) null else event to Hit(today, labels)
        }
    }
    var showAllUpcoming by rememberSaveable { mutableStateOf(false) }
    var showAllTogether by rememberSaveable { mutableStateOf(false) }
    val listScroll = rememberScrollState()
    var addVisible by remember { mutableStateOf(true) }
    val scrollThreshold = with(LocalDensity.current) { 12.dp.toPx() }
    LaunchedEffect(listScroll, scrollThreshold) {
        var previous = listScroll.value
        var travel = 0
        snapshotFlow { listScroll.value }.collect { current ->
            val delta = current - previous
            previous = current
            if (current == 0) {
                addVisible = true
                travel = 0
            } else if (delta != 0) {
                if ((delta > 0) != (travel > 0)) travel = 0
                travel += delta
                if (kotlin.math.abs(travel) >= scrollThreshold) {
                    addVisible = travel < 0
                    travel = 0
                }
            }
        }
    }
    var showCheer by remember(resumeTick) { mutableStateOf(cheers.isNotEmpty()) }
    LaunchedEffect(resumeTick) { cheers.forEach { (event, _) -> Store.markCheered(context, event.id, today) } }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.app_name)) {
            IconButton(onClick = onToggleTheme) {
                Icon(
                    painterResource(if (dark) R.drawable.ic_sun else R.drawable.ic_moon),
                    contentDescription = stringResource(if (dark) R.string.light else R.string.dark),
                )
            }
            IconButton(onClick = onSetup) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.phone_setup))
            }
        }
        if (ranked.isEmpty()) {
            Column(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.no_dates), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = C.ink)
                Text(
                    stringResource(R.string.empty_body),
                    color = C.muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(listScroll).padding(start = 20.dp, end = 20.dp, bottom = 96.dp)) {
                Text(stringResource(R.string.home_eyebrow), color = C.action, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text(stringResource(R.string.home_title), color = C.ink, fontSize = 29.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.home_subtitle), color = C.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
                val (hero, hit) = ranked.first()
                HeroCard(hero, hit, today, leadDate(hit.date, today, includeToday, early), { onEdit(hero) }) { dropId = hero.id }
                if (ranked.size > 1) {
                    Text(stringResource(R.string.upcoming_heading), color = C.ink, fontSize = 21.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
                    ranked.drop(1).take(if (showAllUpcoming) Int.MAX_VALUE else 3).forEach { (event, later) -> LaterRow(event, later, today, leadDate(later.date, today, includeToday, early), { onEdit(event) }) { dropId = event.id } }
                    if (ranked.size > 4) TextButton(onClick = { showAllUpcoming = !showAllUpcoming }) { Text(stringResource(if (showAllUpcoming) R.string.show_less else R.string.show_more)) }
                }
                if (elapsed.isNotEmpty()) {
                    Text(stringResource(R.string.together_heading), color = C.ink, fontSize = 21.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
                    elapsed.take(if (showAllTogether) Int.MAX_VALUE else 3).forEach { (event, days) -> SinceRow(event, days, { onEdit(event) }) { dropId = event.id } }
                    if (elapsed.size > 3) TextButton(onClick = { showAllTogether = !showAllTogether }) { Text(stringResource(if (showAllTogether) R.string.show_less else R.string.show_more)) }
                }

            }
        }
    }
    AnimatedVisibility(
        visible = addVisible || ranked.isEmpty(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
        enter = fadeIn() + scaleIn(initialScale = 0.7f),
        exit = fadeOut() + scaleOut(targetScale = 0.7f),
    ) {
        val addLabel = stringResource(R.string.add_date)
        FloatingActionButton(
            onClick = onAdd, shape = CircleShape,
            containerColor = C.button, contentColor = Color.White,
            modifier = Modifier.size(60.dp).semantics { contentDescription = addLabel },
        ) { Text("+", fontSize = 32.sp, fontWeight = FontWeight.Light) }
    }
    if (showCheer) CheerOverlay(cheers) { showCheer = false }
    val removing = dropId?.let { id -> events.find { it.id == id } }
    if (removing != null) {
        AlertDialog(
            onDismissRequest = { dropId = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(removing.name) },
            confirmButton = {
                TextButton(onClick = {
                    Store.delete(context, removing.id)
                    Alarms.reschedule(context)
                    dropId = null
                    revision++
                }) { Text(stringResource(R.string.delete), color = C.delete) }
            },
            dismissButton = { TextButton(onClick = { dropId = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun EditScreen(existing: Event?, onDone: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var date by remember { mutableStateOf(existing?.start ?: LocalDate.now()) }
    var cycles by remember { mutableStateOf(existing?.cycles ?: defaultCycles()) }
    var extra by remember { mutableStateOf("") }
    var dropDays by remember { mutableStateOf<Int?>(null) }
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(if (existing == null) R.string.new_date else R.string.edit_date), onBack = onDone)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 56.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, C.line, RoundedCornerShape(18.dp)).clickable { picking = true }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.date), color = C.muted, fontSize = 13.sp)
                    Text(formatDate(date), color = C.ink, fontSize = 16.sp)
                }
            }
            SettingsGroup(stringResource(R.string.cycles)) {
                cycles.forEachIndexed { index, cycle ->
                    if (index > 0) Hairline()
                    val markDate = date.plusDays(cycle.days.toLong())
                    val (hour, minute) = Store.time(context)
                    val past = !stillAhead(markDate, hour, minute, LocalDateTime.now())
                    val ink = if (past) C.muted else C.ink
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).combinedClickable(
                            onClick = {},
                            onLongClick = { dropDays = cycle.days },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(context.markPhrase(Mark(cycle.days, MarkUnit.DAY)), color = ink, fontWeight = FontWeight.SemiBold)
                            Text(formatDate(markDate), color = C.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        if (past) {
                            Text(
                                stringResource(R.string.passed),
                                color = C.muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(end = 8.dp).clip(RoundedCornerShape(99.dp)).background(C.line).padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Switch(
                            checked = cycle.on,
                            onCheckedChange = { on -> cycles = cycles.map { if (it.days == cycle.days) it.copy(on = on) else it } },
                            colors = SwitchDefaults.colors(checkedTrackColor = C.button, checkedThumbColor = Color.White),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = extra,
                        onValueChange = { extra = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text(stringResource(R.string.cycles)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            val days = extra.toIntOrNull() ?: return@Button
                            if (days < 1 || days > 100_000 || cycles.any { it.days == days }) return@Button
                            cycles = (cycles + DayCycle(days, true)).sortedBy { it.days }
                            extra = ""
                            focusManager.clearFocus()
                            keyboard?.hide()
                        },
                        enabled = extra.toIntOrNull()?.let { days -> days in 1..100_000 && cycles.none { it.days == days } } == true,
                        modifier = Modifier.padding(start = 8.dp).height(48.dp),
                        shape = ButtonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = C.button, contentColor = Color.White, disabledContainerColor = C.button.copy(alpha = 0.4f), disabledContentColor = Color.White),
                    ) { Text(stringResource(R.string.add_mark), fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        PrimaryButton(stringResource(R.string.save), enabled = name.isNotBlank()) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@PrimaryButton
            Store.upsert(context, Event(existing?.id ?: System.currentTimeMillis(), trimmed, date, cycles))
            Alarms.reschedule(context)
            onDone()
        }
        if (existing != null) {
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp).height(48.dp),
                shape = ButtonShape,
                border = BorderStroke(1.dp, C.delete),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = C.delete),
            ) { Text(stringResource(R.string.delete), fontWeight = FontWeight.SemiBold) }
        }
    }
    if (picking) {
        DateDialog(date, onDismiss = { picking = false }) {
            date = it
            picking = false
        }
    }
    val removing = dropDays
    if (removing != null) {
        AlertDialog(
            onDismissRequest = { dropDays = null },
            title = { Text(stringResource(R.string.remove_mark)) },
            text = { Text(context.markPhrase(Mark(removing, MarkUnit.DAY))) },
            confirmButton = {
                TextButton(onClick = {
                    val next = cycles.filterNot { it.days == removing }
                    cycles = next
                    dropDays = null
                    if (existing != null) {
                        val trimmed = name.trim().ifEmpty { existing.name }
                        Store.upsert(context, Event(existing.id, trimmed, date, next))
                        Alarms.reschedule(context)
                    }
                }) { Text(stringResource(R.string.delete), color = C.delete) }
            },
            dismissButton = { TextButton(onClick = { dropDays = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(existing.name) },
            confirmButton = {
                TextButton(onClick = {
                    Store.delete(context, existing.id)
                    Alarms.reschedule(context)
                    onDone()
                }) { Text(stringResource(R.string.delete), color = C.delete) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupScreen(
    resumeTick: Int,
    onEvents: () -> Unit,
    language: String,
    onLanguage: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var pickingTime by remember { mutableStateOf(false) }
    var pickingEarly by remember { mutableStateOf(false) }
    var timeVersion by remember { mutableIntStateOf(0) }
    var earlyVersion by remember { mutableIntStateOf(0) }
    val (hour, minute) = remember(resumeTick, timeVersion) { Store.time(context) }
    val early = remember(resumeTick, earlyVersion) { Store.earlyDays(context) }
    val notifications = remember(resumeTick) { Alarms.notificationsOn(context) }
    val alarms = remember(resumeTick) { Alarms.exactAlarmOn(context) }
    val battery = remember(resumeTick) { Alarms.batteryFree(context) }
    val autostart = remember(resumeTick) { Store.autostart(context) }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0" }
    var pickingLanguage by remember { mutableStateOf(false) }
    val languages = listOf(
        "en" to stringResource(R.string.english),
        "vi" to stringResource(R.string.vietnamese),
        "zh" to stringResource(R.string.chinese),
        "ko" to stringResource(R.string.korean),
    )
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.phone_setup), onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            SettingsGroup(stringResource(R.string.events_section), first = true) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onEvents), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.manage_events), color = C.ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("›", color = C.action, fontSize = 24.sp)
                }
            }
            SettingsGroup(stringResource(R.string.display_section)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { pickingLanguage = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.language), modifier = Modifier.weight(1f), color = C.ink, fontWeight = FontWeight.SemiBold)
                    Text(languages.firstOrNull { it.first == language }?.second ?: language, color = C.action, fontWeight = FontWeight.SemiBold)
                }
            }
            SettingsGroup(stringResource(R.string.reminders_section)) {
                SetupRow(stringResource(R.string.notifications), if (notifications) stringResource(R.string.notifications_on) else stringResource(R.string.notifications_off), notifications) {
                    if (it && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Alarms.openNotifications(context)
                    }
                }
                Hairline()
                SetupRow(stringResource(R.string.alarms), stringResource(R.string.alarms_detail), alarms) { Alarms.openExactAlarm(context) }
                Hairline()
                SetupRow(stringResource(R.string.battery), if (battery) stringResource(R.string.battery_on) else stringResource(R.string.battery_off), battery) { Alarms.openBattery(context) }
                Hairline()
                SetupRow(stringResource(R.string.autostart), stringResource(R.string.autostart_detail), autostart) {
                    Store.setAutostart(context, it)
                    Alarms.openAutostart(context)
                }
                Hairline()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { pickingTime = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.time_label), modifier = Modifier.weight(1f), color = C.ink, fontWeight = FontWeight.SemiBold)
                    Text("%02d:%02d".format(LocalConfiguration.current.locales[0], hour, minute), color = C.action, fontWeight = FontWeight.SemiBold)
                }
                Hairline()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { pickingEarly = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.early), modifier = Modifier.weight(1f), color = C.ink, fontWeight = FontWeight.SemiBold)
                    Text(earlyLabel(early), color = C.action, fontWeight = FontWeight.SemiBold)
                }
            }
            SettingsGroup(stringResource(R.string.app_info)) {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painterResource(R.drawable.ic_launcher_mark),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF8FAFC)),
                    )
                    Text(stringResource(R.string.app_name), color = C.ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                    Text(stringResource(R.string.version_label, version), color = C.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    Text(stringResource(R.string.app_about), color = C.muted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp, modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp))
                    Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(1.dp).background(C.line))
                    Text(stringResource(R.string.copyright), color = C.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
    if (pickingLanguage) {
        AlertDialog(
            onDismissRequest = { pickingLanguage = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    languages.forEach { (code, name) ->
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                                pickingLanguage = false
                                onLanguage(code)
                            },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(name, color = if (code == language) C.action else C.ink, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickingLanguage = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (pickingEarly) {
        val choices = listOf(0, 1, 3, 7)
        AlertDialog(
            onDismissRequest = { pickingEarly = false },
            title = { Text(stringResource(R.string.early)) },
            text = {
                Column {
                    choices.forEach { days ->
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                                Store.setEarlyDays(context, days)
                                Alarms.reschedule(context)
                                earlyVersion++
                                pickingEarly = false
                            },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(earlyLabel(days), color = if (days == early) C.action else C.ink, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickingEarly = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (pickingTime) {
        TimeDialog(hour, minute, onDismiss = { pickingTime = false }) { pickedHour, pickedMinute ->
            Store.setTime(context, pickedHour, pickedMinute)
            Alarms.reschedule(context)
            timeVersion++
            pickingTime = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(state.hour, state.minute) }) { Text(stringResource(R.string.use_time)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = { TimePicker(state) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                if (picked != null) onPick(picked) else onDismiss()
            }) { Text(stringResource(R.string.use_date)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(state) }
}

@Composable
private fun SettingsGroup(title: String, first: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = C.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = if (first) 4.dp else 20.dp, bottom = 8.dp))
    Surface(color = C.card, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, C.line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp), content = content)
    }
}

@Composable
private fun Hairline() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
}

@Composable
private fun SetupRow(title: String, detail: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = C.ink, fontWeight = FontWeight.SemiBold)
            if (detail != null) Text(detail, color = C.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = C.button, checkedThumbColor = Color.White),
        )
    }
}

@Composable
private fun TopBar(title: String, onBack: (() -> Unit)? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.back)) }
        }
        Text(
            title,
            modifier = Modifier.weight(1f).padding(start = if (onBack == null) 20.dp else 0.dp, end = 8.dp),
            color = C.ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (action != null) action()
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp).height(48.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = C.button, contentColor = Color.White, disabledContainerColor = C.button.copy(alpha = 0.4f), disabledContentColor = Color.White),
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun CheerOverlay(hits: List<Pair<Event, Hit>>, onDone: () -> Unit) {
    val context = LocalContext.current
    val embers = remember { celebrationEmbers() }
    val timeline = remember { CelebrationTimeline() }
    LaunchedEffect(Unit) {
        do {
            withFrameNanos { timeline.onFrame(it) }
        } while (timeline.elapsed < 5.5f)
        onDone()
    }
    val elapsed = timeline.elapsed
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF29434F), Color(0xFF101E29)))).clickable(onClick = onDone)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCelebration(elapsed, embers)
            drawBeerToast(elapsed)
        }
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().heightIn(max = 150.dp).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).alpha(((elapsed - 0.85f) / 0.22f).coerceIn(0f, 1f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Cheers!", color = Color(0xFFF4D69E), fontSize = 46.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
            hits.forEach { (event, hit) ->
                Text(event.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                Text(context.hitPhrase(hit), color = Color(0xFFC8DFE6), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Text(stringResource(R.string.tap_continue), color = Color(0xFFA3B4BF), fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCard(event: Event, hit: Hit, today: LocalDate, lead: LocalDate?, onClick: () -> Unit, onHold: () -> Unit) {
    val context = LocalContext.current
    val days = ChronoUnit.DAYS.between(today, hit.date).coerceAtLeast(0)
    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onHold),
        shape = RoundedCornerShape(26.dp), color = Color(0xFF233F49),
    ) {
        Box {
        Canvas(Modifier.matchParentSize()) { drawMilestoneCardArt() }
        Column(Modifier.padding(24.dp)) {
            Text(stringResource(R.string.nearest_milestone), color = Color(0xFFE7CF9E), fontSize = 10.sp, letterSpacing = 1.6.sp)
            Text(event.name, color = Color(0xFFFFF8E9), fontSize = 24.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp))
            Text(stringResource(R.string.celebrating_mark, context.hitPhrase(hit)), color = Color(0xFFBBCDCE), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.padding(vertical = 22.dp), verticalAlignment = Alignment.Bottom) {
                Text(if (days == 0L) stringResource(R.string.today) else String.format(LocalConfiguration.current.locales[0], "%,d", days), color = Color(0xFFF5DBAB), fontSize = if (days == 0L) 36.sp else 64.sp, fontFamily = FontFamily.Serif, lineHeight = 68.sp)
                if (days > 0) Text(stringResource(R.string.days_to_go), color = Color(0xFFE0E9E5), fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp, bottom = 9.dp))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x33FFFFFF)))
            Text(formatDate(hit.date), color = Color(0xFFD8E2DF), fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
            if (lead != null) Text(stringResource(R.string.early_when, formatDate(lead)), color = Color(0xFFE7CF9E), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LaterRow(event: Event, hit: Hit, today: LocalDate, lead: LocalDate?, onClick: () -> Unit, onHold: () -> Unit) {
    val context = LocalContext.current
    val days = ChronoUnit.DAYS.between(today, hit.date).coerceAtLeast(0)
    Surface(Modifier.fillMaxWidth().padding(bottom = 9.dp).combinedClickable(onClick = onClick, onLongClick = onHold), shape = RoundedCornerShape(18.dp), color = C.card, border = BorderStroke(1.dp, C.line)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(event.name, color = C.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(context.hitPhrase(hit) + " · " + formatDate(hit.date), color = C.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                if (lead != null) Text(stringResource(R.string.early_when, formatDate(lead)), color = C.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(String.format(LocalConfiguration.current.locales[0], "%,d", days), color = C.action, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.days_to_go), color = C.muted, fontSize = 10.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SinceRow(event: Event, days: Long, onClick: () -> Unit, onHold: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(bottom = 9.dp).combinedClickable(onClick = onClick, onLongClick = onHold), shape = RoundedCornerShape(18.dp), color = C.card, border = BorderStroke(1.dp, C.line)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(event.name, color = C.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.since_date, formatDate(event.start)), color = C.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(String.format(LocalConfiguration.current.locales[0], "%,d", days), color = C.action, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.days_together), color = C.muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun earlyLabel(days: Int): String = when (days) {
    1 -> stringResource(R.string.early_1)
    3 -> stringResource(R.string.early_3)
    7 -> stringResource(R.string.early_7)
    else -> stringResource(R.string.early_off)
}

@Composable
private fun remainLabel(today: LocalDate, date: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days <= 0L -> stringResource(R.string.today)
        days == 1L -> stringResource(R.string.tomorrow)
        else -> stringResource(R.string.in_days, days.toInt())
    }
}

@Composable
private fun EventsScreen(onBack: () -> Unit, onAdd: () -> Unit, onEdit: (Event) -> Unit) {
    val context = LocalContext.current
    val events = Store.events(context)
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.manage_events), onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            if (events.isEmpty()) Text(stringResource(R.string.no_dates), color = C.ink)
            events.forEach { event ->
                Surface(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onEdit(event) }, shape = RoundedCornerShape(18.dp), color = C.card, border = BorderStroke(1.dp, C.line)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(event.name, color = C.ink, fontWeight = FontWeight.SemiBold)
                        Text(formatDate(event.start), color = C.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        PrimaryButton(stringResource(R.string.add_date), onClick = onAdd)
    }
}
