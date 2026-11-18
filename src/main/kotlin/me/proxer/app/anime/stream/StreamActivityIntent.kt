package me.proxer.app.anime.stream

import android.content.Intent
import androidx.core.content.IntentCompat
import android.net.Uri
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.AD_TAG_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.COVER_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.EPISODE_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.ID_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.INTERNAL_PLAYER_ONLY_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.LANGUAGE_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.NAME_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.REFERER_EXTRA
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.util.ProxerUrls.hasProxerStreamFileHost

/**
 * Typed access to the intent extras of [StreamActivity].
 *
 * @author Ruben Gees
 */
internal class StreamActivityIntent(
    private val intent: Intent,
) {
    companion object {
        private const val CLOUDFLARE_HOST = "videodelivery.net"
    }

    val id: String
        get() = intent.getSafeStringExtra(ID_EXTRA)

    val name: String
        get() = intent.getSafeStringExtra(NAME_EXTRA)

    val episode: Int
        get() = intent.getIntExtra(EPISODE_EXTRA, -1).let { if (it <= 0) 1 else it }

    val language: AnimeLanguage
        get() = requireNotNull(IntentCompat.getSerializableExtra(intent, LANGUAGE_EXTRA, AnimeLanguage::class.java))

    val coverUri: Uri?
        get() = IntentCompat.getParcelableExtra(intent, COVER_EXTRA, Uri::class.java)

    val referer: String?
        get() = intent.getStringExtra(REFERER_EXTRA)

    val uri: Uri
        get() = requireNotNull(intent.data)

    val isProxerStream: Boolean
        get() {
            val url = intent.dataString?.toPrefixedUrlOrNull()

            return url != null && (url.hasProxerStreamFileHost || url.host == CLOUDFLARE_HOST)
        }

    val mimeType: String
        get() = requireNotNull(intent.type)

    val isInternalPlayerOnly: Boolean
        get() = intent.getBooleanExtra(INTERNAL_PLAYER_ONLY_EXTRA, false)

    val adTag: Uri?
        get() = IntentCompat.getParcelableExtra(intent, AD_TAG_EXTRA, Uri::class.java)
}
