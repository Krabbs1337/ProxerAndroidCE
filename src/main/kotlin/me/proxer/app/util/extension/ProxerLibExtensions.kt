@file:Suppress("MethodOverloading")

package me.proxer.app.util.extension

import android.content.Context
import android.content.res.Resources
import androidx.appcompat.content.res.AppCompatResources
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import me.proxer.app.R
import me.proxer.app.R.id.description
import me.proxer.app.R.id.post
import me.proxer.app.anime.AnimeStream
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.comment.LocalComment
import me.proxer.app.forum.ParsedPost
import me.proxer.app.forum.TopicMetaData
import me.proxer.app.media.LocalTag
import me.proxer.app.media.comments.ParsedComment
import me.proxer.app.profile.comment.ParsedUserComment
import me.proxer.app.profile.history.LocalUserHistoryEntry
import me.proxer.app.profile.media.LocalUserMediaListEntry
import me.proxer.app.profile.settings.LocalProfileSettings
import me.proxer.app.profile.topten.LocalTopTenEntry
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.toBBTree
import me.proxer.app.ui.view.bbcode.toSimpleBBTree
import me.proxer.library.entity.anime.Stream
import me.proxer.library.entity.forum.Post
import me.proxer.library.entity.forum.Topic
import me.proxer.library.entity.info.Comment
import me.proxer.library.entity.info.Entry
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.entity.info.EntrySeasonInfo
import me.proxer.library.entity.info.Synonym
import me.proxer.library.entity.list.Tag
import me.proxer.library.entity.manga.Chapter
import me.proxer.library.entity.manga.Page
import me.proxer.library.entity.ucp.UcpHistoryEntry
import me.proxer.library.entity.ucp.UcpSettings
import me.proxer.library.entity.ucp.UcpTopTenEntry
import me.proxer.library.entity.user.TopTenEntry
import me.proxer.library.entity.user.UserComment
import me.proxer.library.entity.user.UserHistoryEntry
import me.proxer.library.entity.user.UserMediaListEntry
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.CalendarDay
import me.proxer.library.enums.Category
import me.proxer.library.enums.Country
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Gender
import me.proxer.library.enums.IndustryType
import me.proxer.library.enums.Language
import me.proxer.library.enums.License
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.enums.MediaState
import me.proxer.library.enums.MediaType
import me.proxer.library.enums.Medium
import me.proxer.library.enums.RelationshipStatus
import me.proxer.library.enums.Season
import me.proxer.library.enums.SynonymType
import me.proxer.library.enums.UserMediaProgress
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUrls.hasProxerHost
import okhttp3.HttpUrl
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

object ProxerLibExtensions {
    fun fskConstraintFromAppString(
        context: Context,
        string: String,
    ) = when (string) {
        context.getString(R.string.fsk_0) -> FskConstraint.FSK_0
        context.getString(R.string.fsk_6) -> FskConstraint.FSK_6
        context.getString(R.string.fsk_12) -> FskConstraint.FSK_12
        context.getString(R.string.fsk_16) -> FskConstraint.FSK_16
        context.getString(R.string.fsk_18) -> FskConstraint.FSK_18
        context.getString(R.string.fsk_bad_language) -> FskConstraint.BAD_LANGUAGE
        context.getString(R.string.fsk_fear) -> FskConstraint.FEAR
        context.getString(R.string.fsk_violence) -> FskConstraint.VIOLENCE
        context.getString(R.string.fsk_sex) -> FskConstraint.SEX
        else -> error("Could not find fsk constraint for description: $description")
    }
}

fun Medium.toAppString(context: Context): String =
    context.getString(
        when (this) {
            Medium.ANIMESERIES -> R.string.medium_anime_series
            Medium.HENTAI -> R.string.medium_hentai
            Medium.MOVIE -> R.string.medium_movie
            Medium.OVA -> R.string.medium_ova
            Medium.MANGASERIES -> R.string.medium_manga_series
            Medium.LIGHTNOVEL -> R.string.medium_lightnovel
            Medium.WEBNOVEL -> R.string.medium_webnovel
            Medium.VISUALNOVEL -> R.string.medium_visualnovel
            Medium.DOUJIN -> R.string.medium_doujin
            Medium.HMANGA -> R.string.medium_h_manga
            Medium.ONESHOT -> R.string.medium_oneshot
            Medium.OTHER -> R.string.medium_other
        },
    )
