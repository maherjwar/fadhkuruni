package com.example.fadhkuruni

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdhkarTheme {
                // تخطيط من اليمين إلى اليسار للعربية
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AdhkarScreen()
                }
            }
        }
    }
}

/** تحويل الأرقام إلى الأرقام العربية الهندية (٠١٢...). */
fun arabicNumber(n: Int, minDigits: Int = 1): String =
    n.toString().padStart(minDigits, '0')
        .map { c -> if (c.isDigit()) '٠' + (c - '0') else c }
        .joinToString("")

fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "ص" else "م"
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "${arabicNumber(h12)}:${arabicNumber(minute, 2)} $period"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarScreen() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(Category.MORNING) }

    // العدادات محفوظة لليوم الحالي ومُستعادة عند فتح التطبيق
    val counts = remember {
        mutableStateMapOf<String, Int>().apply { putAll(CounterStore.load(context)) }
    }
    var showSettings by remember { mutableStateOf(false) }

    val setCount: (String, Int) -> Unit = { key, value ->
        counts[key] = value
        CounterStore.save(context, key, value)
    }

    val dhikrs = selected.items
    val doneCount = dhikrs.indices.count { i ->
        (counts["${selected.name}-$i"] ?: 0) >= dhikrs[i].repeat
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("فاذكروني أذكركم", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    TextButton(onClick = { showSettings = true }) { Text("الإعدادات") }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Category.entries) { category ->
                    FilterChip(
                        selected = category == selected,
                        onClick = { selected = category },
                        label = { Text(category.title) },
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    text = "تم ${arabicNumber(doneCount)} من ${arabicNumber(dhikrs.size)}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (dhikrs.isEmpty()) 0f else doneCount.toFloat() / dhikrs.size },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(dhikrs) { index, dhikr ->
                    val key = "${selected.name}-$index"
                    val done = counts[key] ?: 0
                    DhikrCard(
                        dhikr = dhikr,
                        done = done,
                        onTap = { if (done < dhikr.repeat) setCount(key, done + 1) },
                        onReset = { setCount(key, 0) },
                    )
                }
            }
        }
    }

    if (showSettings) {
        ReminderSettingsDialog(onDismiss = { showSettings = false })
    }
}

@Composable
fun DhikrCard(
    dhikr: Dhikr,
    done: Int,
    onTap: () -> Unit,
    onReset: () -> Unit,
) {
    val finished = done >= dhikr.repeat
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !finished, onClick = onTap),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (finished) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = dhikr.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 21.sp, lineHeight = 40.sp),
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { done.toFloat() / dhikr.repeat },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (finished) "تم ✓" else "${arabicNumber(done)} / ${arabicNumber(dhikr.repeat)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onReset) { Text("إعادة") }
            }
        }
    }
}

@Composable
fun ReminderSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val configs = remember {
        mutableStateListOf<ReminderConfig>().apply { addAll(Reminders.load(context)) }
    }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* إن رُفض الإذن فلن تظهر الإشعارات */ }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعدادات التذكير", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                configs.forEachIndexed { index, config ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(config.category.title, style = MaterialTheme.typography.titleMedium)
                            TextButton(
                                onClick = { editingIndex = index },
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text("الوقت: ${formatTime(config.hour, config.minute)}")
                            }
                        }
                        Switch(
                            checked = config.enabled,
                            onCheckedChange = { on ->
                                if (on && Build.VERSION.SDK_INT >= 33) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                val updated = config.copy(enabled = on)
                                configs[index] = updated
                                Reminders.save(context, updated)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("تم") } },
    )

    editingIndex?.let { index ->
        val config = configs[index]
        TimePickerDialog(
            initialHour = config.hour,
            initialMinute = config.minute,
            onConfirm = { hour, minute ->
                val updated = config.copy(hour = hour, minute = minute)
                configs[index] = updated
                Reminders.save(context, updated)
                editingIndex = null
            },
            onDismiss = { editingIndex = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = { TimePicker(state = state) },
    )
}
