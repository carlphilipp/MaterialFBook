/*
 * Created by ZeeRooo
 * https://github.com/ZeeRooo
 */
package me.zeeroooo.materialfb.ui

import android.content.Context
import android.support.annotation.CheckResult
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.AppCompatTextView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import me.zeeroooo.materialfb.R

object CookingAToast {

    @CheckResult
    fun cooking(context: Context, message_to_show: CharSequence, text_color: Int, background: Int, icon_toast: Int, duration: Boolean): Toast {
        val view = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.cooking_a_toast, null)
        view.setBackgroundColor(background)

        val icon = view.findViewById<AppCompatImageView>(R.id.icon)
        icon.setImageResource(icon_toast)

        val message = view.findViewById<AppCompatTextView>(R.id.message)
        message.text = message_to_show
        message.setTextColor(text_color)

        val toast = Toast(context)
        toast.view = view
        toast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.BOTTOM, 0, 0)

        toast.duration = if (duration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT

        return toast
    }
}