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

/**
 * Server error categories used for error mapping.
 */
internal object ErrorCategories {
    internal val apiErrors =
        arrayOf(
            UNKNOWN_API,
            API_REMOVED,
            INVALID_API_CLASS,
            INVALID_API_FUNCTION,
            INSUFFICIENT_PERMISSIONS,
            FUNCTION_BLOCKED,
            COMMENT_SAVE_ERROR,
        )

    internal val maintenanceErrors = arrayOf(SERVER_MAINTENANCE, API_MAINTENANCE)

    internal val loginErrors =
        arrayOf(
            INVALID_TOKEN,
            NOTIFICATIONS_LOGIN_REQUIRED,
            UCP_LOGIN_REQUIRED,
            INFO_LOGIN_REQUIRED,
            USER_2FA_SECRET_REQUIRED,
            ANIME_LOGIN_REQUIRED,
            IP_AUTHENTICATION_REQUIRED,
            COMMENT_LOGIN_REQUIRED,
        )

    internal val clientErrors =
        arrayOf(
            NEWS,
            LOGIN_MISSING_CREDENTIALS,
            UCP_INVALID_CATEGORY,
            INFO_INVALID_TYPE,
            LOGIN_ALREADY_LOGGED_IN,
            LOGIN_DIFFERENT_USER_ALREADY_LOGGED_IN,
            LIST_INVALID_CATEGORY,
            LIST_INVALID_MEDIUM,
            MEDIA_INVALID_STYLE,
            ANIME_INVALID_STREAM,
            LIST_INVALID_LANGUAGE,
            LIST_INVALID_TYPE,
            ERRORLOG_INVALID_INPUT,
            LIST_INVALID_SUBJECT,
            INFO_DELETE_COMMENT_INVALID_INPUT,
            UCP_INVALID_SETTINGS,
            COMMENT_INVALID_ID,
            COMMENT_INVALID_COMMENT,
            COMMENT_INVALID_RATING,
            COMMENT_INVALID_EPISODE,
            COMMENT_INVALID_STATUS,
            COMMENT_INVALID_ENTRY_ID,
            COMMENT_INVALID_CONTENT,
            COMMENT_ALREADY_EXISTS,
            WIKI_INVALID_TITLE,
        )

    internal val invalidIdErrors =
        arrayOf(
            USERINFO_INVALID_ID,
            UCP_INVALID_ID,
            INFO_INVALID_ID,
            MEDIA_INVALID_ENTRY,
            UCP_INVALID_EPISODE,
            LIST_INVALID_ID,
            FORUM_INVALID_ID,
            APPS_INVALID_ID,
        )

    internal val unsupportedErrors =
        arrayOf(
            USER,
            AUTH_INVALID_USER,
            AUTH_CODE_ALREADY_EXISTS,
            AUTH_CODE_DOES_NOT_EXIST,
            AUTH_CODE_REJECTED,
            AUTH_CODE_PENDING,
            AUTH_CODE_INVALID_NAME,
            AUTH_CODE_DUPLICATE,
            LIST_NO_ENTRIES_LEFT,
        )
}
