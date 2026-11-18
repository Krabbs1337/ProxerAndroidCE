package com.rubengees.rxbus

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * In-repo replacement for `com.github.rubengees:rxbus` (jitpack-only). Only the API surface
 * actually used by the app is implemented: [post] and [register].
 */
class RxBus {
    private val subject = PublishSubject.create<Any>().toSerialized()

    fun post(event: Any): Boolean {
        subject.onNext(event)
        return subject.hasObservers()
    }

    fun <T : Any> register(eventClass: Class<T>): Observable<T> = subject.ofType(eventClass)
}
