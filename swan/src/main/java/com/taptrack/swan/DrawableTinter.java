/*
 * Copyright (c) 2016. Papyrus Electronics, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.taptrack.swan;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

abstract class DrawableTinter {
    private DrawableTinter() {
        super();
    }

    public static Drawable getColorResTintedDrawable(Context ctx, @DrawableRes int drawable, @ColorRes int color) {
        return getColorIntTintedDrawable(ctx, drawable, ContextCompat.getColor(ctx, color));
    }

    public static Drawable getColorIntTintedDrawable(Context ctx, @DrawableRes int drawable, @ColorInt int color) {
        Drawable wrapDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(ctx,drawable));
        wrapDrawable.mutate();
        DrawableCompat.setTint(wrapDrawable, color);
        return wrapDrawable;
    }
}
