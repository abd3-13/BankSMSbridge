package com.abdelah.banksms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.abdelah.banksms.db.AppDatabase
import com.abdelah.banksms.parser.SmsParsePlugin
import com.abdelah.banksms.sync.SyncConfig
import com.abdelah.banksms.sync.SyncScheduler
import com.abdelah.banksms.ui.theme.BankSMSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            BankSMSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var fireflyUrl by remember { mutableStateOf("") }
    var fireflyToken by remember { mutableStateOf("") }
    var retryInterval by remember { mutableStateOf("15") }
    var parserPluginsJson by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf("") }

    var totalParsed by remember { mutableIntStateOf(0) }
    var totalSynced by remember { mutableIntStateOf(0) }
    var totalFailed by remember { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

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
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Bank SMS Bridge Settings", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = fireflyUrl,
            onValueChange = { fireflyUrl = it },
            label = { Text("Firefly Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = fireflyToken,
            onValueChange = { fireflyToken = it },
            label = { Text("Firefly API Token") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = retryInterval,
            onValueChange = { retryInterval = it.filter(Char::isDigit) },
            label = { Text("Retry interval (minutes, min 15)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = parserPluginsJson,
            onValueChange = { parserPluginsJson = it },
            label = { Text("Parser plugins JSON") },
            supportingText = { Text("Each plugin defines sender, hints, debit/credit regex and optional refs") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val retry = retryInterval.toLongOrNull() ?: 15L
                    val plugins = SmsParsePlugin.listFromJson(parserPluginsJson)
                    if (plugins == null || plugins.isEmpty()) {
                        saveMessage = "Parser plugins JSON is invalid or empty"
                        return@Button
                    }

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
                }
            ) {
                Text("Save")
            }

            Button(
                onClick = {
                    saveMessage = "Stats refreshed"
                    refreshTrigger++
                }
            ) {
                Text("Refresh stats")
            }
        }

        if (saveMessage.isNotBlank()) {
            Text(saveMessage, color = MaterialTheme.colorScheme.primary)
        }

        Text("Sync Stats", style = MaterialTheme.typography.titleMedium)
        StatsCard(totalParsed = totalParsed, totalSynced = totalSynced, totalFailed = totalFailed)
    }
}

@Composable
private fun StatsCard(totalParsed: Int, totalSynced: Int, totalFailed: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Total parsed:")
                Text(totalParsed.toString())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Synced:")
                Text(totalSynced.toString())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Failed:")
                Text(totalFailed.toString())
            }
        }
    }
}
