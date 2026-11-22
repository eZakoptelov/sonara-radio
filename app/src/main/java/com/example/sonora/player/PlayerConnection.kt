package com.example.sonora.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.session.MediaController

object PlayerConnection {
    var controller: MediaController? by mutableStateOf(null)
}
