package com.kylecorry.heart_rate.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ImageButton
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.setImageColor
import com.kylecorry.heart_rate.R

object UiUtils {
    fun setButtonState(button: ImageButton, state: Boolean) {
        setButtonState(
            button,
            state,
            Resources.getAndroidColorAttr(button.context, androidx.appcompat.R.attr.colorPrimary),
            Resources.color(button.context, R.color.colorSecondary)
        )
    }

    fun ImageButton.flatten() {
        backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        elevation = 0f
    }

    private fun setButtonState(
        button: ImageButton,
        isOn: Boolean,
        @ColorInt primaryColor: Int,
        @ColorInt secondaryColor: Int
    ) {
        if (isOn) {
            setImageColor(button.drawable, secondaryColor)
            button.backgroundTintList = ColorStateList.valueOf(primaryColor)
        } else {
            setImageColor(button.drawable, Resources.androidTextColorSecondary(button.context))
            button.backgroundTintList =
                ColorStateList.valueOf(Resources.androidBackgroundColorSecondary(button.context))
        }
    }
}