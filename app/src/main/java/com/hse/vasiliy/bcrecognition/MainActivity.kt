package com.hse.vasiliy.bcrecognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.content.ContextCompat

import android.view.TextureView
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.Window
import java.util.*
import android.hardware.camera2.CaptureRequest




class MainActivity : FragmentActivity() {

    private lateinit var cameraView: TextureView
    private lateinit var mLayout: View
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
            this@MainActivity.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        mLayout = findViewById(R.id.main_layout)

        cameraView = findViewById(R.id.camera_view)
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
        //Need to request permission on first launch
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
            return
        }

        setCameraSettings(width, height)

        val mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mCameraManager.openCamera(cameraId, cameraCallbacks, backgroundHandler)
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        mCameraDevice?.close()
        mCameraDevice = null
    }

    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            CameraAccessConfirmationDialog().apply { isCancelable = false }.show(supportFragmentManager, CONFIRMATION_DIALOG)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                } else {
                    ErrorDialog.newInstance(getString(R.string.camera_access_error))
                        .show(supportFragmentManager, ERROR_DIALOG)
                }
            }
        }
    }

    private fun setCameraSettings(width: Int,
                                  height: Int){
        val mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
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
            ErrorDialog.newInstance(getString(R.string.camera_access_error))
                .show(supportFragmentManager, ERROR_DIALOG)
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
                        } catch (e: CameraAccessException) {
                            ErrorDialog.newInstance(getString(R.string.camera_access_error))
                                .show(supportFragmentManager, ERROR_DIALOG)
                        }

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        //TODO:add error handler
                    }
                }, null)

        } catch (exc: CameraAccessException) {
            ErrorDialog.newInstance(getString(R.string.camera_access_error))
                .show(supportFragmentManager, ERROR_DIALOG)
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
            Log.e(TAG, exc.toString())
        }

    }
}
