package com.r7tkeysx.app

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class ImeTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = EditText(this).apply {
            hint = "Ô nhập thật để thử bàn phím"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            gravity = Gravity.TOP
            textSize = 20f
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 160, 48, 48)
            addView(TextView(context).apply { text = "R7TKeysX · thử bàn phím" ; textSize = 18f })
            addView(box, LinearLayout.LayoutParams(-1, 600))
        })
        box.requestFocus()
    }
}
