package com.portfolio.tracker.feature.portfolio

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Entry point for the portfolio dashboard.
 *
 * Intentionally blank: this exists to prove :app -> :feature:portfolio ->
 * :core:* wiring compiles and renders. The dashboard itself is built later,
 * against /docs/design/screens.md.
 */
@Composable
fun PortfolioRoute(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Blank screen.
        }
    }
}
