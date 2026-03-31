package com.masterpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.masterpaper.ui.navigation.NavGraph
import com.masterpaper.ui.theme.MasterPaperTheme
import kotlinx.coroutines.flow.combine

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = application as MasterPaperApp
            
            val systemDarkTheme = isSystemInDarkTheme()
            var shouldUseDarkTheme by remember { mutableStateOf(systemDarkTheme) }
            
            LaunchedEffect(app) {
                combine(
                    app.preferencesRepository.isFollowSystem,
                    app.preferencesRepository.isDarkMode
                ) { followSystem, darkMode ->
                    if (followSystem) systemDarkTheme else darkMode
                }.collect { useDark ->
                    shouldUseDarkTheme = useDark
                }
            }

            MasterPaperTheme(darkTheme = shouldUseDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph()
                }
            }
        }
    }
}
