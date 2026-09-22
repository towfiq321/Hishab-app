package com.hishab.finance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hishab.finance.AppContainer

/** The dependency container, handed down the tree once from MainActivity. */
val LocalContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided. Wrap the tree in CompositionLocalProvider(LocalContainer provides ...).")
}

/**
 * Creates a ViewModel with the container (and any screen arguments) in scope, without pulling in
 * a DI framework. [key] keeps per-argument instances apart, e.g. one per income source.
 */
@Composable
inline fun <reified VM : ViewModel> rememberViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM
): VM {
    val container = LocalContainer.current
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container) } }
    )
}
