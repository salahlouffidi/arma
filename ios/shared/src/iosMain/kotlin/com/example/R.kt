package com.example

/** iOS counterpart of the Android R class for the raw audio/video files bundled with the app. */
object R {
    object raw {
        const val arma_snake = 0x7f0e0001
        const val stage_transition = 0x7f0e0002
        const val louffi_edition_logo_intro_animation_202609082217 = 0x7f0e0003
    }

    /** Raw resource id -> (file name, extension) inside the iOS app bundle. */
    internal fun rawFile(id: Int): Pair<String, String>? = when (id) {
        raw.arma_snake -> "arma_snake" to "mp3"
        raw.stage_transition -> "stage_transition" to "mp3"
        raw.louffi_edition_logo_intro_animation_202609082217 -> "louffi_edition_logo_intro_animation_202609082217" to "mp4"
        else -> null
    }
}
