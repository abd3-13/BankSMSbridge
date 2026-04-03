package com.abdelah.banksms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.abdelah.banksms.db.AppDatabase
import com.abdelah.banksms.logging.RuntimeLogStore
import com.abdelah.banksms.parser.SmsParsePlugin
import com.abdelah.banksms.sync.SyncConfig
import com.abdelah.banksms.sync.SyncScheduler
import com.abdelah.banksms.ui.theme.BankSMSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DashboardTab { Home, Logs, Settings }
private data class BankItemUi(val bankName: String, val smsCount: Int, val isActive: Boolean)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                1
            )
        }

        enableEdgeToEdge()
        SyncScheduler.ensurePeriodic(this)

        setContent {
            BankSMSTheme(darkTheme = true, dynamicColor = false) {
                DashboardScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun DashboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var fireflyUrl by remember { mutableStateOf("") }
    var fireflyToken by remember { mutableStateOf("") }
    var retryInterval by remember { mutableStateOf("15") }
    var parserPluginsJson by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf("") }
    var isPluginsExpanded by remember { mutableStateOf(false) }
    var isLogViewerExpanded by remember { mutableStateOf(false) }

    var totalParsed by remember { mutableIntStateOf(0) }
    var totalSynced by remember { mutableIntStateOf(0) }
    var totalFailed by remember { mutableIntStateOf(0) }
    var bankItems by remember { mutableStateOf(emptyList<BankItemUi>()) }
    var selectedTab by remember { mutableStateOf(DashboardTab.Home) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        val settings = SyncConfig.load(context)
        fireflyUrl = settings.baseUrl
        fireflyToken = settings.token
        retryInterval = settings.retryIntervalMinutes.toString()
        parserPluginsJson = settings.parserPluginsJson

        val dao = AppDatabase.getDatabase(context).transactionDao()
        totalParsed = withContext(Dispatchers.IO) { dao.countAll() }
        totalSynced = withContext(Dispatchers.IO) { dao.countByStatus("SENT") }
        totalFailed = withContext(Dispatchers.IO) { dao.countByStatus("FAILED") }

        val allTransactions = withContext(Dispatchers.IO) {
            dao.getByStatuses(listOf("PENDING", "SENT", "FAILED"))
        }
        val transactionCountByBank = allTransactions.groupingBy { it.bank }.eachCount()
        val configuredPlugins = SmsParsePlugin.listFromJson(parserPluginsJson).orEmpty()
        bankItems = configuredPlugins.map { plugin ->
            val smsCount = transactionCountByBank[plugin.bankName] ?: 0
            BankItemUi(
                bankName = plugin.bankName,
                smsCount = smsCount,
                isActive = smsCount > 0
            )
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.Home,
                    onClick = { selectedTab = DashboardTab.Home },
                    icon = { Text("⌂") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.Logs,
                    onClick = { selectedTab = DashboardTab.Logs },
                    icon = { Text("≡") },
                    label = { Text("Logs") }
                )
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.Settings,
                    onClick = { selectedTab = DashboardTab.Settings },
                    icon = { Text("⚙") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            DashboardTab.Home -> HomeTabContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                totalParsed = totalParsed,
                totalSynced = totalSynced,
                totalFailed = totalFailed,
                pluginCount = bankItems.size,
                bankItems = bankItems,
                onRefresh = {
                    saveMessage = "Stats refreshed"
                    refreshTrigger++
                    scope.launch {
                        delay(5000)
                        saveMessage = ""
                    }
                }
            )

            DashboardTab.Logs -> LogsTabContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                isExpanded = isLogViewerExpanded,
                onToggle = { isLogViewerExpanded = !isLogViewerExpanded }
            )

            DashboardTab.Settings -> SettingsTabContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                fireflyUrl = fireflyUrl,
                onFireflyUrlChange = { fireflyUrl = it },
                fireflyToken = fireflyToken,
                onFireflyTokenChange = { fireflyToken = it },
                retryInterval = retryInterval,
                onRetryIntervalChange = { retryInterval = it.filter(Char::isDigit) },
                parserPluginsJson = parserPluginsJson,
                onParserPluginsJsonChange = { parserPluginsJson = it },
                isPluginsExpanded = isPluginsExpanded,
                onTogglePlugins = { isPluginsExpanded = !isPluginsExpanded },
                saveMessage = saveMessage,
                onSave = {
                    val retry = retryInterval.toLongOrNull() ?: 15L
                    val plugins = SmsParsePlugin.listFromJson(parserPluginsJson)
                    if (plugins == null || plugins.isEmpty()) {
                        saveMessage = "Parser plugins JSON is invalid or empty"
                    } else {
                        SyncConfig.save(
                            context = context,
                            baseUrl = fireflyUrl,
                            token = fireflyToken,
                            retryIntervalMinutes = retry,
                            parserPluginsJson = SmsParsePlugin.listToJson(plugins)
                        )
                        SyncScheduler.reconfigurePeriodic(context)
                        saveMessage = "Settings saved"
                        refreshTrigger++
                        scope.launch {
                            delay(5000)
                            saveMessage = ""
                        }
                    }
                },
                onSyncNow = {
                    SyncScheduler.enqueueImmediate(context)
                    saveMessage = "Sync started"
                    scope.launch {
                        delay(5000)
                        saveMessage = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeTabContent(
    modifier: Modifier = Modifier,
    totalParsed: Int,
    totalSynced: Int,
    totalFailed: Int,
    pluginCount: Int,
    bankItems: List<BankItemUi>,
    onRefresh: () -> Unit
) {
    val totalSms = totalParsed
    val stats = listOf(
        "Total SMS" to totalSms,
        "Parsed SMS" to totalParsed,
        "Failed SMS" to totalFailed,
        "Synced SMS" to totalSynced,
        "Plugin count" to pluginCount
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(stats) { (label, value) ->
                    StatGridCard(title = label, value = value.toString())
                }
            }
        }
        item {
            Button(onClick = onRefresh) {
                Text("Refresh stats")
            }
        }
        item {
            Text("Banks", style = MaterialTheme.typography.titleLarge)
        }
        if (bankItems.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Text(
                        text = "No banks configured",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(bankItems) { bank ->
                BankCard(bank)
            }
        }
    }
}

@Composable
private fun LogsTabContent(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Logs", style = MaterialTheme.typography.headlineMedium)
        LogViewer(isExpanded = isExpanded, onToggle = onToggle)
    }
}

@Composable
private fun SettingsTabContent(
    modifier: Modifier = Modifier,
    fireflyUrl: String,
    onFireflyUrlChange: (String) -> Unit,
    fireflyToken: String,
    onFireflyTokenChange: (String) -> Unit,
    retryInterval: String,
    onRetryIntervalChange: (String) -> Unit,
    parserPluginsJson: String,
    onParserPluginsJsonChange: (String) -> Unit,
    isPluginsExpanded: Boolean,
    onTogglePlugins: () -> Unit,
    saveMessage: String,
    onSave: () -> Unit,
    onSyncNow: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider()

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSave) { Text("Save Settings") }
            Button(onClick = onSyncNow) { Text("Sync now") }
        }

        if (saveMessage.isNotBlank()) {
            Text(saveMessage, color = MaterialTheme.colorScheme.primary)
        }

        OutlinedTextField(
            value = fireflyUrl,
            onValueChange = onFireflyUrlChange,
            label = { Text("Firefly Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = fireflyToken,
            onValueChange = onFireflyTokenChange,
            label = { Text("Firefly API Token") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = retryInterval,
            onValueChange = onRetryIntervalChange,
            label = { Text("Retry interval (minutes, min 15)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        TextButton(onClick = onTogglePlugins) {
            Text(if (isPluginsExpanded) "Hide parser plugins" else "Show parser plugins")
        }

        if (isPluginsExpanded) {
            OutlinedTextField(
                value = parserPluginsJson,
                onValueChange = onParserPluginsJsonChange,
                label = { Text("Parser plugins JSON") },
                supportingText = { Text("Each plugin defines sender, hints, debit/credit regex and optional refs") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LogViewer(isExpanded: Boolean, onToggle: () -> Unit) {
    val logs by RuntimeLogStore.logs.collectAsState()
    val logScrollState = rememberScrollState()

    LaunchedEffect(isExpanded, logs.size) {
        if (isExpanded) {
            logScrollState.scrollTo(logScrollState.maxValue)
        }
    }

    TextButton(onClick = onToggle) {
        Text(if (isExpanded) "Hide runtime logs" else "Show runtime logs")
    }

    if (!isExpanded) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Runtime Logs", style = MaterialTheme.typography.titleSmall)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(logScrollState)
                    .padding(10.dp)
            ) {
                Text(
                    text = if (logs.isEmpty()) "No runtime logs yet" else logs.joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun StatGridCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun BankCard(bank: BankItemUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(bank.bankName, style = MaterialTheme.typography.titleMedium)
            Text("SMS count: ${bank.smsCount}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (bank.isActive) "Status: Active" else "Status: Disabled",
                color = if (bank.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}
