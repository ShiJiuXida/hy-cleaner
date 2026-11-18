package com.sabac.hy.cleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sabac.hy.cleaner.ui.SettingsScreen
import com.sabac.hy.cleaner.ui.HyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HyTheme {
                SettingsScreen()
            }
        }
    }
}
