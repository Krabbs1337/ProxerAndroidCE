package me.proxer.app.media.info

import androidx.core.view.isNotEmpty
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TableLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.info.industry.IndustryActivity
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.media.MediaInfoViewModel
import me.proxer.app.ui.view.MaxLineFlexboxLayout
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toAppStringDescription
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.toEndAppString
import me.proxer.app.util.extension.toStartAppString
import me.proxer.app.util.extension.toTypeAppString
import me.proxer.library.entity.info.Entry
import me.proxer.library.entity.info.MediaUserInfo
import me.proxer.library.enums.IndustryType
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf
import me.proxer.app.databinding.FragmentMediaInfoBinding

internal fun MediaInfoFragment.bindRating(result: Entry) =
    if (result.rating > 0) {
        ratingContainer.isVisible = true
        rating.rating = result.rating / 2.0f
        ratingAmount.isVisible = true
        ratingAmount.text =
            requireContext().resources.getQuantityString(
                R.plurals.fragment_media_info_rate_count,
                result.ratingAmount,
                result.rating,
                result.ratingAmount,
            )
    } else {
        ratingContainer.isGone = true
        ratingAmount.isGone = true
    }

internal fun MediaInfoFragment.bindSynonyms(result: Entry) {
    result.synonyms.forEach {
        infoTable.addView(constructInfoTableRow(it.toTypeAppString(requireContext()), it.name, true))
    }
}

internal fun MediaInfoFragment.bindSeasons(result: Entry) {
    val seasons = result.seasons

    if (seasons.isNotEmpty()) {
        val tableRow =
            LayoutInflater
                .from(context)
                .inflate(R.layout.layout_media_info_seasons_row, infoTable, false)

        val seasonStartView = tableRow.findViewById<TextView>(R.id.seasonStart)
        val seasonEndView = tableRow.findViewById<TextView>(R.id.seasonEnd)

        seasonStartView.text = seasons[0].toStartAppString(requireContext())

        if (seasons.size >= 2) {
            seasonEndView.text = seasons[1].toEndAppString(requireContext())
        } else {
            seasonEndView.isGone = true
        }

        infoTable.addView(tableRow)
    }
}

internal fun MediaInfoFragment.bindStatus(result: Entry) {
    infoTable.addView(
        constructInfoTableRow(
            requireContext().getString(R.string.fragment_media_info_status_title),
            result.state.toAppString(requireContext()),
        ),
    )
}

internal fun MediaInfoFragment.bindLicense(result: Entry) {
    infoTable.addView(
        constructInfoTableRow(
            requireContext().getString(R.string.fragment_media_info_license_title),
            result.license.toAppString(requireContext()),
        ),
    )
}

