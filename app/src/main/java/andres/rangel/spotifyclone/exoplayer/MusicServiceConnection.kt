package andres.rangel.spotifyclone.exoplayer

import andres.rangel.spotifyclone.utils.Constants.NETWORK_ERROR
import andres.rangel.spotifyclone.utils.Event
import andres.rangel.spotifyclone.utils.Resource
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.MutableLiveData

class MusicServiceConnection(
    context: Context
) {

    val isConnected = MutableLiveData<Event<Resource<Boolean>>>()
    val networkError = MutableLiveData<Event<Resource<Boolean>>>()
    val playbackState = MutableLiveData<PlaybackStateCompat?>()
    val currentPlayingSong = MutableLiveData<MediaMetadataCompat?>()

    lateinit var mediaController: MediaControllerCompat

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context, MusicService::class.java),
        mediaBrowserConnectionCallback,
        null
    ).apply { connect() }

    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            isConnected.postValue(
                Event(
                    Resource.success(true)
                )
            )
        }

        override fun onConnectionSuspended() {
            isConnected.postValue(
                Event(
                    Resource.error("The connection was suspended", false)
                )
            )
        }

        override fun onConnectionFailed() {
            isConnected.postValue(
                Event(
                    Resource.error("Couldn't connect to media browser", false)
                )
            )
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState.postValue(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            currentPlayingSong.postValue(metadata)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when (event) {
                NETWORK_ERROR -> networkError.postValue(
                    Event(
                        Resource.error(
                            "Couldn't connect to the server. Please check your internet connection.",
                            null
                        )
                    )
                )
            }
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}