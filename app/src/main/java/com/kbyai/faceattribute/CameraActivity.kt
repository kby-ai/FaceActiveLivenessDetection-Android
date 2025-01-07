package com.kbyai.faceattribute

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.*
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.kbyai.faceattribute.FaceRectTransformer
import com.kbyai.faceattribute.FaceRectView
import com.kbyai.faceattribute.R
import com.kbyai.faceattribute.SettingsActivity
import com.kbyai.faceattribute.Utils
import com.kbyai.facesdk.FaceSDK
import com.kbyai.facesdk.FaceBox
import com.kbyai.facesdk.FaceDetectionParam

import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame
import io.fotoapparat.selector.front
import io.fotoapparat.selector.highestResolution
import io.fotoapparat.util.FrameProcessor
import io.fotoapparat.view.CameraView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.*
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.thread
import kotlin.math.acos
import kotlin.math.sqrt


class CameraActivity : AppCompatActivity() {

    private enum class State {
        IDLE,
        START_LOOK_UP,
        LOOK_UP,
        START_NOD,
        NOD,
        START_LOOK_UP_NOD,
        LOOK_UP_NOD,
        START_NOD_LOOK_UP,
        NOD_LOOK_UP,
        START_ZOOM_IN,
        ZOOM_IN,
        START_ZOOM_OUT,
        ZOOM_OUT,
        START_ZOOM_IN_OUT,
        ZOOM_IN_OUT,
        START_ZOOM_OUT_IN,
        ZOOM_OUT_IN,
        START_MOUTH,
        MOUTH,
        START_EYE_BLINK,
        EYE_BLINK,
        START_TURN_LEFT,
        TURN_LEFT,
        START_TURN_RIGHT,
        TURN_RIGHT,
        START_TURN_LEFT_RIGHT,
        TURN_LEFT_RIGHT,
        START_TURN_RIGHT_LEFT,
        TURN_RIGHT_LEFT,
        LIVENESS_CHECK_COMPLETED,
        END
    }

    public enum class ROI_CHECK_RESULT {
        ROI_NO_FACE,
        ROI_INTERSECTS,
        ROI_SMALL_FACE,
        ROI_FACE_OK
    }

    companion object {
        private val TAG = CameraActivity::class.simpleName
        private const val ALLOW_NO_FACE_TIMES = 2
        private const val FACING_CAMERA_KEEP_TIME = 3000L

        val PREVIEW_WIDTH = 720
        val PREVIEW_HEIGHT = 1280

        const val RESULT_KEY_FACING_CAMERA_IMAGE_PATH = "facing_image"
        const val RESULT_KEY_SMILING_IMAGE_PATH = "smiling_image"
        const val RESULT_KEY_MOUTH_IMAGE_PATH = "mouth_image"
        const val RESULT_KEY_SHAKE_IMAGE_PATH = "shake_image"

        const val HANDLE_UPDATE_FACE = 0
        const val HANDLE_TOAST_SHOW = 1
        const val HANDLE_SET_TITLE = 2
        const val HANDLE_VIEW_MODE = 3
        const val HANDLE_START_TIMER = 4
        const val HANDLE_STOP_TIMER = 5
        const val HANDLE_VIBRATOR = 6
    }

    private lateinit var imageScene: ImageView
    private lateinit var txtWarning: TextView

    private var warningMessage = ""
    private val actionsList: List<State> = listOf(State.START_LOOK_UP, State.START_NOD, State.START_LOOK_UP_NOD, State.START_NOD_LOOK_UP,
        State.START_ZOOM_IN, State.START_ZOOM_OUT, State.START_ZOOM_IN_OUT, State.START_ZOOM_OUT_IN,
        State.START_MOUTH, State.START_EYE_BLINK, State.START_TURN_LEFT, State.START_TURN_RIGHT, State.START_TURN_LEFT_RIGHT, State.START_TURN_RIGHT_LEFT)
    private var actionsIdxs = ArrayList<Int>()
    private var currentActionIdx = 0
    private var context: Context? = null
    private var cameraView: CameraView? = null
    private var textView:TextView?=null
    private var txtTimer: TextView?= null
    private var progressTimer: ProgressBar? = null
    private var rectanglesView: FaceRectView? = null
    private var faceRectTransformer: FaceRectTransformer? = null
    private var frontFotoapparat: Fotoapparat? = null

    lateinit var lytPrepareFaceCapture: ConstraintLayout
    lateinit var imgFaceCapture: ImageView
    lateinit var txtFaceCapture: TextView
    lateinit var txtFaceCaptureWarning: TextView

    private var state: State = State.IDLE
    private var timer: CountDownTimer? = null

    private var facingStartTime = 0L
    private var hasShakeToLeft = false
    private var hasShakeToRight = false
    private var hasLookUp = false
    private var hasLookNod = false
    private var hasZoomIn = false
    private var hasZoomOut = false
    private var lastEyeClosed = false
    private var faceCaptured = 0

