package com.albagram.app.data.seed

sealed class SeedState {
    data object Loading : SeedState()
    data object Ready : SeedState()
    data class Failed(val message: String) : SeedState()
}
