package com.hse.vasiliy.bcrecognition

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, ConfirmationDialog.ConfirmationDialogListener {

    private lateinit var container: FrameLayout
    private lateinit var fragmentManager: FragmentManager
    private lateinit var cameraFragment: CameraFragment
    private lateinit var recognitionFragment: RecognitionFragment
    private lateinit var settingsFragment: SettingsFragment

    private val activityTag = "MAIN_ACTIVITY"

    private var permissionsRequested = false

    override fun onDialogPositiveClick(dialogId: Int) {
        when (dialogId) {
            ConfirmationDialogsTypes.PERMISSION_RATIONALE.ordinal -> {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_STARTUP_PERMISSIONS)
            }
            ConfirmationDialogsTypes.NETWORK_SWITCH_ONLINE_NOTIFICATION.ordinal -> {
                findViewById<NavigationView>(R.id.nav_view).menu.findItem(R.id.nav_offline_switch).
                    actionView.findViewById<Switch>(R.id.offline_switch).isChecked = false
                openRecognition()
            }
            ConfirmationDialogsTypes.NETWORK_SWITCH_OFFLINE_NOTIFICATION.ordinal -> {
                findViewById<NavigationView>(R.id.nav_view).menu.findItem(R.id.nav_offline_switch).
                    actionView.findViewById<Switch>(R.id.offline_switch).isChecked = true
                openRecognition()
            }
        }
    }

    override fun onDialogNegativeClick(dialogId: Int) {
        when (dialogId) {
            ConfirmationDialogsTypes.PERMISSION_RATIONALE.ordinal -> {
                Log.w(activityTag, "User declines apps permissions")
                this.finish()
            }
            ConfirmationDialogsTypes.NETWORK_SWITCH_ONLINE_NOTIFICATION.ordinal -> {
                openRecognition()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initAuthToken()
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.fragment_container)
        fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        cameraFragment = CameraFragment()
        recognitionFragment = RecognitionFragment()
        settingsFragment = SettingsFragment()
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
            R.id.nav_settings -> {
                openSettings()
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_offline_switch -> {
                return true
            }
            R.id.nav_share -> {

            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun checkExternalStorageWritable() {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            ErrorDialog
                .newInstance(getString(R.string.external_storage_error))
                .show(supportFragmentManager, ERROR_DIALOG)
        }
    }

    private fun copyTesseractFilesOnStorage() {
        //create tesseract directory if it still not exist
        val dirName = "${applicationContext.getExternalFilesDir(null)}/$TESSERACT_SAMPLES_PATH"
        val dir = File(dirName)
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                ErrorDialog
                    .newInstance(getString(R.string.external_storage_error))
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

    private fun openSettings(){
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(
            R.id.fragment_container,
            settingsFragment,
            SETTINGS_FRAGMENT_TAG
        )
        fragmentTransaction.addToBackStack(null).commit()
    }

    private fun openRecognition(){
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(
            R.id.fragment_container,
            recognitionFragment,
            RECOGNITION_FRAGMENT_TAG
        )
        fragmentTransaction.addToBackStack(null).commit()
    }

    private fun moduleModePreparation(){
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val offlineSwitch = findViewById<NavigationView>(R.id.nav_view).menu.findItem(R.id.nav_offline_switch).
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

    private fun isNetworkAvailable(wifiOnly : Boolean = false): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: Network? = cm.activeNetwork
        activeNetwork ?: return false
        if (wifiOnly && !cm.getNetworkCapabilities(activeNetwork).hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
            return false
        }
        return true
    }

    fun checkNetworkSettings(){
        val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val applicationPrefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val isOnline = isNetworkAvailable(settingsPrefs.getBoolean("wifi_only_mode", false))
        if (isOnline &&
            applicationPrefs.getBoolean(OFFLINE_MODE, false) &&
            settingsPrefs.getBoolean("internet_enable_notification", false)
        ) {
            ConfirmationDialog.newInstance(
                ConfirmationDialogsTypes.NETWORK_SWITCH_ONLINE_NOTIFICATION.ordinal,
                getString(R.string.switch_online_notification_dialog_header),
                getString(R.string.switch_online_notification_dialog_body),
                getString(R.string.ok),
                getString(R.string.cancel)
            ).show(supportFragmentManager, CONFIRMATION_DIALOG)
            return
        }
        if (!isOnline && !applicationPrefs.getBoolean(OFFLINE_MODE, false)){
            if (settingsPrefs.getBoolean("internet_disable_notification", false)) {
                    ConfirmationDialog.newInstance(
                        ConfirmationDialogsTypes.NETWORK_SWITCH_OFFLINE_NOTIFICATION.ordinal,
                        getString(R.string.switch_offline_notification_dialog_header),
                        getString(R.string.switch_offline_notification_dialog_body),
                        getString(R.string.ok),
                        getString(R.string.cancel)
                    ).show(supportFragmentManager, CONFIRMATION_DIALOG)
                    return
                } else {
                findViewById<NavigationView>(R.id.nav_view).menu.findItem(R.id.nav_offline_switch).
                    actionView.findViewById<Switch>(R.id.offline_switch).isChecked = true
            }
        }
        openRecognition()
    }

    fun addContact(intent: Intent){
        startActivity(intent)
    }

    fun requestStartupPermission() {
        if (permissionsRequested) {
            return
        } else {
            permissionsRequested = true
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ConfirmationDialog.newInstance(
                ConfirmationDialogsTypes.PERMISSION_RATIONALE.ordinal,
                getString(R.string.camera_access_required_title),
                getString(R.string.camera_access_required),
                getString(R.string.ok),
                getString(R.string.cancel)
            ).apply { isCancelable = false }.show(supportFragmentManager, CONFIRMATION_DIALOG)
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
                if (!permissionGranted) {
                    ErrorDialog.newInstance(getString(R.string.permission_denied_error))
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
