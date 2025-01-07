package com.ttv.facerecog

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.EditTextPreference

class FloatEditTextPreference(context: Context?, attrs: AttributeSet?) :
    EditTextPreference(context!!, attrs) {

    override fun getPersistedString(defaultReturnValue: String?): String? {
        return getPersistedFloat(0.0f).toString()
    }

    override fun persistString(value: String?): Boolean {
        return persistFloat(value!!.toFloat())
    }
}