    private var yawThreshold = 0.0f
    private var rollThreshold = 0.0f
    private var pitchThreshold = 0.0f
    private var maxLivenessCount = 0
    private var minimumLivenessRange = 0
    private var maximumLivenessRange = 0
    private var timeoutEachActions = 0
    private var timeoutFaceCapture = 0
    private var hasPostProcess = false
    private var hasCoolDown = false
    private var postProcessAddress = ""
    private var minimumLuminance = 0
    private var maximumLuminance = 0

    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            val i: Int = msg.what
            if (i == HANDLE_UPDATE_FACE) {
                var detectionResult = msg.obj as ArrayList<FaceBox>
                rectanglesView!!.setHasFace(detectionResult.count() > 0)
            } else if(i == HANDLE_TOAST_SHOW) {
                val str: String = msg.obj as String
                Toast.makeText(
                    context,
                    str,
                    Toast.LENGTH_SHORT
                ).show()
            } else if(i == HANDLE_SET_TITLE) {
                val str: String = msg.obj as String
                textView!!.text = str
            } else if(i == HANDLE_VIEW_MODE) {
                rectanglesView!!.setMode(msg.obj as FaceRectView.DispState)
            } else if(i == HANDLE_START_TIMER) {
                val timeOut: Int = msg.obj as Int
                startCaptureTimer(timeOut)
            } else if(i == HANDLE_STOP_TIMER) {
                timer?.cancel()

                progressTimer?.progress = timeoutEachActions
                txtTimer?.text = "" + timeoutEachActions + "s"
            } else if(i == HANDLE_VIBRATOR) {
                val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    //deprecated in API 26
                    v.vibrate(500)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        context = this
        cameraView = findViewById<View>(R.id.camera_view) as CameraView
        rectanglesView = findViewById<View>(R.id.rectanglesView) as FaceRectView
        textView = findViewById<View>(R.id.textView) as TextView
        txtTimer = findViewById<View>(R.id.txtTimer) as TextView
        txtWarning = findViewById<TextView>(R.id.txtWarning)
        progressTimer = findViewById<View>(R.id.progressBar) as ProgressBar
        lytPrepareFaceCapture = findViewById<ConstraintLayout>(R.id.lytPrepareFaceCapture)
        imgFaceCapture = findViewById<ImageView>(R.id.imgFaceCapture)
        txtFaceCapture = findViewById<TextView>(R.id.txtFaceCaptureResult)
        txtFaceCaptureWarning = findViewById<TextView>(R.id.txtFaceCaptureWarning)

        var tempActionsIdxs = ArrayList<Int>()
        for(i in actionsList.indices)
            tempActionsIdxs.add(i)

        for(i in actionsList.indices) {
            val rand = (Math.random() * 100).toInt() % tempActionsIdxs.size
            actionsIdxs.add(tempActionsIdxs.get(rand))
            tempActionsIdxs.removeAt(rand)
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        yawThreshold = sharedPreferences.getString("valid_yaw_angle", "" + SettingsActivity.DEFAULT_VALID_YAW_ANGLE)!!.toFloat()
        rollThreshold = sharedPreferences.getString("valid_roll_angle", "" + SettingsActivity.DEFAULT_VALID_ROLL_ANGLE)!!.toFloat()
        pitchThreshold = sharedPreferences.getString("valid_pitch_angle", "" + SettingsActivity.DEFAULT_VALID_PITCH_ANGLE)!!.toFloat()
        minimumLivenessRange = sharedPreferences.getString("minimum_range", "" + SettingsActivity.DEFAULT_MINIMUM_RANGE)!!.toInt()
        maximumLivenessRange = sharedPreferences.getString("maximum_range", "" + SettingsActivity.DEFAULT_MAXIMUM_RANGE)!!.toInt()
        maxLivenessCount = minimumLivenessRange + (Math.random() * (maximumLivenessRange + 1)).toInt() % (maximumLivenessRange - minimumLivenessRange + 1)
        timeoutEachActions = sharedPreferences.getString("liveness_timeout", "" + SettingsActivity.DEFAULT_TIMEOUT_EACH_ACTION)!!.toInt()
        timeoutFaceCapture = sharedPreferences.getString("face_capture_timeout", "" + SettingsActivity.DEFAULT_TIMEOUT_FACE_CAPTURE)!!.toInt()
        hasPostProcess = sharedPreferences.getBoolean("post_process_enable", SettingsActivity.DEFAULT_POST_PROCESS_ENABLE)
        hasCoolDown = sharedPreferences.getBoolean("cool_down_enable", SettingsActivity.DEFAULT_COOL_DOWN_ENABLE)
        postProcessAddress = sharedPreferences.getString("address_of_api", SettingsActivity.DEFAULT_POST_PROCESS_ADDRESS).toString()
        minimumLuminance = sharedPreferences.getString("minimum_range_luminance", "" + SettingsActivity.DEFAULT_MIN_RANGE_LUM)!!.toInt()
        maximumLuminance = sharedPreferences.getString("maximum_range_luminance", "" + SettingsActivity.DEFAULT_MAX_RANGE_LUM)!!.toInt()

        progressTimer?.min = 0
        progressTimer?.max = timeoutEachActions
        progressTimer?.progress = timeoutEachActions
        txtTimer?.text = "" + timeoutEachActions + "s"

        frontFotoapparat = Fotoapparat.with(this)
            .into(cameraView!!)
            .lensPosition(front())
            .frameProcessor(SampleFrameProcessor())
            .previewResolution { Resolution(PREVIEW_HEIGHT,PREVIEW_WIDTH) }
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            frontFotoapparat!!.start()
        }
    }


    override fun onStop() {
        super.onStop()
        timer?.cancel()
        try {
            frontFotoapparat!!.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            frontFotoapparat!!.start()
        }
    }