internal fun MediaInfoFragment.bindAdaption(result: Entry) {
    result.adaptionInfo.let { adaptionInfo ->
        if (adaptionInfo.id != "0") {
            val title = getString(R.string.fragment_media_info_adaption_title)
            val content = "${adaptionInfo.name} (${adaptionInfo.medium?.toAppString(requireContext())})"

            infoTable.addView(
                constructInfoTableRow(title, content).also { tableRow ->
                    tableRow.findViewById<View>(R.id.content).also { contentView ->
                        val selectableItemBackground =
                            TypedValue().apply {
                                requireContext().theme.resolveAttribute(R.attr.selectableItemBackground, this, true)
                            }

                        contentView.setBackgroundResource(selectableItemBackground.resourceId)

                        contentView
                            .clicks()
                            .autoDispose(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                            .subscribe {
                                MediaActivity.navigateTo(
                                    requireActivity(),
                                    adaptionInfo.id,
                                    adaptionInfo.name,
                                    adaptionInfo.medium?.toCategory(),
                                )
                            }
                    }
                },
            )
        }
    }
}

internal fun MediaInfoFragment.constructInfoTableRow(
    title: String,
    content: String,
    isSelectable: Boolean = false,
): View {
    val tableRow = LayoutInflater.from(context).inflate(R.layout.layout_media_info_row, infoTable, false)
    val titleView = tableRow.findViewById<TextView>(R.id.title)
    val contentView = tableRow.findViewById<TextView>(R.id.content)

    titleView.text = title
    contentView.text = content
    contentView.isSaveEnabled = false
    contentView.setTextIsSelectable(isSelectable)

    return tableRow
}

internal fun MediaInfoFragment.bindGenres(result: Entry) {
    if (result.genres.isEmpty()) {
        genresTitle.isGone = true
        genres.isGone = true

        return
    }

    bindChips(
        genres,
        result.genres.toList(),
        mapFunction = { it.name },
        onClick = { hostingActivity.multilineSnackbar(it.description) },
    )
}

internal fun MediaInfoFragment.bindTags(result: Entry) {
    if (result.tags.isEmpty()) {
        tagsTitle.isGone = true
        unratedTags.isGone = true
        spoilerTags.isGone = true
    } else {
        updateSpoilerButton()
        updateUnratedButton()
    }

    val filteredTags =
        result.tags.filter {
            when (it.isRated) {
                true -> {
                    when (it.isSpoiler) {
                        true -> showSpoilerTags
                        false -> true
                    }
                }

                false -> {
                    when (showUnratedTags) {
                        true -> {
                            when (it.isSpoiler) {
                                true -> showSpoilerTags
                                false -> true
                            }
                        }

                        false -> {
                            false
                        }
                    }
                }
            }
        }

    bindChips(
        tags,
        filteredTags,
        mapFunction = { it.name },
        onClick = { hostingActivity.multilineSnackbar(it.description) },
    )
}

internal fun MediaInfoFragment.updateUnratedButton() {
    unratedTags.text =
        getString(
            when (showUnratedTags) {
                true -> R.string.fragment_media_info_tags_unrated_hide
                false -> R.string.fragment_media_info_tags_unrated_show
            },
        )
}

internal fun MediaInfoFragment.updateSpoilerButton() {
    spoilerTags.text =
        getString(
            when (showSpoilerTags) {
                true -> R.string.fragment_media_info_tags_spoiler_hide
                false -> R.string.fragment_media_info_tags_spoiler_show
            },
        )
}

internal fun MediaInfoFragment.bindFskConstraints(result: Entry) {
    fskConstraints.removeAllViews()

    if (result.fskConstraints.isEmpty()) {
        fskConstraintsTitle.isGone = true
        fskConstraints.isGone = true
    } else {
        result.fskConstraints.forEach { constraint ->
            val image =
                LayoutInflater
                    .from(context)
                    .inflate(R.layout.layout_image, fskConstraints, false) as ImageView

            image.setImageDrawable(constraint.toAppDrawable(requireContext()))

            image
                .clicks()
                .autoDispose(viewLifecycleOwner.scope())
                .subscribe {
                    hostingActivity.multilineSnackbar(constraint.toAppStringDescription(requireContext()))
                }

            fskConstraints.addView(image)
        }
    }
}

internal fun MediaInfoFragment.bindTranslatorGroups(result: Entry) =
    if (result.translatorGroups.isEmpty()) {
        translatorGroupsTitle.isGone = true
        translatorGroups.isGone = true
    } else {
        bindChips(
            translatorGroups,
            result.translatorGroups,
            mapFunction = { it.name },
            onClick = { TranslatorGroupActivity.navigateTo(requireActivity(), it.id, it.name) },
        )
    }

internal fun MediaInfoFragment.bindIndustries(result: Entry) =
    if (result.industries.isEmpty()) {
        industriesTitle.isGone = true
        industries.isGone = true
    } else {
        bindChips(
            industries,
            result.industries,
            mapFunction = {
                if (it.type == IndustryType.UNKNOWN) {
                    it.name
                } else {
                    "${it.name} (${it.type.toAppString(requireContext())})"
                }
            },
            onClick = {
                IndustryActivity.navigateTo(requireActivity(), it.id, it.name)
            },
        )
    }

@Suppress("LabeledExpression")
internal fun <T> MediaInfoFragment.bindChips(
    layout: FlexboxLayout,
    items: List<T>,
    mapFunction: (T) -> String = { it.toString() },
    onClick: ((T) -> Unit)? = null,
) {
    layout.post {
        if (layout.width <= 0 || view == null || view?.isAttachedToWindow != true) return@post
        if (layout.isNotEmpty()) layout.removeAllViews()

        for ((index, mappedItem) in items
            .asSequence()
            .map(mapFunction)
            .withIndex()
            .toList()) {
            val chip = LayoutInflater.from(layout.context).inflate(R.layout.item_chip, layout, false) as Chip

            chip.text = mappedItem

            if (onClick != null) {
                chip
                    .clicks()
                    .autoDispose(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                    .subscribe { onClick.invoke(items[index]) }
            }

            if (layout is MaxLineFlexboxLayout && !layout.canAddView(chip)) {
                layout.showAllEvents
                    .autoDispose(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                    .subscribe { bindChips(layout, items, mapFunction, onClick) }

                layout.enableShowAllButton()

                break
            } else {
                layout.addView(chip)
            }
        }
    }
}

internal fun MediaInfoFragment.bindUserInfo(userInfo: MediaUserInfo?) {
    val noteColor = if (userInfo?.isNoted == true) R.attr.colorSecondary else R.attr.colorIcon
    val favorColor = if (userInfo?.isTopTen == true) R.attr.colorSecondary else R.attr.colorIcon
    val finishColor = if (userInfo?.isFinished == true) R.attr.colorSecondary else R.attr.colorIcon
    val subscribeColor = if (userInfo?.isSubscribed == true) R.attr.colorSecondary else R.attr.colorIcon

    note.setIconicsImage(CommunityMaterial.Icon.cmd_clock, 24, 0, noteColor)
    favor.setIconicsImage(CommunityMaterial.Icon3.cmd_star, 24, 0, favorColor)
    finish.setIconicsImage(CommunityMaterial.Icon.cmd_check, 24, 0, finishColor)
    subscribe.setIconicsImage(CommunityMaterial.Icon3.cmd_newspaper, 24, 0, subscribeColor)
}
