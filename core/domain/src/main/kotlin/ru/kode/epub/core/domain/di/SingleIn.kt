package ru.kode.epub.core.domain.di

import me.tatarka.inject.annotations.Scope
import kotlin.reflect.KClass

/**
 * Indicates that this provided type (via [Provides], [Inject], etc)
 * will only have a single instances within the target [value] scope.
 *
 * Note that the [value] does not actually need to be a [Scope]-annotated
 * annotation class. It is _solely_ a key.
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class SingleIn(val value: KClass<*>)
