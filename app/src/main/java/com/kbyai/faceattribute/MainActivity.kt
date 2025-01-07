package com.kbyai.faceattribute

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.kbyai.faceattribute.SettingsActivity
import com.kbyai.facesdk.FaceBox
import com.kbyai.facesdk.FaceDetectionParam
import com.kbyai.facesdk.FaceSDK
import com.kbyai.faceattribute.CameraActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    companion object {
        private val SELECT_PHOTO_REQUEST_CODE = 1
        private val SELECT_ATTRIBUTE_REQUEST_CODE = 2
    }

    private lateinit var textWarning: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textWarning = findViewById<TextView>(R.id.textWarning)


        var ret = FaceSDK.setActivation(
            "DYv3mu71v8b9hqraZETNpg+CdriRbg1qLLMbfNqTeGdvSdbGPGudJpiVR4Tl9TEcJrZrZG+59ay6\n" +
                    "BtL78C1VsDGsOzSKw9ssETgVnT9DIc/LdNrqs4/o7o3nO0ZPz3iNu/P2jKkUXLo/uzh+aaVLbi55\n" +
                    "X9NQYhD5EHFqL2mLtGxcfqccTHLMW0MIe0Wq65hzPIrR6oh7tvtKzX5EcOOPv8UK2a3i9+MgtG4Y\n" +
                    "b+CHoQ0lNJhmZkpdKmcRidibJLKgwJqDPiZfwsW6C3hcrNNo6T8T+NMZ4W7rHcQKfdSr0yXtYqCr\n" +
                    "kaMKrGzlk8nYubwfqZGeAKSyjuL8mWWgY57I3Q=="
        )

        if (ret == FaceSDK.SDK_SUCCESS) {
            ret = FaceSDK.init(assets)
        }

        if (ret != FaceSDK.SDK_SUCCESS) {
            textWarning.setVisibility(View.VISIBLE)
            if (ret == FaceSDK.SDK_LICENSE_KEY_ERROR) {
                textWarning.setText("Invalid license!")
            } else if (ret == FaceSDK.SDK_LICENSE_APPID_ERROR) {
                textWarning.setText("Invalid error!")
            } else if (ret == FaceSDK.SDK_LICENSE_EXPIRED) {
                textWarning.setText("License expired!")
            } else if (ret == FaceSDK.SDK_NO_ACTIVATED) {
                textWarning.setText("No activated!")
            } else if (ret == FaceSDK.SDK_INIT_ERROR) {
                textWarning.setText("Init error!")
            }
        }

        findViewById<Button>(R.id.buttonCapture).setOnClickListener {
            startActivity(Intent(this, CaptureActivityKt::class.java))
        }

        findViewById<Button>(R.id.buttonActive).setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        findViewById<Button>(R.id.buttonSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.buttonAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.lytBrand).setOnClickListener {
            val browse = Intent(Intent.ACTION_VIEW, Uri.parse("https://kby-ai.com"))
            startActivity(browse)
        }
    }


    override fun onResume() {
        super.onResume()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {

        } else if (requestCode == SELECT_ATTRIBUTE_REQUEST_CODE && resultCode == RESULT_OK) {
        }
    }
}