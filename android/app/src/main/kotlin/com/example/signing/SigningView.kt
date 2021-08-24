package com.example.signing

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView


class SigningView(context: Context, id: Int, creationParams: Map<String?, Any?>?) : PlatformView {
    private var textView: TextView

    override fun getView(): View {
        return textView
    }

    override fun dispose() {}

    fun setText(newNumber: Int): Int {
        textView.text = newNumber.toString()
        return newNumber*100
    }

    init {
        textView = TextView(context)
        textView.textSize = 32f
        textView.setBackgroundColor(Color.rgb(255, 255, 255))
        textView.text = "Rendered on a native Android view (id: $id)"
    }
}