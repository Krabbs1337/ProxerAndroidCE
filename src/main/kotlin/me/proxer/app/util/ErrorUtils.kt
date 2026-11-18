package me.proxer.app.util

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.Loader
import me.proxer.app.R
import me.proxer.app.auth.LoginDialog
import me.proxer.app.base.BaseActivity
import me.proxer.app.comment.CommentInvalidProgressException
import me.proxer.app.comment.CommentTooLongException
import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.NotConnectedException
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.exception.PartialException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.manga.MangaLinkException
import me.proxer.app.manga.MangaNotAvailableException
import me.proxer.app.settings.AgeConfirmationDialog
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction.AGE_CONFIRMATION
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction.CAPTCHA
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction.LOGIN
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction.NETWORK_SETTINGS
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction.OPEN_LINK
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.safeInject
import me.proxer.library.ProxerException
import me.proxer.library.ProxerException.ErrorType.CANCELLED
import me.proxer.library.ProxerException.ErrorType.IO
import me.proxer.library.ProxerException.ErrorType.PARSING
import me.proxer.library.ProxerException.ErrorType.SERVER
import me.proxer.library.ProxerException.ErrorType.TIMEOUT
import me.proxer.library.ProxerException.ErrorType.UNKNOWN
import me.proxer.library.ProxerException.ServerErrorType.ANIME_INVALID_EPISODE
import me.proxer.library.ProxerException.ServerErrorType.ANIME_INVALID_STREAM
import me.proxer.library.ProxerException.ServerErrorType.ANIME_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.API_MAINTENANCE
import me.proxer.library.ProxerException.ServerErrorType.API_REMOVED
import me.proxer.library.ProxerException.ServerErrorType.APPS_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_ALREADY_EXISTS
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_DOES_NOT_EXIST
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_DUPLICATE
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_INVALID_NAME
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_PENDING
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_REJECTED
import me.proxer.library.ProxerException.ServerErrorType.AUTH_INVALID_USER
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_ALREADY_EXISTS
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INSUFFICIENT_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_COMMENT
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_CONTENT
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_ENTRY_ID
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_EPISODE
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_RATING
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_STATUS
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_NOT_ACTIVE_YET
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_SAVE_ERROR
import me.proxer.library.ProxerException.ServerErrorType.ERRORLOG_INVALID_INPUT
import me.proxer.library.ProxerException.ServerErrorType.FORUM_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.FORUM_INVALID_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.FUNCTION_BLOCKED
import me.proxer.library.ProxerException.ServerErrorType.INFO_DELETE_COMMENT_INVALID_INPUT
import me.proxer.library.ProxerException.ServerErrorType.INFO_ENTRY_ALREADY_IN_LIST
import me.proxer.library.ProxerException.ServerErrorType.INFO_EXCEEDED_MAXIMUM_ENTRIES
import me.proxer.library.ProxerException.ServerErrorType.INFO_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.INFO_INVALID_TYPE
import me.proxer.library.ProxerException.ServerErrorType.INFO_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.INSUFFICIENT_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.INTERNAL
import me.proxer.library.ProxerException.ServerErrorType.INVALID_API_CLASS
import me.proxer.library.ProxerException.ServerErrorType.INVALID_API_FUNCTION
import me.proxer.library.ProxerException.ServerErrorType.INVALID_TOKEN
import me.proxer.library.ProxerException.ServerErrorType.IP_AUTHENTICATION_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.IP_BLOCKED
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_CATEGORY
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_LANGUAGE
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_MEDIUM
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_SUBJECT
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_TYPE
import me.proxer.library.ProxerException.ServerErrorType.LIST_NO_ENTRIES_LEFT
import me.proxer.library.ProxerException.ServerErrorType.LIST_TOP_ACCESS_RESET
import me.proxer.library.ProxerException.ServerErrorType.LOGIN_ALREADY_LOGGED_IN
import me.proxer.library.ProxerException.ServerErrorType.LOGIN_DIFFERENT_USER_ALREADY_LOGGED_IN
import me.proxer.library.ProxerException.ServerErrorType.LOGIN_INVALID_CREDENTIALS
import me.proxer.library.ProxerException.ServerErrorType.LOGIN_MISSING_CREDENTIALS
import me.proxer.library.ProxerException.ServerErrorType.MANGA_INVALID_CHAPTER
import me.proxer.library.ProxerException.ServerErrorType.MEDIA_INVALID_ENTRY
import me.proxer.library.ProxerException.ServerErrorType.MEDIA_INVALID_STYLE
import me.proxer.library.ProxerException.ServerErrorType.MEDIA_REMOVED_DUE_TO_COPYRIGHT
import me.proxer.library.ProxerException.ServerErrorType.NEWS
import me.proxer.library.ProxerException.ServerErrorType.NOTIFICATIONS_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.RATE_LIMIT
import me.proxer.library.ProxerException.ServerErrorType.SERVER_MAINTENANCE
import me.proxer.library.ProxerException.ServerErrorType.UCP_INVALID_CATEGORY
import me.proxer.library.ProxerException.ServerErrorType.UCP_INVALID_EPISODE
import me.proxer.library.ProxerException.ServerErrorType.UCP_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.UCP_INVALID_SETTINGS
import me.proxer.library.ProxerException.ServerErrorType.UCP_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.UNKNOWN_API
import me.proxer.library.ProxerException.ServerErrorType.USER
import me.proxer.library.ProxerException.ServerErrorType.USERINFO_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.USER_2FA_SECRET_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.USER_ACCOUNT_BLOCKED
import me.proxer.library.ProxerException.ServerErrorType.USER_ACCOUNT_EXPIRED
import me.proxer.library.ProxerException.ServerErrorType.USER_INSUFFICIENT_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.WIKI_INVALID_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.WIKI_INVALID_TITLE
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * @author Ruben Gees
 */
