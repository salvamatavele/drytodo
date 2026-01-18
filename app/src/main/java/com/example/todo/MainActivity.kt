package com.example.todo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.example.todo.notification.MaintenanceWorker
import com.example.todo.ui.DryTodoApp
import com.example.todo.ui.TaskViewModel
import com.example.todo.ui.theme.ToDOTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Configure Background Maintenance on every start (WorkManager handles duplicates)
        setupBackgroundWork(this)

        setContent {
            ToDOTheme {
                val viewModel: TaskViewModel = viewModel()
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    // 1. Request Notification Permission (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    
                    // 2. Request Battery Optimization Exemption for first time
                    requestBatteryOptimizationExemption(this@MainActivity)
                }

                DryTodoApp(viewModel)
            }
        }
    }

    private fun setupBackgroundWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val maintenanceWork = PeriodicWorkRequestBuilder<MaintenanceWorker>(
            1, TimeUnit.HOURS // Runs every hour to keep the agenda "Intelligent"
        )
            .setConstraints(constraints)
            .addTag("agenda_maintenance")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "AgendaMaintenance",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing one if already scheduled
            maintenanceWork
        )
    }

    private fun requestBatteryOptimizationExemption(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName
        
        // Only ask if not already exempted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general settings if direct request is blocked
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                }
            }
        }
    }
}
