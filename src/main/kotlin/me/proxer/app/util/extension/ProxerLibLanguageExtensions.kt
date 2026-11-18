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

fun MediaLanguage.toGeneralLanguage() =
    when (this) {
        MediaLanguage.GERMAN, MediaLanguage.GERMAN_SUB, MediaLanguage.GERMAN_DUB -> Language.GERMAN
        MediaLanguage.ENGLISH, MediaLanguage.ENGLISH_SUB, MediaLanguage.ENGLISH_DUB -> Language.ENGLISH
        MediaLanguage.OTHER -> Language.OTHER
    }

fun MediaLanguage.toAnimeLanguage() =
    when (this) {
        MediaLanguage.GERMAN, MediaLanguage.GERMAN_SUB -> AnimeLanguage.GERMAN_SUB
        MediaLanguage.ENGLISH, MediaLanguage.ENGLISH_SUB -> AnimeLanguage.ENGLISH_SUB
        MediaLanguage.GERMAN_DUB -> AnimeLanguage.GERMAN_DUB
        MediaLanguage.ENGLISH_DUB -> AnimeLanguage.ENGLISH_DUB
        MediaLanguage.OTHER -> AnimeLanguage.OTHER
    }

fun Language.toMediaLanguage() =
    when (this) {
        Language.GERMAN -> MediaLanguage.GERMAN
        Language.ENGLISH -> MediaLanguage.ENGLISH
        Language.OTHER -> MediaLanguage.OTHER
    }

fun AnimeLanguage.toMediaLanguage() =
    when (this) {
        AnimeLanguage.GERMAN_SUB -> MediaLanguage.GERMAN_SUB
        AnimeLanguage.GERMAN_DUB -> MediaLanguage.GERMAN_DUB
        AnimeLanguage.ENGLISH_SUB -> MediaLanguage.ENGLISH_SUB
        AnimeLanguage.ENGLISH_DUB -> MediaLanguage.ENGLISH_DUB
        AnimeLanguage.OTHER -> MediaLanguage.OTHER
    }

fun Language.toAppDrawable(context: Context) =
    AppCompatResources.getDrawable(
        context,
        when (this) {
            Language.GERMAN -> R.drawable.ic_germany
            Language.ENGLISH -> R.drawable.ic_united_states
            Language.OTHER -> R.drawable.ic_united_nations
        },
    ) ?: error("Could not resolve Drawable for language: $this")

fun MediaLanguage.toAppString(context: Context): String =
    context.getString(
        when (this) {
            MediaLanguage.GERMAN -> R.string.language_german
            MediaLanguage.ENGLISH -> R.string.language_english
            MediaLanguage.GERMAN_SUB -> R.string.language_german_sub
            MediaLanguage.GERMAN_DUB -> R.string.language_german_dub
            MediaLanguage.ENGLISH_SUB -> R.string.language_english_sub
            MediaLanguage.ENGLISH_DUB -> R.string.language_english_dub
            MediaLanguage.OTHER -> R.string.language_other
        },
    )

fun Country.toAppDrawable(context: Context) =
    AppCompatResources.getDrawable(
        context,
        when (this) {
            Country.GERMANY -> R.drawable.ic_germany
            Country.ENGLAND -> R.drawable.ic_united_states
            Country.UNITED_STATES -> R.drawable.ic_united_states
            Country.JAPAN -> R.drawable.ic_japan
            Country.KOREA -> R.drawable.ic_korea
            Country.CHINA -> R.drawable.ic_china
            Country.INTERNATIONAL, Country.OTHER, Country.NONE -> R.drawable.ic_united_nations
        },
    ) ?: error("Could not resolve Drawable for country: $this")
