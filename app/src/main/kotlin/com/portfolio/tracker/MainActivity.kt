package com.portfolio.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.portfolio.tracker.feature.portfolio.PortfolioRoute
import com.portfolio.tracker.ui.theme.PortfolioTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PortfolioTrackerTheme {
                PortfolioRoute()
            }
        }
    }
}
