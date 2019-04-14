package com.hse.vasiliy.bcrecognition

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
import android.view.*
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.Button


class CameraFragment : Fragment() {

    private lateinit var attachedActivityContext: Context
    private lateinit var activity: MainActivity

    private var applicationTag = "CameraFragment"

    private lateinit var cameraView: TextureView
    private lateinit var mLayout: View
    private lateinit var topBorder: View
    private lateinit var lowerBorder: View
    private lateinit var cameraId: String
    //next two required by camera2 docs to perform huge camera tasks in separate thread
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private var mCameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

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
            loadCamera(cameraView.width, cameraView.height)
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
        activity.checkNetworkSettings()
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) = Unit
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            loadCamera(width, height)
        }
    }

    private fun loadCamera(width: Int,
                           height: Int) {
        setCameraSettings(width, height)

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

    private fun setCameraSettings(width: Int,
                                  height: Int){ //TODO: use device resolution for preview instead of const definition
        val mCameraManager = attachedActivityContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            //iterate trough all cameras available on this device
            for (camera in mCameraManager.cameraIdList) {
                val mCharacteristics = mCameraManager.getCameraCharacteristics(camera)
                val map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

                val cameraFacing = mCharacteristics.get(CameraCharacteristics.LENS_FACING)
                //skip all cameras that faces the same direction as the device's screen
                if (cameraFacing != null && cameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }

                cameraId = camera
            }
        } catch (exc: CameraAccessException) {
            Log.e(applicationTag, exc.toString())
            activity.showErrorByRequest(getString(R.string.camera_access_error))
        }
    }

    private fun createCameraPreviewSession() {
        try {

            val cameraWindow = cameraView.surfaceTexture
            cameraWindow.setDefaultBufferSize(1920, 1080) //TODO:eliminate const
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
                            val previewRequest = previewRequestBuilder.build()
                            captureSession?.setRepeatingRequest(previewRequest, null, backgroundHandler)
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
}
