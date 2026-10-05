package com.albagram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.albagram.app.data.seed.SeedRepository
import com.albagram.app.data.seed.SeedState
import com.albagram.app.ui.navigation.AlbagramNavHost
import com.albagram.app.ui.theme.AlbagramTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var seedRepository: SeedRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition {
            seedRepository.state.value is SeedState.Loading
        }
        enableEdgeToEdge()
        setContent {
            AlbagramTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlbagramNavHost()
                }
            }
        }
    }
}
