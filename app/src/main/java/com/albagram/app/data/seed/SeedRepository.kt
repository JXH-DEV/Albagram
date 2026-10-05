package com.albagram.app.data.seed

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedRepository @Inject constructor(
    private val seeder: DatabaseSeeder
) {
    private val _state = MutableStateFlow<SeedState>(SeedState.Loading)
    val state: StateFlow<SeedState> = _state.asStateFlow()

    suspend fun ensureSeeded() {
        _state.value = SeedState.Loading
        try {
            withTimeout(30_000) {
                seeder.seedIfNeeded()
            }
            _state.value = SeedState.Ready
        } catch (e: TimeoutCancellationException) {
            _state.value = SeedState.Failed("Ngarkimi zgjati shumë. Provo përsëri.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = SeedState.Failed("Përmbajtja nuk u ngarkua. Provo përsëri.")
        }
    }

    suspend fun retry() {
        ensureSeeded()
    }
}
