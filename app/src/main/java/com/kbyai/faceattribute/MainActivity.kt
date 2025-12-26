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
            "hPzN4muPydY5qAH0WWw58J/YFLM2z2gYd2gTWFTwo8vFUhpJ47ojVnICY1mOFwBdzzgtkrhqAO4Rq" + 
                "dg2EiTjI0n66rwt2IHvflmj1Xnackh/KHgxZTNVJSgcv6v9+zqI3W+aZwNSBckZ+UalzKVbrQoN+LLno+" + 
                "X8fTD9MWla8uQfudaHcmh238/5V9J1nuzzHB55X18DVkMGuWqlfQe5G8vzkjxY9qX91RIdMUEoHIlnocBZ" + 
                "IfRiNCOY3NQ+DhGZcKEV+v0PyKf8jOHSUgHglyZ78Gp1H8qQskUYl3R+nb4v5QhEW3b3MxIj2YBgzPq81B" + 
                "3Yh48qKuEWnFduuQ5ZFw=="
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
