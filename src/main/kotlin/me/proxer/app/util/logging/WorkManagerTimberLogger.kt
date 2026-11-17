package me.proxer.app.util.logging

import android.annotation.SuppressLint
import android.util.Log
import androidx.work.Logger
import timber.log.Timber

/**
 * @author Ruben Gees
 */
@SuppressLint("RestrictedApi")
class WorkManagerTimberLogger(
    loggingLevel: Int = Log.INFO,
) : Logger(loggingLevel) {
    override fun verbose(
        tag: String,
        message: String,
    ) = Timber.v(message)

    override fun verbose(
        tag: String,
        message: String,
        throwable: Throwable,
    ) = Timber.v(throwable, message)

    override fun debug(
        tag: String,
        message: String,
    ) = Timber.d(message)

    override fun debug(
        tag: String,
        message: String,
        throwable: Throwable,
    ) = Timber.d(throwable, message)

    override fun info(
        tag: String,
        message: String,
    ) = Timber.i(message)

    override fun info(
        tag: String,
        message: String,
        throwable: Throwable,
    ) = Timber.i(throwable, message)

    override fun warning(
        tag: String,
        message: String,
    ) = Timber.w(message)

    override fun warning(
        tag: String,
        message: String,
        throwable: Throwable,
    ) = Timber.w(throwable, message)

    override fun error(
        tag: String,
        message: String,
    ) = Timber.e(message)

    override fun error(
        tag: String,
        message: String,
        throwable: Throwable,
    ) = Timber.e(throwable, message)
}
