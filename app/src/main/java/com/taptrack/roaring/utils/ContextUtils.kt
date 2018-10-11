package com.taptrack.roaring.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

inline fun ViewGroup.inflateChildren(@LayoutRes layoutRes: Int): View =
    this.inflateChildren(layoutRes,true)

inline fun ViewGroup.inflateChildren(@LayoutRes layoutRes: Int, shouldAttach: Boolean): View =
        this.context.inflateChildren(layoutRes,this,shouldAttach)


inline fun Context.inflateChildren(@LayoutRes layoutRes: Int, parent: ViewGroup, shouldAttach: Boolean): View {
    val inflater = LayoutInflater.from(this)
    return inflater.inflate(layoutRes, parent,shouldAttach)
}


inline fun Context.getDrawableCompat(@DrawableRes drawableRes: Int): Drawable? = ContextCompat.getDrawable(this,drawableRes)

@ColorInt
inline fun Context.getColorCompat(@ColorRes colorRes: Int): Int = ContextCompat.getColor(this,colorRes)

inline fun Context.getColorResTintedDrawable(drawable: Drawable, @ColorRes color: Int): Drawable {
    return this.getColorIntTintedDrawable(drawable, ContextCompat.getColor(this, color))
}

inline fun Context.getColorResTintedDrawable(@DrawableRes drawable: Int, @ColorRes color: Int): Drawable? {
    return this.getColorIntTintedDrawable(drawable, ContextCompat.getColor(this, color))
}

inline fun Context.getColorIntTintedDrawable(drawable: Drawable, @ColorInt color: Int): Drawable {
    val wrapDrawable = DrawableCompat.wrap(drawable)
    wrapDrawable.mutate()
    DrawableCompat.setTint(wrapDrawable, color)
    return wrapDrawable
}

inline fun Context.getColorIntTintedDrawable(@DrawableRes drawable: Int, @ColorInt color: Int): Drawable? {
    val rawDrawable = ContextCompat.getDrawable(this,drawable) ?: return null

    val wrapDrawable = DrawableCompat.wrap(rawDrawable)
    wrapDrawable.mutate()
    DrawableCompat.setTint(wrapDrawable, color)
    return wrapDrawable
}

inline fun Context.isNightMode(): Boolean {
    val nightModeFlags = this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    when (nightModeFlags) {
        Configuration.UI_MODE_NIGHT_YES ->
                return true
        else ->
                return false
    }
}