package com.gojuno.koptional

/**
 * Minimal in-repo replacement for the abandoned `com.gojuno.koptional:koptional` library
 * (it was only ever published to jcenter, which no longer serves artifacts).
 */
sealed class Optional<out T> {
    abstract val isPresent: Boolean

    companion object {
        fun <T> toOptional(value: T?): Optional<T> = value.toOptional()
    }
}

fun <T> Optional<T>.toNullable(): T? = (this as? Some<T>)?.value

data class Some<out T>(
    val value: T,
) : Optional<T>() {
    override val isPresent: Boolean = true
}

object None : Optional<Nothing>() {
    override val isPresent: Boolean = false
}

fun <T> T?.toOptional(): Optional<T> = if (this != null) Some(this) else None
