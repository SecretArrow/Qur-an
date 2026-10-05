package id.secretarrow.alquran.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.secretarrow.alquran.di.AppContainer

/** Factory generik untuk ViewModel yang butuh AppContainer. */
class AppViewModelFactory(
    private val container: AppContainer,
    private val creator: (AppContainer) -> ViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator(container) as T
}

inline fun <reified VM : ViewModel> appViewModelFactory(
    container: AppContainer,
    noinline creator: (AppContainer) -> ViewModel
): ViewModelProvider.Factory = AppViewModelFactory(container, creator)
