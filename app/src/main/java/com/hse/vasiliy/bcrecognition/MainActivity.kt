package com.hse.vasiliy.bcrecognition

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.navigation.NavigationView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.Switch
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var container: FrameLayout
    private lateinit var fragmentManager: FragmentManager
    private lateinit var cameraFragment: CameraFragment
    private lateinit var recognitionFragment: RecognitionFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        initAuthToken()
        setTheme(R.style.NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.fragment_container)
        fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        cameraFragment = CameraFragment()
        recognitionFragment = RecognitionFragment()
        fragmentTransaction.add(R.id.fragment_container, cameraFragment, CAMERA_FRAGMENT_TAG)
        fragmentTransaction.commit()

        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        moduleModePreparation()

        //Need to request permission on first launch
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStartupPermission()
        }

        //Check that storage is available
        checkExternalStorageWritable()
        copyTesseractFilesOnStorage()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_camera -> {
                openCamera()
            }
            R.id.offline_switch -> {
                return true
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_share -> {

            }
        }
        return true
    }

    private fun checkExternalStorageWritable() {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            ErrorDialog.newInstance(getString(R.string.external_storage_error))
                .show(supportFragmentManager, ERROR_DIALOG)
        }
    }

    private fun copyTesseractFilesOnStorage() {
        //create tesseract directory if it still not exist
        val dirName = "${applicationContext.getExternalFilesDir(null)}/$TESSERACT_SAMPLES_PATH"
        val dir = File(dirName)
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                ErrorDialog.newInstance(getString(R.string.external_storage_error))
                    .show(supportFragmentManager, ERROR_DIALOG)
            }
        }

        //copy files itself
        try {
            val fileList = assets.list(TESSERACT_ASSETS)

            for (fileName in fileList!!) {
                val dst = "$dirName/$fileName"
                if (!File(dst).exists()) {
                    val input = assets.open("$TESSERACT_ASSETS/$fileName")
                    val output = FileOutputStream(dst)
                    val buf = ByteArray(1024)
                    var len = input.read(buf)
                    while (len > 0) {
                        output.write(buf, 0, len)
                        len = input.read(buf)
                    }
                    input.close()
                    output.close()
                }
            }
        } catch (e: IOException) {
            ErrorDialog.newInstance(getString(R.string.external_storage_error))
                .show(supportFragmentManager, ERROR_DIALOG)
        }
    }

    private fun initAuthToken() {
        val task = GoogleAccessTokenLoader(applicationContext)
        task.execute()
    }

    private fun openCamera(){
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(
            R.id.fragment_container,
            cameraFragment,
            CAMERA_FRAGMENT_TAG
        )
        fragmentTransaction.commit()
    }

    private fun moduleModePreparation(){
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val offlineSwitch = findViewById<NavigationView>(R.id.nav_view).menu.findItem(R.id.offline_switch_item).
            actionView.findViewById<Switch>(R.id.offline_switch)
        if (prefs.getBoolean(OFFLINE_MODE, false)){
            offlineSwitch.isChecked = true
        }
        offlineSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                prefs.edit().putBoolean(OFFLINE_MODE, true).apply()
            }
            else{
                prefs.edit().putBoolean(OFFLINE_MODE, false).apply()
            }
        }
    }

    fun openRecognition(){
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(
            R.id.fragment_container,
            recognitionFragment,
            RECOGNITION_FRAGMENT_TAG
        )
        fragmentTransaction.addToBackStack(null).commit()
    }

    fun addContact(intent: Intent){
        startActivity(intent)
    }

    fun requestStartupPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionRequestConfirmationDialog().apply { isCancelable = false }.show(supportFragmentManager, CONFIRMATION_DIALOG)
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_STARTUP_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            REQUEST_STARTUP_PERMISSIONS -> {
                var permissionGranted = true
                for (i in grantResults.indices){
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                    {
                        permissionGranted = false
                        break
                    }
                }
                if (permissionGranted) {
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                } else {
                    ErrorDialog.newInstance(getString(R.string.camera_access_error))
                        .show(supportFragmentManager, ERROR_DIALOG)
                }
            }
        }
    }

    fun showErrorByRequest(errorText: String) {
        ErrorDialog.newInstance(errorText)
            .show(supportFragmentManager, ERROR_DIALOG)
    }

}
