package com.kbyai.faceattribute

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.preference.*


class SettingsActivity : AppCompatActivity() {

    companion object {
        const val LIVENESS_MIN_COUNT = 1
        const val LIVENESS_MAX_COUNT = 14

        const val DEFAULT_VALID_YAW_ANGLE = 12.0f
        const val DEFAULT_VALID_ROLL_ANGLE = 12.0f
        const val DEFAULT_VALID_PITCH_ANGLE = 12.0f

        const val DEFAULT_MINIMUM_RANGE = 8
        const val DEFAULT_MAXIMUM_RANGE = 10

        const val DEFAULT_TIMEOUT_EACH_ACTION = 5
        const val DEFAULT_TIMEOUT_FACE_CAPTURE = 5
        const val DEFAULT_POST_PROCESS_ADDRESS = "https://xxx.xxx.com/post-process/"
        const val DEFAULT_POST_PROCESS_ENABLE = false
        const val DEFAULT_COOL_DOWN_ENABLE = false

        const val DEFAULT_MIN_RANGE_LUM = 80
        const val DEFAULT_MAX_RANGE_LUM = 190

        val TAG = SettingsActivity::class.simpleName

        class SettingsPreferenceFragment : PreferenceFragmentCompat() {
            override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
                setPreferencesFromResource(R.xml.root_preferences, rootKey)

                val yawPref = findPreference<EditTextPreference>("valid_yaw_angle")
                yawPref?.setSummary("-" + yawPref?.text + " ~ " + yawPref?.text)
                yawPref?.setOnPreferenceChangeListener{ preference, newValue ->
                    val stringPref = newValue as String
                    try {
                        if(stringPref.toFloat() < 0.0f) {
                            Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                            false
                        } else {
                            preference.setSummary("-" + stringPref + " ~ " + stringPref)
                            true
                        }
                    } catch (e:Exception) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    }
                }

                val rollPref = findPreference<EditTextPreference>("valid_roll_angle")
                rollPref?.setSummary("-" + rollPref?.text + " ~ " + rollPref?.text)
                rollPref?.setOnPreferenceChangeListener{ preference, newValue ->
                    val stringPref = newValue as String
                    try {
                        if(stringPref.toFloat() < 0.0f) {
                            Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                            false
                        } else {
                            preference.setSummary("-" + stringPref + " ~ " + stringPref)
                            true
                        }
                    } catch (e:Exception) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    }
                }

                val pitchPref = findPreference<EditTextPreference>("valid_pitch_angle")
                pitchPref?.setSummary("-" + pitchPref?.text + " ~ " + pitchPref?.text)
                pitchPref?.setOnPreferenceChangeListener{ preference, newValue ->
                    val stringPref = newValue as String
                    try {
                        if(stringPref.toFloat() < 0.0f) {
                            Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                            false
                        } else {
                            preference.setSummary("-" + stringPref + " ~ " + stringPref)
                            true
                        }
                    } catch (e:Exception) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    }
                }


                val minmumPref = findPreference<EditTextPreference>("minimum_range")
                val maxmumPref = findPreference<EditTextPreference>("maximum_range")
                minmumPref?.setSummary(minmumPref?.text)
                minmumPref?.setOnPreferenceChangeListener{ preference, newValue ->
                    val stringPref = newValue as String
                    try {
                        if(stringPref.toInt() < LIVENESS_MIN_COUNT || stringPref.toInt() > LIVENESS_MAX_COUNT) {
                            Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                            false
                        } else if(stringPref.toInt() > maxmumPref!!.text!!.toInt()) {
                            Toast.makeText(context, getString(R.string.minimum_value_less), Toast.LENGTH_SHORT).show()
                            false
                        } else {
                            preference.setSummary(stringPref)
                            true
                        }
                    } catch (e:Exception) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    }
                }

                maxmumPref?.setSummary(maxmumPref?.text)
                maxmumPref?.setOnPreferenceChangeListener{ preference, newValue ->
                    val stringPref = newValue as String
                    try {
                        if(stringPref.toInt() < LIVENESS_MIN_COUNT || stringPref.toInt() > LIVENESS_MAX_COUNT) {
                            Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                            false
                        } else if(stringPref.toInt() < minmumPref!!.text!!.toInt()) {
                            Toast.makeText(context, getString(R.string.maximum_value_less), Toast.LENGTH_SHORT).show()
                            false
                        } else {
                            preference.setSummary(stringPref)
                            true
                        }
                    } catch (e:Exception) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    }
                }

                val timeoutPref = findPreference<EditTextPreference>("liveness_timeout")
                timeoutPref?.setSummary(timeoutPref.text)
                timeoutPref?.setOnPreferenceChangeListener{ preference, newValue ->
                    val stringPref = newValue as String
                    try {
                        if(stringPref.toInt() < 2 || stringPref.toInt() > 15) {
                            Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                            false
                        } else {
                            preference.setSummary(stringPref)
                            true
                        }
                    } catch (e:Exception) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    }
                }

                val addressPref = findPreference<EditTextPreference>("address_api")
                addressPref?.setSummary(addressPref.text)
                addressPref?.setOnPreferenceChangeListener{ preference, newValue ->
                    val stringPref = newValue as String
                    preference.setSummary(stringPref)
                    true
                }

                val captureTimeoutPref = findPreference<EditTextPreference>("face_capture_timeout")
                captureTimeoutPref?.setSummary(captureTimeoutPref.text)
                captureTimeoutPref?.setOnPreferenceChangeListener{ preference, newValue ->
                    val stringPref = newValue as String
                    try {
                        if(stringPref.toInt() < 2 || stringPref.toInt() > 15) {
                            Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                            false
                        } else {
                            preference.setSummary(stringPref)
                            true
                        }
                    } catch (e:Exception) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    }
                }


                val minmumLumPref = findPreference<EditTextPreference>("minimum_range_luminance")
                val maxmumLumPref = findPreference<EditTextPreference>("maximum_range_luminance")
                minmumLumPref?.setSummary(minmumLumPref?.text)
                minmumLumPref?.setOnPreferenceChangeListener{ preference, newValue ->
                    val stringPref = newValue as String
                    try {
                        if(stringPref.toInt() < 0 || stringPref.toInt() > 255) {
                            Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                            false
                        } else if(stringPref.toInt() > maxmumLumPref!!.text!!.toInt()) {
                            Toast.makeText(context, getString(R.string.minimum_value_less), Toast.LENGTH_SHORT).show()
                            false
                        } else {
                            preference.setSummary(stringPref)
                            true
                        }
                    } catch (e:Exception) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    }
                }

                maxmumLumPref?.setSummary(maxmumLumPref?.text)
                maxmumLumPref?.setOnPreferenceChangeListener{ preference, newValue ->
                    val stringPref = newValue as String
                    try {
                        if(stringPref.toInt() < 0 || stringPref.toInt() > 255) {
                            Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                            false
                        } else if(stringPref.toInt() < minmumLumPref!!.text!!.toInt()) {
                            Toast.makeText(context, getString(R.string.maximum_value_less), Toast.LENGTH_SHORT).show()
                            false
                        } else {
                            preference.setSummary(stringPref)
                            true
                        }
                    } catch (e:Exception) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsPreferenceFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}