object ErrorUtils {
    const val ENTRY_DATA_KEY = "entry"
    const val CHAPTER_TITLE_DATA_KEY = "chapterTitle"
    const val LINK_DATA_KEY = "link"

    fun getMessage(error: Throwable): Int =
        when (val innermostError = getInnermostError(error)) {
            is ProxerException -> {
                getMessageForProxerException(innermostError)
            }

            is HttpDataSource.InvalidResponseCodeException -> {
                when (innermostError.responseCode) {
                    404 -> R.string.error_video_deleted
                    503 -> R.string.error_video_service_unavailable
                    in 400 until 600 -> R.string.error_video_unknown
                    else -> R.string.error_unknown
                }
            }

            is SocketTimeoutException -> {
                R.string.error_timeout
            }

            is SSLPeerUnverifiedException -> {
                R.string.error_ssl
            }

            is NotConnectedException -> {
                R.string.error_no_network
            }

            is IOException -> {
                R.string.error_io
            }

            is NotLoggedInException -> {
                R.string.error_login_required
            }

            is AgeConfirmationRequiredException -> {
                R.string.error_age_confirmation_needed
            }

            is StreamResolutionException -> {
                R.string.error_stream_resolution
            }

            is MangaNotAvailableException -> {
                R.string.error_manga_not_available
            }

            is MangaLinkException -> {
                R.string.error_manga_link
            }

            is CommentTooLongException -> {
                R.string.error_comment_too_long
            }

            is CommentInvalidProgressException -> {
                R.string.error_comment_invalid_progress
            }

            else -> {
                R.string.error_unknown
            }
        }

    fun isIpBlockedError(error: Throwable) =
        getInnermostError(error).let {
            it is ProxerException && it.serverErrorType == IP_BLOCKED
        }