    fun adjustPreview(width: Int, height: Int) : Boolean{
        if(faceRectTransformer == null) {
            val frameSize: Size = Size(width, height);
            if(cameraView!!.measuredWidth == 0)
                return false;

            var displayOrientation: Int = 90;
            adjustPreviewViewSize (cameraView!!,
                cameraView!!, rectanglesView!!,
                Size(frameSize.width, frameSize.height), displayOrientation, 1.0f);

            faceRectTransformer = FaceRectTransformer (
                frameSize.height, frameSize.width,
                cameraView!!.getLayoutParams().width, cameraView!!.getLayoutParams().height,
                0, 1, false,
                false,
                false);

            return true;
        }

        return true;
    }

    private fun adjustPreviewViewSize(
        rgbPreview: View,
        previewView: View,
        faceRectView: FaceRectView,
        previewSize: Size,
        displayOrientation: Int,
        scale: Float
    ): ViewGroup.LayoutParams? {
        val layoutParams = previewView.layoutParams
        val measuredWidth = previewView.measuredWidth
        val measuredHeight = previewView.measuredHeight
        layoutParams.width = measuredWidth
        layoutParams.height = measuredHeight
//        previewView.layoutParams = layoutParams

        faceRectView.layoutParams.width = measuredWidth
        faceRectView.layoutParams.height = measuredHeight
        return layoutParams
    }

    /* access modifiers changed from: private */ /* access modifiers changed from: public */
    private fun sendMessage(w: Int, o: Any?) {
        val message = Message()
        message.what = w as Int
        message.obj = o
        mHandler.sendMessage(message)
    }

    inner class SampleFrameProcessor : FrameProcessor {
        var frThreadQueue: LinkedBlockingQueue<Runnable>? = null
        var frExecutor: ExecutorService? = null
        init {
            frThreadQueue = LinkedBlockingQueue<Runnable>(1)
            frExecutor = ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, frThreadQueue
            ) { r: Runnable? ->
                val t = Thread(r)
                t.name = "frThread-" + t.id
                t
            }
        }

