/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.glance.appwidget

import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal open class ViewSubject(
    metaData: FailureMetadata,
    private val actual: View?
) : Subject(metaData, actual) {
    fun hasBackgroundColor(@ColorInt color: Int) {
        isNotNull()
        actual!!
        check("getBackground()").that(actual.background).isInstanceOf(ColorDrawable::class.java)
        val background = actual.background as ColorDrawable
        // Comparing the hex string representation is equivalent to comparing the int, and the
        // error message is a lot more readable with the hex string if this fails.
        check("getBackground().getColor()")
            .that(Integer.toHexString(background.color))
            .isEqualTo(Integer.toHexString(color))
    }

    fun hasBackgroundColor(hexString: String) =
        hasBackgroundColor(android.graphics.Color.parseColor(hexString))

    fun hasLayoutParamsWidth(@Px px: Int) {
        check("getLayoutParams().width").that(actual?.layoutParams?.width).isEqualTo(px)
    }

    fun hasLayoutParamsWidth(dp: Dp) {
        assertNotNull(actual)
        hasLayoutParamsWidth(dp.toPixels(actual.context))
    }

    fun hasLayoutParamsHeight(@Px px: Int) {
        check("getLayoutParams().height").that(actual?.layoutParams?.height).isEqualTo(px)
    }

    fun hasLayoutParamsHeight(dp: Dp) {
        assertNotNull(actual)
        hasLayoutParamsHeight(dp.toPixels(actual.context))
    }

    companion object {
        fun views(): Factory<ViewSubject, View> {
            return Factory<ViewSubject, View> { metadata, actual -> ViewSubject(metadata, actual) }
        }

        fun assertThat(view: View?): ViewSubject = assertAbout(views()).that(view)
    }
}

internal open class TextViewSubject(
    metaData: FailureMetadata,
    private val actual: TextView?
) : ViewSubject(metaData, actual) {
    fun hasTextColor(@ColorInt color: Int) {
        isNotNull()
        actual!!
        // Comparing the hex string representation is equivalent to comparing the int, and the
        // error message is a lot more readable with the hex string if this fails.
        check("getCurrentTextColor()")
            .that(Integer.toHexString(actual.currentTextColor))
            .isEqualTo(Integer.toHexString(color))
    }

    fun hasTextColor(hexString: String) = hasTextColor(android.graphics.Color.parseColor(hexString))

    companion object {
        fun textViews(): Factory<TextViewSubject, TextView> {
            return Factory<TextViewSubject, TextView> { metadata, actual ->
                TextViewSubject(metadata, actual)
            }
        }

        fun assertThat(view: TextView?): TextViewSubject = assertAbout(textViews()).that(view)
    }
}

internal open class ImageViewSubject(
    metaData: FailureMetadata,
    private val actual: ImageView?
) : ViewSubject(metaData, actual) {
    fun hasColorFilter(@ColorInt color: Int) {
        assertNotNull(actual)
        val colorFilter = actual.colorFilter
        assertIs<PorterDuffColorFilter>(colorFilter)

        check("getColorFilter().getColor()")
            .that(Integer.toHexString(shadowOf(colorFilter).color))
            .isEqualTo(Integer.toHexString(color))
    }

    fun hasColorFilter(color: Color) {
        hasColorFilter(color.toArgb())
    }

    fun hasColorFilter(color: String) {
        hasColorFilter(android.graphics.Color.parseColor(color))
    }

    companion object {
        fun imageViews(): Factory<ImageViewSubject, ImageView> {
            return Factory<ImageViewSubject, ImageView> { metadata, actual ->
                ImageViewSubject(metadata, actual)
            }
        }

        fun assertThat(view: ImageView?): ImageViewSubject =
            assertAbout(imageViews()).that(view)
    }
}
