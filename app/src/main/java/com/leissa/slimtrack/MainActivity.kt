package com.leissa.slimtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.leissa.slimtrack.ui.SlimTrackRoot
import com.leissa.slimtrack.ui.theme.SlimTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlimTrackTheme {
                SlimTrackRoot()
            }
        }
    }
}