        override fun invoke(frame: Frame) {
            if(state == State.END)
                return

            val bitmap = FaceSDK.yuv2Bitmap(frame.image, frame.size.width, frame.size.height, 7)

            val faceDetectionParam = FaceDetectionParam()
            faceDetectionParam.check_face_occlusion = true
            faceDetectionParam.check_eye_closeness = true
            faceDetectionParam.check_mouth_opened = true
            val faceResults = FaceSDK.faceDetection(bitmap, faceDetectionParam)
            Log.e("TestEngine", "face result count " + faceResults.size)
            val faceCount = faceResults?.count() ?: 0
            var face: FaceBox? = null
            if (faceResults != null && !faceResults.isEmpty()) {
                face = faceResults[0]
            }

            if (faceCount == 0) {
                if(state != State.IDLE && state != State.LIVENESS_CHECK_COMPLETED) {
                    state = State.IDLE
                    endProcess("Liveness check failed")
                    return
                }
            } else if (faceCount > 1) {
//                endProcess("Please make sure there is only one face on the screen.")
//                return
            }

            when (state) {
                State.IDLE -> {
                    setTitle("Place your face in center")
                    setViewMode(FaceRectView.DispState.NO_FACE)

                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setWarning("No face")
                        facingStartTime = 0L
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setWarning("Fit in circle")
                        facingStartTime = 0L
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setWarning("Move closer")
                        facingStartTime = 0L
                    } else if(!isFacingCamera(face)) {
                        setWarning("See front")
                        facingStartTime = 0L
                    } else {
                        if(facingStartTime == 0L) {
                            facingStartTime = System.currentTimeMillis()
                            setWarning("")
                        } else if(System.currentTimeMillis() - facingStartTime >= FACING_CAMERA_KEEP_TIME){

                            postProcess(state.toString())
                            gotoNextAction()
                        }
                    }
                }
                State.START_LOOK_UP -> {
                    setViewMode(FaceRectView.DispState.ROUND_NORMAL)
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setTitle( "Place your face in center")
                        setWarning("Fit in circle")
                    } else if(!isFacingCamera(face)) {
                        setTitle( "Place your face in center")
                        setWarning("See front")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("Move closer")
                    } else {
                        setTitle("Look Up")
                        setWarning("")
                        startTimer(timeoutEachActions)
                        state = State.LOOK_UP
                        hasLookUp = false
                    }
                }
                State.LOOK_UP -> {
                    val pitch = face?.pitch ?: 0f
                    val thresholdLookup = -pitchThreshold
                    if (pitch < thresholdLookup && !hasLookUp) {
                        hasLookUp = true
                    }

                    if (hasLookUp) {
                        postProcess(state.toString())
                        gotoNextAction()
                    }
                }
                State.START_NOD -> {
                    setViewMode(FaceRectView.DispState.ROUND_NORMAL)
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setTitle( "Place your face in center")
                        setWarning("Fit in circle")
                    } else if(!isFacingCamera(face)) {
                        setTitle( "Place your face in center")
                        setWarning("See front")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("Move closer")
                    } else {
                        setTitle( "Look Down")
                        setWarning("")
                        startTimer(timeoutEachActions)
                        state = State.NOD
                        hasLookNod = false
                    }
                }
                State.NOD -> {
                    val pitch = face?.pitch ?: 0f
                    val thresholdLookNod = pitchThreshold
                    if (pitch > thresholdLookNod && !hasLookNod) {
                        hasLookNod = true
                    }
                    if (hasLookNod) {
                        postProcess(state.toString())
                        gotoNextAction()
                    }
                }
                State.START_LOOK_UP_NOD -> {
                    setViewMode(FaceRectView.DispState.ROUND_NORMAL)
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setTitle( "Place your face in center")
                        setWarning("Fit in circle")
                    } else if(!isFacingCamera(face)) {
                        setTitle( "Place your face in center")
                        setWarning("See front")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("Move closer")
                    } else {
                        setTitle( "Look Up => Down")
                        setWarning("")
                        startTimer(timeoutEachActions)
                        state = State.LOOK_UP_NOD
                        hasLookUp = false
                        hasLookNod = false
                    }
                }
                State.LOOK_UP_NOD -> {
                    val pitch = face?.pitch ?: 0f
                    val thresholdLookup = -pitchThreshold
                    if (pitch < thresholdLookup && !hasLookUp && !hasLookNod) {
                        hasLookUp = true
                        postProcess(state.toString() + "1")
                    }

                    val thresholdLookNod = 12f
                    if (pitch > thresholdLookNod && hasLookUp && !hasLookNod) {
                        hasLookNod = true
                        postProcess(state.toString() + "2")
                    }

                    if (hasLookUp && hasLookNod) {
                        gotoNextAction()
                    }
                }
                State.START_NOD_LOOK_UP -> {
                    setViewMode(FaceRectView.DispState.ROUND_NORMAL)
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setTitle( "Place your face in center")
                        setWarning("Fit in circle")
                    } else if(!isFacingCamera(face)) {
                        setTitle( "Place your face in center")
                        setWarning("See front")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("Move closer")
                    } else {
                        setTitle( "Look Down => Up")
                        setWarning("")
                        startTimer(timeoutEachActions)
                        state = State.NOD_LOOK_UP
                        hasLookUp = false
                        hasLookNod = false
                    }
                }
                State.NOD_LOOK_UP -> {
                    val pitch = face?.pitch ?: 0f
                    val thresholdLookNod = pitchThreshold
                    if (pitch > thresholdLookNod && !hasLookUp && !hasLookNod) {
                        hasLookNod = true
                        postProcess(state.toString() + "1")
                    }

                    val thresholdLookup = -pitchThreshold
                    if (pitch < thresholdLookup && !hasLookUp && hasLookNod) {
                        hasLookUp = true
                        postProcess(state.toString() + "2")
                    }

                    if (hasLookUp && hasLookNod) {
                        gotoNextAction()
                    }
                }
                State.START_ZOOM_IN -> {
                    setTitle("Zoom In")
                    setViewMode(FaceRectView.DispState.ROUND_ZOOM_IN)
                    startTimer(timeoutEachActions)
                    state = State.ZOOM_IN
                }
                State.ZOOM_IN -> {
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setWarning("Fit in circle")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setWarning("Move closer")
                    } else {
                        postProcess(state.toString())
                        gotoNextAction()
                    }
                }
                State.START_ZOOM_IN_OUT -> {
                    setTitle("Zoom In => Out")
                    setViewMode(FaceRectView.DispState.ROUND_ZOOM_IN)
                    startTimer(timeoutEachActions)
                    state = State.ZOOM_IN_OUT
                    hasZoomIn = false
                    hasZoomOut = false
                }
                State.ZOOM_IN_OUT -> {
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setWarning("Fit in circle")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setWarning("Move closer")
                    } else {

                        if(hasZoomIn == false && hasZoomOut == false) {
                            hasZoomIn = true
                            postProcess(state.toString() + "1")
                            setViewMode(FaceRectView.DispState.ROUND_ZOOM_OUT)
                        }
                        else if(hasZoomIn == true && hasZoomOut == false) {
                            postProcess(state.toString() + "2")
                            gotoNextAction()
                        }
                    }
                }
                State.START_ZOOM_OUT -> {
                    setTitle("Zoom Out")
                    setViewMode(FaceRectView.DispState.ROUND_ZOOM_OUT)
                    startTimer(timeoutEachActions)
                    state = State.ZOOM_OUT
                }
                State.ZOOM_OUT -> {
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setWarning("Fit in circle")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setWarning("Move closer")
                    } else {
                        postProcess(state.toString())
                        gotoNextAction()
                    }
                }
                State.START_ZOOM_OUT_IN -> {
                    setTitle("Zoom Out => In")
                    setViewMode(FaceRectView.DispState.ROUND_ZOOM_OUT)
                    startTimer(timeoutEachActions)
                    state = State.ZOOM_OUT_IN
                    hasZoomIn = false
                    hasZoomOut = false
                }
                State.ZOOM_OUT_IN -> {
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setWarning("Fit in circle")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setWarning("Move closer")
                    } else {
                        if(hasZoomIn == false && hasZoomOut == false) {
                            hasZoomOut = true
                            setViewMode(FaceRectView.DispState.ROUND_ZOOM_IN)
                            postProcess(state.toString() + "1")
                        } else if(hasZoomIn == false && hasZoomOut == true) {
                            postProcess(state.toString() + "2")
                            gotoNextAction()
                        }
                    }
                }
                State.START_MOUTH -> {
                    setViewMode(FaceRectView.DispState.ROUND_NORMAL)
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setTitle( "Place your face in center")
                        setWarning("Fit in circle")
                    } else if(!isFacingCamera(face)) {
                        setTitle( "Place your face in center")
                        setWarning("See front")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("Move closer")
                    } else {
                        setTitle( "Open your mouth")
                        setWarning("")

                        startTimer(timeoutEachActions)
                        state = State.MOUTH
                    }
                }
                State.MOUTH -> {
                    if (isMouthOpened(face)) {
                        postProcess(state.toString())
                        gotoNextAction()
                    }
                }
                State.START_EYE_BLINK -> {
                    setViewMode(FaceRectView.DispState.ROUND_NORMAL)
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setTitle( "Place your face in center")
                        setWarning("Fit in circle")
                    } else if(!isFacingCamera(face)) {
                        setTitle( "Place your face in center")
                        setWarning("See front")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("Move closer")
                    } else {
                        setTitle( "Blink your eyes")
                        setWarning("")

                        if(face!!.left_eye_closed < 0.5 && face!!.right_eye_closed < 0.5) {
                            startTimer(timeoutEachActions)
                            state = State.EYE_BLINK
                        }
                    }
                }
                State.EYE_BLINK -> {
                    if(isEyeBlinking(face)) {
                        postProcess(state.toString())
                        gotoNextAction()
                    }
                }
                State.START_TURN_LEFT -> {
                    setViewMode(FaceRectView.DispState.ROUND_NORMAL)
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setTitle( "Place your face in center")
                        setWarning("Fit in circle")
                    } else if(!isFacingCamera(face)) {
                        setTitle( "Place your face in center")
                        setWarning("See front")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("Move closer")
                    } else {
                        setTitle( "Look Left")
                        setWarning("")

                        startTimer(timeoutEachActions)
                        state = State.TURN_LEFT
                        hasShakeToLeft = false
                    }
                }
                State.TURN_LEFT -> {
                    val yaw = face?.yaw ?: 0f
                    val thresholdLeft = yawThreshold
                    if (yaw > thresholdLeft && !hasShakeToLeft) {
                        hasShakeToLeft = true
                    }
                    if (hasShakeToLeft) {
                        postProcess(state.toString())
                        gotoNextAction()
                    }
                }
                State.START_TURN_RIGHT -> {
                    setViewMode(FaceRectView.DispState.ROUND_NORMAL)
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setTitle( "Place your face in center")
                        setWarning("Fit in circle")
                    } else if(!isFacingCamera(face)) {
                        setTitle( "Place your face in center")
                        setWarning("See front")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("Move closer")
                    } else {
                        setTitle( "Look right")
                        setWarning("")

                        startTimer(timeoutEachActions)
                        state = State.TURN_RIGHT
                        hasShakeToRight = false
                    }
                }
                State.TURN_RIGHT -> {
                    val yaw = face?.yaw ?: 0f
                    val thresholdRight = -yawThreshold
                    if (yaw < thresholdRight && !hasShakeToRight) {
                        hasShakeToRight = true
                    }
                    if (hasShakeToRight) {
                        postProcess(state.toString())
                        gotoNextAction()
                    }
                }
                State.START_TURN_LEFT_RIGHT -> {
                    setViewMode(FaceRectView.DispState.ROUND_NORMAL)
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setTitle( "Place your face in center")
                        setWarning("Fit in circle")
                    } else if(!isFacingCamera(face)) {
                        setTitle( "Place your face in center")
                        setWarning("See front")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("Move closer")
                    } else {
                        setTitle( "Look Left => Right")
                        setWarning("")

                        startTimer(timeoutEachActions)
                        state = State.TURN_LEFT_RIGHT
                        hasShakeToLeft = false
                        hasShakeToRight = false

                    }
                }
                State.TURN_LEFT_RIGHT -> {
                    val yaw = face?.yaw ?: 0f
                    val thresholdLeft = yawThreshold
                    if (yaw > thresholdLeft && !hasShakeToLeft && !hasShakeToRight) {
                        hasShakeToLeft = true
                        postProcess(state.toString() + "1")
                    }

                    val thresholdRight = -yawThreshold
                    if (yaw < thresholdRight && hasShakeToLeft && !hasShakeToRight) {
                        hasShakeToRight = true
                        postProcess(state.toString() + "2")
                    }

                    if (hasShakeToLeft && hasShakeToRight) {
                        gotoNextAction()
                    }
                }
                State.START_TURN_RIGHT_LEFT -> {
                    setViewMode(FaceRectView.DispState.ROUND_NORMAL)
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("No face")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setTitle( "Place your face in center")
                        setWarning("Fit in circle")
                    } else if(!isFacingCamera(face)) {
                        setTitle( "Place your face in center")
                        setWarning("See front")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setTitle( "Place your face in center")
                        setWarning("Move closer")
                    } else {
                        setTitle( "Look Right => Left")
                        setWarning("")

                        startTimer(timeoutEachActions)
                        state = State.TURN_RIGHT_LEFT
                        hasShakeToLeft = false
                        hasShakeToRight = false

                    }
                }
                State.TURN_RIGHT_LEFT -> {
                    val yaw = face?.yaw ?: 0f
                    val thresholdRight = -yawThreshold
                    if (yaw < thresholdRight && !hasShakeToLeft && !hasShakeToRight) {
                        hasShakeToRight = true
                    }

                    val thresholdLeft = yawThreshold
                    if (yaw > thresholdLeft && !hasShakeToLeft && hasShakeToRight) {
                        hasShakeToLeft = true
                        postProcess(state.toString() + "1")
                    }

                    if (hasShakeToLeft && hasShakeToRight) {
                        postProcess(state.toString() + "2")
                        gotoNextAction()
                    }
                }
                State.LIVENESS_CHECK_COMPLETED -> {

                    setViewMode(FaceRectView.DispState.ROUND_NORMAL)
                    setTitle( "Face Capture")
                    if(faceCaptured == 0) {
                        faceCaptured = 1
                        if(hasCoolDown) {
                            startTimer(timeoutFaceCapture)
                        }
                    }

                    val luminance = calculateLuminance(frame)
                    val faceInRect = isFaceInDetectionRect(face, frame.size.width, frame.size.height)
                    if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                        setWarning("No face")
                    } else if(luminance < minimumLuminance) {
                        setWarning("Low luminance")
                    } else if(luminance > maximumLuminance) {
                        setWarning("High luminance")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                        setWarning("Fit in circle")
                    } else if(!isFacingCamera(face)) {
                        setWarning("See front")
                    } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                        setWarning("Move closer")
                    } else if(isMouthOpened(face)) {
                        setWarning("Close mouth")
                    } else if(face!!.left_eye_closed > 0.5 || face!!.right_eye_closed > 0.5) {
                        setWarning("Do not blink")
                    } else {
                        setWarning("")
                        if(hasCoolDown == false) {
                            faceCaptured = 2
                        }
                    }

                    if(faceCaptured == 2) {
                        faceCaptured = 3
                        faceCapture(frame)
                    }
                }
                State.END -> {

                }
            }

            if(adjustPreview(frame.size.width, frame.size.height))
                sendMessage(HANDLE_UPDATE_FACE, faceResults)

        }
    }

    private fun startCaptureTimer(timeOut: Int) {

        timer?.cancel()
        timer = object: CountDownTimer((timeOut * 1000).toLong() + 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                runOnUiThread {
                    txtTimer?.text = (millisUntilFinished / 1000).toString() + "s"
                    progressTimer?.progress = (millisUntilFinished / 1000).toInt()
                }
            }

            override fun onFinish() {
                if(state == State.LIVENESS_CHECK_COMPLETED) {
                    faceCaptured = 2
                } else {
                    endProcess("Liveness check timeout")
                }
            }
        }
        timer?.start()
    }

    private fun isFaceInDetectionRect(face: FaceBox?, frameWidth: Int, frameHeight: Int): ROI_CHECK_RESULT {
        face ?: return ROI_CHECK_RESULT.ROI_NO_FACE
        val maxSize = Math.max(frameWidth, frameHeight)
        val minSize = Math.min(frameWidth, frameHeight)
        var sizeRate = 0.45f
        var interRate = 0.1f

        val viewWidth = rectanglesView!!.width
        val viewHeight = rectanglesView!!.height
        val minView = Math.min(viewWidth, viewHeight)
        val maxView = Math.max(viewWidth, viewHeight)
        val ratioView = minView / maxView.toFloat()
        val ratioFrame = minSize / maxSize.toFloat()

        var cropRect = Rect()
        if(state != State.ZOOM_IN && state != State.ZOOM_OUT && state != State.ZOOM_IN_OUT && state != State.ZOOM_OUT_IN) {
            val margin = minView / 6
            val rectHeight = (minView - 2 * margin) * 4 / 3
            cropRect = Rect(margin.toInt(),
                ((maxView - rectHeight) / 2).toInt(),
                (minView - margin).toInt(),
                ((maxView - rectHeight) / 2 + rectHeight).toInt()
            )
        } else if(state == State.ZOOM_OUT || (state == State.ZOOM_IN_OUT && hasZoomIn == true) || (state == State.ZOOM_OUT_IN && hasZoomOut == false)) {
            val margin = minView / 4
            val rectHeight = (minView - 2 * margin) * 4 / 3
            cropRect = Rect(margin.toInt(),
                ((maxView - rectHeight) / 2).toInt(),
                (minView - margin).toInt(),
                ((maxView - rectHeight) / 2 + rectHeight).toInt()
            )
            interRate = 0.01f
            sizeRate = 0.50f
        } else if(state == State.ZOOM_IN || (state == State.ZOOM_IN_OUT && hasZoomIn == false) || (state == State.ZOOM_OUT_IN && hasZoomOut == true)) {
            val margin = minView / 15
            val rectHeight = minView * 7 / 5
            cropRect = Rect(margin.toInt(),
                ((maxView - rectHeight) / 2).toInt(),
                (minView - margin).toInt(),
                ((maxView - rectHeight) / 2 + rectHeight).toInt()
            )
            sizeRate = 0.55f
        }

        var frameCropRect = Rect()
        if(ratioView < ratioFrame) {
            var dx = ((maxView * ratioFrame) - minView) / 2
            var dy = 0f
            var ratio = maxSize / maxView.toFloat()

            val x1 = (cropRect.left + dx) * ratio
            val y1 = (cropRect.top + dy) * ratio
            val x2 = (cropRect.right + dx) * ratio
            val y2 = (cropRect.bottom + dy) * ratio
            frameCropRect = Rect(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt())
        } else {
            var dx = 0f
            var dy = ((minView / ratioFrame) - maxView) / 2
            var ratio = maxSize / maxView.toFloat()

            val x1 = (cropRect.left + dx) * ratio
            val y1 = (cropRect.top + dy) * ratio
            val x2 = (cropRect.right + dx) * ratio
            val y2 = (cropRect.bottom + dy) * ratio
            frameCropRect = Rect(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt())
        }

        var faceLeft = Float.MAX_VALUE
        var faceRight = 0f
        var faceBottom = 0f
        for(i in 0..67) {
            faceLeft = Math.min(faceLeft, face.landmarks_68[i * 2])
            faceRight = Math.max(faceRight, face.landmarks_68[i * 2])
            faceBottom = Math.max(faceBottom, face.landmarks_68[i * 2 + 1])
        }


        val centerY = (face.y2 + face.y1) / 2
        val topY = centerY - (face.y2 - face.y1) * 2 / 3

        var interX = Math.max(0f, frameCropRect.left.toFloat() - faceLeft) + Math.max(0f, faceRight - frameCropRect.right.toFloat())
        var interY = Math.max(0f, frameCropRect.top.toFloat() - topY) + Math.max(0f, faceBottom - frameCropRect.bottom.toFloat())

        if(interX / frameCropRect.width().toFloat() > interRate || interY / frameCropRect.height().toFloat() > interRate) {
            return ROI_CHECK_RESULT.ROI_INTERSECTS
        }

        if((face.y2 - face.y1) * (face.x2 - face.x1) <  frameCropRect.width() * frameCropRect.height() * sizeRate) {
            return ROI_CHECK_RESULT.ROI_SMALL_FACE
        }

        return ROI_CHECK_RESULT.ROI_FACE_OK
    }

    private fun isFacingCamera(face: FaceBox?): Boolean {
        face ?: return false
        return face.roll < rollThreshold && face.roll > -rollThreshold
                && face.yaw < yawThreshold && face.yaw > -yawThreshold
                && face.pitch < pitchThreshold && face.pitch > -pitchThreshold
    }

    private fun isEyeBlinking(face:FaceBox?): Boolean {
        face ?: return false

        if(lastEyeClosed == false) {
            if(face.left_eye_closed > 0.5 && face.right_eye_closed > 0.5) {
                lastEyeClosed = true
                return false
            }
        } else {
            if(face.left_eye_closed < 0.5 && face.right_eye_closed < 0.5) {
                return true
            }
        }

        return false
    }

    private fun isMouthOpened(face: FaceBox?): Boolean {
        face ?: return false
        return face.mouth_opened > 0.5;
    }

    private fun lengthSquare(a: PointF, b: PointF): kotlin.Float {
        val x = a.x - b.x
        val y = a.y - b.y
        return x * x + y * y
    }

    private fun setTitle(msg:String) {
        sendMessage(HANDLE_SET_TITLE, msg)
    }

    private fun setViewMode(mode: FaceRectView.DispState) {
        sendMessage(HANDLE_VIEW_MODE, mode)
    }

    private fun startTimer(timeout: Int) {
        sendMessage(HANDLE_START_TIMER, timeout)
    }

    private fun setWarning(msg: String) {
        warningMessage = msg
        runOnUiThread { txtWarning.text = warningMessage }
    }

    private fun endProcess(msg: String) {
        state = State.END
        timer?.cancel()
        sendMessage(HANDLE_TOAST_SHOW, msg)
        finish()
    }

    private fun gotoNextAction() {

        setWarning("")
        sendMessage(HANDLE_STOP_TIMER, 0)
        sendMessage(HANDLE_VIBRATOR, 0)
        if(state == State.LIVENESS_CHECK_COMPLETED) {
            lytPrepareFaceCapture.visibility = View.VISIBLE

            val savePath = filesDir.path + "/capture.jpg"
            val bitmapCapture = Utils.getCorrectlyOrientedImage(context, savePath)
            imgFaceCapture.setImageBitmap(bitmapCapture)

            val faceDetectionParam = FaceDetectionParam()
            faceDetectionParam.check_face_occlusion = true
            faceDetectionParam.check_eye_closeness = true
            faceDetectionParam.check_mouth_opened = true
            val faceResults = FaceSDK.faceDetection(bitmapCapture, faceDetectionParam)
            var face: FaceBox? = null
            if (faceResults != null && !faceResults.isEmpty()) {
                face = faceResults[0]
            }

            val luminance = calcLuminanceFromBitmap(bitmapCapture)
            val faceInRect = isFaceInDetectionRect(face, bitmapCapture.width, bitmapCapture.height)
            if(faceInRect == ROI_CHECK_RESULT.ROI_NO_FACE) {
                txtFaceCapture.text = "Face capture failed!"
                txtFaceCaptureWarning.text = "No face"
            } else if(luminance < minimumLuminance) {
                txtFaceCapture.text = "Face capture failed!"
                txtFaceCaptureWarning.text = "Low luminance"
            } else if(luminance > maximumLuminance) {
                txtFaceCapture.text = "Face capture failed!"
                txtFaceCaptureWarning.text = "High luminance"
            } else if(faceInRect == ROI_CHECK_RESULT.ROI_INTERSECTS) {
                txtFaceCapture.text = "Face capture failed!"
                txtFaceCaptureWarning.text = "Fit in circle"
            } else if(!isFacingCamera(face)) {
                txtFaceCapture.text = "Face capture failed!"
                txtFaceCaptureWarning.text = "See front"
            } else if(faceInRect == ROI_CHECK_RESULT.ROI_SMALL_FACE) {
                txtFaceCapture.text = "Face capture failed!"
                txtFaceCaptureWarning.text = "Move closer"
            } else if(isMouthOpened(face)) {
                txtFaceCapture.text = "Face capture failed!"
                txtFaceCaptureWarning.text = "Close mouth"
            } else if(face!!.left_eye_closed > 0.5 || face!!.right_eye_closed > 0.5) {
                txtFaceCapture.text = "Face capture failed!"
                txtFaceCaptureWarning.text = "Do not blink"
            } else {
                txtFaceCapture.text = "Face capture succeed!"
                txtFaceCaptureWarning.text = ""

                if(hasPostProcess) {
                    val thread = thread(start = true) {
                        // Code to run in the new thread
                        FileUploadRunnable(savePath).run()
                    }
                }
            }
            state = State.END
        } else if(currentActionIdx >= maxLivenessCount) {
            state = State.LIVENESS_CHECK_COMPLETED
        } else {
            state = actionsList[actionsIdxs[currentActionIdx]]
        }
        currentActionIdx ++

        if(state == State.LIVENESS_CHECK_COMPLETED) {
            runOnUiThread {
                Toast.makeText(
                    context,
                    "Liveness Check Succeed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun Bitmap.flip(horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.preScale((if (horizontal) -1 else 1).toFloat(), (if (vertical) -1 else 1).toFloat())
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        val scaledBitmap = Bitmap.createScaledBitmap(this, width, height, true)
        return Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )
    }

    private fun postProcess(saveName: String) {
        if(hasPostProcess == false) {
            return
        }

        val savePath = filesDir.path + "/" + saveName
        val saveFile = File(savePath)
        if(!saveFile.exists()) {
            saveFile.createNewFile()
        }

        frontFotoapparat!!.takePicture().saveToFile(saveFile).whenAvailable {
            val thread = thread(start = true) {
                // Code to run in the new thread
                FileUploadRunnable(savePath).run()
            }
        }
    }

    private fun faceCapture(frame: Frame) {
        val savePath = filesDir.path + "/capture.jpg"
        val saveFile = File(savePath)
        if(!saveFile.exists()) {
            saveFile.createNewFile()
        }

        YuvImage(
            frame.image,
            ImageFormat.NV21,
            frame.size.width,
            frame.size.height,
            null
        ).let { yuvImage ->
            ByteArrayOutputStream().use { output ->
                yuvImage.compressToJpeg(
                    Rect(0, 0, frame.size.width, frame.size.height),
                    100,
                    output
                )
                output.toByteArray().apply {
                    BitmapFactory.decodeByteArray(this, 0, size)?.let { bitmap ->

                        val fixedBitmap = bitmap.rotate(90f).flip(
                            false, true
                        )

                        try {
                            val fos = FileOutputStream(saveFile)
                            fixedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                            fos.flush()
                            fos.close()

                            runOnUiThread {
                                frontFotoapparat!!.stop()
                                gotoNextAction()
                                if(hasPostProcess) {
                                    val thread = thread(start = true) {
                                        // Code to run in the new thread
                                        FileUploadRunnable(savePath).run()
                                    }
                                }
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    inner class FileUploadRunnable(saveName_: String) : Runnable {
        val saveName: String

        init {
            saveName = saveName_
        }

        override fun run() {
//            try {
//                // Load the certificate file
//                val inputStream: InputStream = resources.openRawResource(R.raw.mycert)
//                val certificateFactory = CertificateFactory.getInstance("X.509")
//                val serverCert = certificateFactory.generateCertificate(inputStream) as X509Certificate
//
//                val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
//                trustStore.load(null, null)
//                trustStore.setCertificateEntry("server", serverCert)
//
//                val trustManagerFactory = TrustManagerFactory.getInstance(
//                    TrustManagerFactory.getDefaultAlgorithm()
//                )
//                trustManagerFactory.init(trustStore)
//
//                val sslContext = SSLContext.getInstance("TLS")
//                sslContext.init(null, trustManagerFactory.trustManagers, SecureRandom())
//
//                val client = OkHttpClient.Builder()
//                    .addInterceptor(HttpLoggingInterceptor().apply {
//                        level = HttpLoggingInterceptor.Level.BODY
//                    })
//                    .sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as X509TrustManager)
//                    .build()
//
//                val file = File(saveName)
//                val requestBody: RequestBody = MultipartBody.Builder()
//                    .setType(MultipartBody.FORM)
//                    .addFormDataPart(
//                        "image",
//                        file.name,
//                        RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)
//                    )
//                    .build()
//
//                val request: Request = Builder()
//                    .url(postProcessAddress)
//                    .post(requestBody)
//                    .build()
//
//                val response = client.newCall(request).execute()
//                Log.e("TestEngine", "response: " + response)
//            } catch(e:Exception) {
//                e.printStackTrace()
//            }
        }
    }

    fun calculateLuminance(frame: Frame): Double {
        // Get the Y (luminance) plane
        val yBuffer = frame.image
        val yRowStride = frame.size.width
        val yPixelStride = 1

        val width = frame.size.width
        val height = frame.size.height

        var totalLuminance = 0.0
        var pixelCount = 0

        for (row in 0 until height) {
            for (col in 0 until width step yPixelStride) {
                val index = row * yRowStride + col
                val yValue = yBuffer.get(index).toInt() and 0xFF // Convert byte to unsigned int
                totalLuminance += yValue
                pixelCount++
            }
        }

        // Calculate the average luminance
        return if (pixelCount > 0) totalLuminance / pixelCount else 0.0
    }

    fun calcLuminanceFromBitmap(bitmap: Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height

        // Convert the Bitmap to ARGB pixels
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var sum = 0.0

        // Iterate through each pixel and calculate luminance
        for (pixel in pixels) {
            val R = (pixel shr 16) and 0xFF // Extract red channel
            val G = (pixel shr 8) and 0xFF  // Extract green channel
            val B = pixel and 0xFF         // Extract blue channel

            // Calculate Y (luminance) using the same formula
            val Y = ((66 * R + 129 * G + 25 * B + 128) shr 8) + 16
            sum += Y
        }

        // Return the average luminance
        return (sum / (width * height)).toInt()
    }

}