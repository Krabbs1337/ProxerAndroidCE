package com.gojuno.koptional.rxjava2

import com.gojuno.koptional.Optional
import com.gojuno.koptional.Some
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

/**
 * In-repo replacement for `koptional-rxjava2-extensions` (see [com.gojuno.koptional.KOptional]).
 */

fun <T> Observable<Optional<T>>.filterSome(): Observable<T> = filter { it is Some }.map { (it as Some<T>).value }

fun <T> Flowable<Optional<T>>.filterSome(): Flowable<T> = filter { it is Some }.map { (it as Some<T>).value }

fun <T> Maybe<Optional<T>>.filterSome(): Maybe<T> = filter { it is Some }.map { (it as Some<T>).value }

fun <T> Single<Optional<T>>.filterSome(): Maybe<T> =
    flatMapMaybe { if (it is Some) Maybe.just(it.value) else Maybe.empty() }
