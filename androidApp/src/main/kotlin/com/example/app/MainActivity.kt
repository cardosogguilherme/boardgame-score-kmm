package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ui.DeepSeaScreenA

/** Launches Screen A (Deep Sea score entry). All UI lives in :ui; this is just the host. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeepSeaScreenA()
        }
    }
}