    fun handle(error: Throwable): ErrorAction {
        val innermostError = getInnermostError(error)
        val errorMessage = getMessage(innermostError)

        val buttonMessage =
            when (innermostError) {
                is ProxerException -> {
                    when (innermostError.serverErrorType) {
                        IP_BLOCKED -> R.string.error_action_captcha
                        MEDIA_REMOVED_DUE_TO_COPYRIGHT -> ACTION_MESSAGE_HIDE
                        in ErrorCategories.loginErrors -> R.string.error_action_login
                        else -> ACTION_MESSAGE_DEFAULT
                    }
                }

                is NotLoggedInException -> {
                    R.string.error_action_login
                }

                is NotConnectedException -> {
                    R.string.error_action_network_settings
                }

                is AgeConfirmationRequiredException -> {
                    R.string.error_action_confirm
                }

                is MangaLinkException -> {
                    R.string.error_action_open_link
                }

                else -> {
                    ACTION_MESSAGE_DEFAULT
                }
            }

        val buttonAction =
            when (innermostError) {
                is ProxerException -> {
                    when (innermostError.serverErrorType) {
                        IP_BLOCKED -> CAPTCHA
                        in ErrorCategories.loginErrors -> LOGIN
                        else -> null
                    }
                }

                is NotLoggedInException -> {
                    LOGIN
                }

                is NotConnectedException -> {
                    NETWORK_SETTINGS
                }

                is AgeConfirmationRequiredException -> {
                    AGE_CONFIRMATION
                }

                is MangaLinkException -> {
                    OPEN_LINK
                }

                else -> {
                    null
                }
            }

        val data = mutableMapOf<String, Any?>()

        if (error is PartialException) {
            data[ENTRY_DATA_KEY] = error.partialData
        }

        if (innermostError is MangaLinkException) {
            data[CHAPTER_TITLE_DATA_KEY] = innermostError.chapterTitle
            data[LINK_DATA_KEY] = innermostError.link
        }

        return ErrorAction(errorMessage, buttonMessage, buttonAction, data)
    }

    private fun getInnermostError(error: Throwable): Throwable =
        when (error) {
            is ProxerException -> {
                when (error.errorType) {
                    IO -> error.cause?.let { getInnermostError(it) } ?: error
                    UNKNOWN -> error.cause?.let { getInnermostError(it) } ?: error
                    else -> error
                }
            }

            is PartialException -> {
                error.innerError
            }

            is ExoPlaybackException -> {
                error.cause?.let { getInnermostError(it) } ?: error
            }

            is Loader.UnexpectedLoaderException -> {
                error.cause?.let { getInnermostError(it) } ?: error
            }

            else -> {
                error
            }
        }

    open class ErrorAction(
        val message: Int,
        val buttonMessage: Int = ACTION_MESSAGE_DEFAULT,
        val buttonAction: ButtonAction? = null,
        val data: Map<String, Any?> = emptyMap(),
    ) {
        companion object {
            const val ACTION_MESSAGE_DEFAULT = -1
            const val ACTION_MESSAGE_HIDE = -2
        }

        fun toClickListener(activity: BaseActivity) =
            when (buttonAction) {
                CAPTCHA -> {
                    View.OnClickListener {
                        activity.showPage(ProxerUrls.captchaWeb(Utils.getIpAddress(), Device.MOBILE), skipCheck = true)
                    }
                }

                NETWORK_SETTINGS -> {
                    View.OnClickListener {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            activity.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                        } else {
                            activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                        }
                    }
                }

                LOGIN -> {
                    View.OnClickListener { LoginDialog.show(activity) }
                }

                AGE_CONFIRMATION -> {
                    View.OnClickListener { AgeConfirmationDialog.show(activity) }
                }

                OPEN_LINK -> {
                    data[LINK_DATA_KEY].let { link ->
                        when (link) {
                            is HttpUrl -> View.OnClickListener { activity.showPage(link, skipCheck = true) }
                            else -> null
                        }
                    }
                }

                else -> {
                    null
                }
            }

        fun toIntent() =
            when (buttonAction) {
                CAPTCHA -> {
                    Intent(
                        Intent.ACTION_VIEW,
                        ProxerUrls.captchaWeb(Utils.getIpAddress(), Device.MOBILE).androidUri(),
                    )
                }

                else -> {
                    null
                }
            }

        enum class ButtonAction { CAPTCHA, NETWORK_SETTINGS, LOGIN, AGE_CONFIRMATION, OPEN_LINK, BOOKMARK }
    }
}
