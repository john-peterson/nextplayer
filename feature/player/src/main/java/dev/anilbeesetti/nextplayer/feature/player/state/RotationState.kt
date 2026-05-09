package dev.anilbeesetti.nextplayer.feature.player.state

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Matrix
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.util.Consumer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.listen
// import androidx.media3.common.Matrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.MatrixTransformation
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.Transformer
import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.model.ScreenOrientation
import dev.anilbeesetti.nextplayer.feature.player.extensions.isPortrait

@UnstableApi
@Composable
fun rememberRotationState(
    player: Player,
    screenOrientation: ScreenOrientation,
): RotationState {
    val activity = LocalActivity.current as ComponentActivity
    val rotationState = remember {
        RotationState(
            activity = activity,
            player = player,
            screenOrientation = screenOrientation,
        )
    }
    DisposableEffect(activity) {
        rotationState.handleListeners(this)
    }
    LaunchedEffect(player) { rotationState.observe() }
    return rotationState
}

@Stable
class RotationState(
    private val activity: ComponentActivity,
    private val player: Player,
    private val screenOrientation: ScreenOrientation,
    private var landscapeHint: Boolean = true,
    private var flip: Boolean = false,
) {
    var currentRequestedOrientation: Int by mutableIntStateOf(activity.requestedOrientation)
        private set

    fun rotateLong() {
        activity.requestedOrientation = when (activity.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        if (landscapeHint) {
            val text = "long press to reverse landscape "
            Toast.makeText(activity, text, 0).show()
            landscapeHint = false
        }
    }

    fun rotate() {
        try {
            val exoPlayer = player as ExoPlayer
        } catch (e: Exception) {
            Logger.logDebug("Rotate", e.toString())
        }

        // Logger.logError("mytag", player::class.qualifiedName+"")
        // val exoPlayer = player as? ExoPlayer
        // if (exoPlayer == null){
        //     Toast.makeText(activity, "exo player null", 0).show()
        //     return
        // }

// if (exoPlayer != null) {
    // Access ExoPlayer-specific methods like createMessage()
// }
        if (flip) {
            val mediaItem = player.currentMediaItem ?: return
            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(listOf(), emptyList())) // audioEffects, videoEffects
            // .setVideoEffects(emptyList())
            .build()
            player.setMediaItem(editedMediaItem.mediaItem)
            // exoPlayer.setVideoEffects(emptyList())
        } else {
            val transformationMatrix = Matrix()
            transformationMatrix.postScale(-1f, 1f)
            val flipEffect = MatrixTransformation { _: Long -> transformationMatrix }
            // val effects = listOf(MatrixTransformation { _: Long -> transformationMatrix })
            
            val brightnessEffect = Brightness(0.5f) // Increase brightness
            val effects = listOf(brightnessEffect, flipEffect)
            // Applying to an ExoPlayer instance
            // exoPlayer.setVideoEffects(videoEffects)

            // exoPlayer.setVideoEffects(effects)
            
            val mediaItem = player.currentMediaItem ?: return
            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(listOf(), effects)) // audioEffects, videoEffects
            // .setVideoEffects(effects)
            .build()
            player.setMediaItem(editedMediaItem.mediaItem)

            Toast.makeText(activity, "flip video", 0).show()
        }
        flip = !flip
    }

    fun handleListeners(disposableEffectScope: DisposableEffectScope): DisposableEffectResult = with(disposableEffectScope) {
        val configurationChangedListener: Consumer<Configuration> = Consumer {
            currentRequestedOrientation = activity.requestedOrientation
        }

        activity.addOnConfigurationChangedListener(configurationChangedListener)

        onDispose {
            activity.removeOnConfigurationChangedListener(configurationChangedListener)
        }
    }

    suspend fun observe() {
        setOrientation()
        player.listen { events ->
            if (events.contains(Player.EVENT_VIDEO_SIZE_CHANGED)) {
                if (screenOrientation == ScreenOrientation.VIDEO_ORIENTATION) {
                    activity.requestedOrientation = getVideoBasedOrientation()
                }
            }
        }
    }

    private fun setOrientation() {
        if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            activity.requestedOrientation = when (screenOrientation) {
                ScreenOrientation.AUTOMATIC -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ScreenOrientation.LANDSCAPE_REVERSE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                ScreenOrientation.LANDSCAPE_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                ScreenOrientation.VIDEO_ORIENTATION -> getVideoBasedOrientation()
            }
        }
    }

    private fun getVideoBasedOrientation() = when {
        player.videoSize.width == 0 || player.videoSize.height == 0 -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.videoSize.isPortrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
}
