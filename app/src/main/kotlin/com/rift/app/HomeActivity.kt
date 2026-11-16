package com.rift.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rift.core.designsystem.theme.RiftTheme
import com.rift.data.widgets.WidgetHostManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    @Inject
    lateinit var widgetHostManager: WidgetHostManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RiftTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent,
                ) {
                    RiftLauncherRoot()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHostManager.onStart()
    }

    override fun onStop() {
        super.onStop()
        widgetHostManager.onStop()
    }
}
