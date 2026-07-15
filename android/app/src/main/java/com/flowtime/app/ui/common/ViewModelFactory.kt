package com.flowtime.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flowtime.app.FlowtimeApp
import com.flowtime.app.data.repository.FlowtimeRepository

/**
 * Builds a [androidx.lifecycle.ViewModelProvider.Factory] that hands the shared
 * [FlowtimeRepository] (from the Application's AppContainer) to a ViewModel.
 */
inline fun <reified VM : ViewModel> repoViewModelFactory(
    crossinline create: (FlowtimeRepository) -> VM,
) = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as FlowtimeApp
        create(app.container.repository)
    }
}
