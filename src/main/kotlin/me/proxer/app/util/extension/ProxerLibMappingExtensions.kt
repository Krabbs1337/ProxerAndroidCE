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

fun Stream.toAnimeStream(
    isSupported: Boolean,
    resolutionResult: StreamResolutionResult? = null,
) = AnimeStream(
    id,
    hoster,
    hosterName,
    image,
    uploaderId,
    uploaderName,
    date.toInstantBP(),
    translatorGroupId,
    translatorGroupName,
    isOfficial,
    isPublic,
    isSupported,
    resolutionResult,
)

fun UcpSettings.toLocalSettings() =
    LocalProfileSettings(
        profileVisibility,
        topTenVisibility,
        animeVisibility,
        mangaVisibility,
        commentVisibility,
        forumVisibility,
        friendVisibility,
        friendRequestConstraint,
        aboutVisibility,
        historyVisibility,
        guestBookVisibility,
        guestBookEntryConstraint,
        galleryVisibility,
        articleVisibility,
        isHideTags,
        isShowAds,
        adInterval,
    )

fun Comment.toParsedComment() =
    ParsedComment(
        id,
        entryId,
        authorId,
        mediaProgress,
        ratingDetails,
        content.toSimpleBBTree(),
        overallRating,
        episode,
        helpfulVotes,
        date.toInstantBP(),
        author,
        image,
    )

fun Comment.toLocalComment() =
    LocalComment(
        id,
        entryId,
        mediaProgress,
        ratingDetails,
        content,
        overallRating,
        episode,
    )

fun UserComment.toParsedUserComment() =
    ParsedUserComment(
        id,
        entryId,
        entryName,
        medium,
        category,
        authorId,
        mediaProgress,
        ratingDetails,
        content.toSimpleBBTree(),
        overallRating,
        episode,
        helpfulVotes,
        date.toInstantBP(),
        author,
        image,
    )

fun Topic.toTopicMetaData() =
    TopicMetaData(
        categoryId,
        categoryName,
        firstPostDate,
        lastPostDate,
        hits,
        isLocked,
        post,
        subject,
    )

fun Post.toParsedPost(resources: Resources): ParsedPost {
    val parsedMessage = message.toBBTree(BBArgs(resources = resources, userId = userId))
    val parsedSignature =
        signature?.let {
            if (it.isNotBlank()) it.toBBTree(BBArgs(resources = resources, userId = userId)) else null
        }

    return ParsedPost(
        id,
        parentId,
        userId,
        username,
        image,
        date.toInstantBP(),
        parsedSignature,
        modifiedById,
        modifiedByName,
        modifiedReason,
        parsedMessage,
        thankYouAmount,
    )
}

fun Tag.toParcelableTag() = LocalTag(id, type, name, description, subType, isSpoiler)

fun UserMediaListEntry.toLocalEntry() =
    LocalUserMediaListEntry(
        id,
        name,
        episodeAmount,
        medium,
        state,
        commentId,
        commentContent,
        mediaProgress,
        episode,
        rating,
    )

fun UserMediaListEntry.toLocalEntryUcp() =
    LocalUserMediaListEntry.Ucp(
        id,
        name,
        episodeAmount,
        medium,
        state,
        commentId,
        commentContent,
        mediaProgress,
        episode,
        rating,
    )

fun UserHistoryEntry.toLocalEntry() = LocalUserHistoryEntry(id, entryId, name, language, medium, category, episode)

fun UcpHistoryEntry.toLocalEntryUcp() =
    LocalUserHistoryEntry.Ucp(
        id,
        entryId,
        name,
        language,
        medium,
        category,
        episode,
        date,
    )

fun TopTenEntry.toLocalEntry() = LocalTopTenEntry(id, name, category, medium)

fun UcpTopTenEntry.toLocalEntryUcp() = LocalTopTenEntry.Ucp(id, name, category, medium, entryId)

fun HttpUrl.proxyIfRequired() =
    when (this.hasProxerHost) {
        true -> this
        false -> ProxerUrls.proxyImage(this)
    }
