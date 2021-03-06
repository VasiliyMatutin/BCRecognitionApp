package com.hse.vasiliy.bcrecognition.recognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.util.Log
import java.util.*
import android.hardware.camera2.CaptureRequest
import android.os.SystemClock.sleep
import android.view.*
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import com.hse.vasiliy.bcrecognition.*


class CameraFragment : Fragment() {

    private lateinit var attachedActivityContext: Context
    private lateinit var activity: MainActivity

    private var applicationTag = "CameraFragment"

    private lateinit var cameraView: TextureView
    private lateinit var flashSwitch: ImageView
    private lateinit var mLayout: View
    private lateinit var topBorder: View
    private lateinit var lowerBorder: View
    private lateinit var cameraId: String
    //next two required by camera2 docs to perform huge camera tasks in separate thread
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest
    private var mCameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var isFlashSupported = false
    private var isFlashRequired = false
    private var captureState = STATE_PREVIEW

    //overridden callbacks for camera
    private val cameraCallbacks = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
            Log.e(applicationTag, "Camera error")
            activity.showErrorByRequest(getString(R.string.camera_access_error))
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {
            when (captureState) {
                STATE_PREVIEW -> Unit
                STATE_WAITING_LOCK -> capturePicture(result)
                STATE_WAITING_PRE_CAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        captureState =
                            STATE_WAITING_NON_PRE_CAPTURE
                        isFlashRequired = true
                    } else if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        captureState =
                            STATE_WAITING_NON_PRE_CAPTURE
                    }
                }
                STATE_WAITING_NON_PRE_CAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        captureState =
                            STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
            }
        }

        private fun capturePicture(result: CaptureResult) {
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            if (afState == null) {
                captureState = STATE_PICTURE_TAKEN
                captureStillPicture()
            } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    captureState =
                        STATE_PICTURE_TAKEN
                    captureStillPicture()
                } else {
                    runPreCaptureSequence()
                }
            }
        }

        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult) {
            process(partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {
            process(result)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            attachedActivityContext = context
            activity = attachedActivityContext as MainActivity
        } else {
            Log.e(applicationTag, "Cannot attach fragment to activity")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.camera_fragment, container, false)
        mLayout = view.findViewById(R.id.main_layout)
        topBorder = view.findViewById(R.id.top_border)
        lowerBorder = view.findViewById(R.id.lower_border)
        cameraView = view.findViewById(R.id.camera_view)

        flashSwitch = view.findViewById(R.id.flash_switcher) as ImageView
        flashSwitch.setOnClickListener{
            flashSwitchClicked()
        }

        if (!activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(AUTO_FLASH, true)) {
            flashSwitch.setImageResource(R.drawable.ic_flash_off_gray_24dp)
        }

        val recognizeBtn = view.findViewById(R.id.recognize_submit) as Button
        recognizeBtn.setOnClickListener{
            recognizeButtonClicked()
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (cameraView.isAvailable) {
            loadCamera()
        } else {
            cameraView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun recognizeButtonClicked() {
        lockFocus()
    }

    private fun flashSwitchClicked() {
        val appPrefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val isFlashAuto = appPrefs.getBoolean(AUTO_FLASH, true)
        if (isFlashAuto) {
            flashSwitch.setImageResource(R.drawable.ic_flash_off_gray_24dp)

        } else {
            flashSwitch.setImageResource(R.drawable.ic_flash_auto_gray_24dp)
        }
        with (appPrefs.edit()) {
            putBoolean(AUTO_FLASH, !isFlashAuto)
            apply()
        }
        setAutoFlash(previewRequestBuilder)
        captureSession?.setRepeatingRequest(previewRequestBuilder.build(), captureCallback,
            backgroundHandler)
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) = Unit
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            loadCamera()
        }
    }

    private fun loadCamera() {
        setCameraSettings()

        if (ContextCompat.checkSelfPermission(attachedActivityContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activity.requestStartupPermission()
            return
        }

        val mCameraManager = attachedActivityContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mCameraManager.openCamera(cameraId, cameraCallbacks, backgroundHandler)
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        mCameraDevice?.close()
        mCameraDevice = null
    }

    private fun setCameraSettings(){
        val mCameraManager = attachedActivityContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            //iterate trough all cameras available on this device
            for (camera in mCameraManager.cameraIdList) {
                val mCharacteristics = mCameraManager.getCameraCharacteristics(camera)

                val cameraFacing = mCharacteristics.get(CameraCharacteristics.LENS_FACING)
                //skip all cameras that faces the same direction as the device's screen
                if (cameraFacing != null && cameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }

                isFlashSupported = mCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                cameraId = camera

                return
            }
        } catch (exc: CameraAccessException) {
            Log.e(applicationTag, exc.toString())
            activity.showErrorByRequest(getString(R.string.camera_access_error))
        }
    }

    private fun createCameraPreviewSession() {
        try {

            val cameraWindow = cameraView.surfaceTexture
            cameraWindow.setDefaultBufferSize(1920, 1080)
            val surface = Surface(cameraWindow)

            previewRequestBuilder = mCameraDevice!!.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )
            previewRequestBuilder.addTarget(surface)

            mCameraDevice?.createCaptureSession(
                Arrays.asList(surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        if (mCameraDevice == null) return

                        captureSession = cameraCaptureSession
                        try {
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            setAutoFlash(previewRequestBuilder)
                            previewRequest = previewRequestBuilder.build()
                            captureSession?.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
                        } catch (exc: CameraAccessException) {
                            Log.e(applicationTag, exc.toString())
                            activity.showErrorByRequest(getString(R.string.camera_access_error))
                        }

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(applicationTag, getString(R.string.camera_configure_error))
                        activity.showErrorByRequest(getString(R.string.camera_configure_error))
                    }
                }, null)

        } catch (exc: CameraAccessException) {
            Log.e(applicationTag, exc.toString())
            activity.showErrorByRequest(getString(R.string.camera_access_error))
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraHugeTasks")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (exc: InterruptedException) {
            Log.e(applicationTag, exc.toString())
        }
    }

    private fun lockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START)
            captureState = STATE_WAITING_LOCK
            captureSession?.capture(previewRequestBuilder.build(), captureCallback,
                backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(applicationTag, e.toString())
        }

    }

    private fun runPreCaptureSequence() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
            captureState = STATE_WAITING_PRE_CAPTURE
            captureSession?.capture(previewRequestBuilder.build(), captureCallback,
                backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(applicationTag, e.toString())
        }

    }

    private fun captureStillPicture() {
        try {
            if (isFlashSupported && isFlashRequired) {
                turnOnFlash()
                isFlashRequired = false
            } else {
                finishImageCapturing()
            }
        } catch (e: CameraAccessException) {
            Log.e(applicationTag, e.toString())
        }
    }

    private fun cropBitmapToCard() {
        val srcBitmap = cameraView.bitmap
        val dstBmp = Bitmap.createBitmap(
            srcBitmap,
            0,
            topBorder.height,
            mLayout.width,
            mLayout.height - topBorder.height - lowerBorder.height
        )
        val outStream = activity.openFileOutput(BITMAP_TMP, Context.MODE_PRIVATE)
        dstBmp.compress(Bitmap.CompressFormat.PNG, 0, outStream)
        outStream.flush()
        outStream.close()
    }


    private fun finishImageCapturing() {
        cropBitmapToCard()
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            setAutoFlash(previewRequestBuilder)
            captureSession?.capture(previewRequestBuilder.build(), captureCallback,
                backgroundHandler)
            captureState = STATE_PREVIEW
            captureSession?.setRepeatingRequest(previewRequest, captureCallback,
                backgroundHandler)
            activity.checkNetworkSettings()
        } catch (e: CameraAccessException) {
            Log.e(applicationTag, e.toString())
        }

    }

    private fun turnOnFlash(){
        val finalCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                finishImageCapturing()
            }
        }
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON)
        previewRequestBuilder.set(CaptureRequest.FLASH_MODE,
            CameraMetadata.FLASH_MODE_TORCH)
        captureSession?.setRepeatingRequest(previewRequestBuilder.build(), captureCallback,
            backgroundHandler)
        sleep(500) //flash requires some time to adjust its brightness level
        captureSession?.stopRepeating()
        captureSession?.capture(previewRequestBuilder.build(), finalCaptureCallback,
            backgroundHandler)
    }


    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
        if (isFlashSupported) {
            if (activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(AUTO_FLASH, true)) {
                requestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                )
            } else {
                requestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
            }
        }
    }

    companion object {

        private const val STATE_PREVIEW = 0
        private const val STATE_WAITING_LOCK = 1
        private const val STATE_WAITING_PRE_CAPTURE = 2
        private const val STATE_WAITING_NON_PRE_CAPTURE = 3
        private const val STATE_PICTURE_TAKEN = 4
    }
}
