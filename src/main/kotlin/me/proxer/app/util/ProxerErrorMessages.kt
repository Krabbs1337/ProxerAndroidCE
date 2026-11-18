package me.proxer.app.util

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

import me.proxer.app.R
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.safeInject

private val storageHelper by safeInject<StorageHelper>()

/**
 * Maps a [ProxerException] to a user-facing message resource.
 */
internal fun getMessageForProxerException(error: ProxerException): Int =
    when (error.errorType) {
        SERVER -> {
            when (error.serverErrorType) {
                IP_BLOCKED -> {
                    R.string.error_captcha
                }

                RATE_LIMIT -> {
                    R.string.error_rate_limit
                }

                INVALID_TOKEN -> {
                    R.string.error_invalid_token
                }

                LOGIN_INVALID_CREDENTIALS -> {
                    R.string.error_login_credentials
                }

                INFO_ENTRY_ALREADY_IN_LIST -> {
                    R.string.error_already_in_list
                }

                INFO_EXCEEDED_MAXIMUM_ENTRIES -> {
                    R.string.error_list_full
                }

                MANGA_INVALID_CHAPTER -> {
                    R.string.error_invalid_chapter
                }

                ANIME_INVALID_EPISODE -> {
                    R.string.error_invalid_episode
                }

                LIST_TOP_ACCESS_RESET -> {
                    R.string.error_list_top_access_reset
                }

                USER_2FA_SECRET_REQUIRED -> {
                    R.string.error_login_two_factor_authentication
                }

                USER_ACCOUNT_EXPIRED -> {
                    R.string.error_account_expired
                }

                USER_ACCOUNT_BLOCKED -> {
                    R.string.error_account_blocked
                }

                USER_INSUFFICIENT_PERMISSIONS -> {
                    when (storageHelper.isLoggedIn) {
                        true -> R.string.error_insufficient_permissions_logged_in
                        false -> R.string.error_insufficient_permissions
                    }
                }

                FORUM_INVALID_PERMISSIONS -> {
                    R.string.error_forum_no_permissions
                }

                COMMENT_INSUFFICIENT_PERMISSIONS -> {
                    R.string.error_comment_no_permissions
                }

                WIKI_INVALID_PERMISSIONS -> {
                    R.string.error_wiki_no_permissions
                }

                COMMENT_NOT_ACTIVE_YET -> {
                    R.string.error_comment_not_active_yet
                }

                IP_AUTHENTICATION_REQUIRED -> {
                    R.string.error_login_ip_authentication
                }

                MEDIA_REMOVED_DUE_TO_COPYRIGHT -> {
                    R.string.error_media_removed_due_to_copyright
                }

                INTERNAL -> {
                    R.string.error_internal
                }

                in ErrorCategories.apiErrors -> {
                    R.string.error_api
                }

                in ErrorCategories.maintenanceErrors -> {
                    R.string.error_maintenance
                }

                in ErrorCategories.loginErrors -> {
                    R.string.error_login
                }

                in ErrorCategories.clientErrors -> {
                    R.string.error_client
                }

                in ErrorCategories.invalidIdErrors -> {
                    R.string.error_invalid_id
                }

                in ErrorCategories.unsupportedErrors -> {
                    R.string.error_unsupported_code
                }

                else -> {
                    R.string.error_unknown
                }
            }
        }

        IO -> {
            R.string.error_io
        }

        TIMEOUT -> {
            R.string.error_timeout
        }

        PARSING -> {
            R.string.error_parsing
        }

        UNKNOWN, CANCELLED -> {
            R.string.error_unknown
        }
    }
