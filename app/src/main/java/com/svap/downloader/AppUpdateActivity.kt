package com.svap.downloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.svap.downloader.databinding.ActivityAppUpdateBinding
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AppUpdateActivity : AppCompatActivity(), OnFileDownloadingCallback {
    private lateinit var mBinding: ActivityAppUpdateBinding
    private val mAppName: String by lazy { getString(R.string.app_name).replace(" ", "_") }
    private val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityAppUpdateBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        if (intent.getBooleanExtra(IS_FORCE_UPDATE, false)) {
            mBinding.txtSkip.visibility = View.GONE
        } else {
            mBinding.txtSkip.visibility = View.VISIBLE
        }
        mBinding.txtSkip.setOnClickListener {
            userLogin()
        }
        mBinding.btnUpdateApp.setOnClickListener {
            if (hasPermissions()) {
                createOutputFile()
                downloadFile(this@AppUpdateActivity)
            } else {
                requestPermission()
            }
        }
    }

    private fun requestPermission() {
        try {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hasPermissions(): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun userLogin() {

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createOutputFile()
                downloadFile(this@AppUpdateActivity)
            }
        }
    }

    override fun onDownloadingStart() {
        runOnUiThread {
            Log.d("downloadFile", " onDownloadingStart ")
            mBinding.tvInfo.text = "Downloading"
            mBinding.tvInfo.visibility = View.VISIBLE
            mBinding.pbHorizontal.visibility = View.VISIBLE
            mBinding.pbHorizontal.progress = 0
            mBinding.btnUpdateApp.visibility = View.GONE
        }
    }

    override fun onDownloadingProgress(progress: Int) {
        runOnUiThread {
            Log.d("downloadFile", " onDownloadingProgress " + progress)
            mBinding.pbHorizontal.progress = progress
            mBinding.tvInfo.text = "$progress%"
        }
    }

    override fun onDownloadingComplete() {
        runOnUiThread {
            Log.d("downloadFile", " onDownloadingComplete ")
            mBinding.btnUpdateApp.visibility = View.VISIBLE
            mBinding.pbHorizontal.visibility = View.VISIBLE
            mBinding.tvInfo.text = "Completed"
            installApk()
        }
    }

    override fun onDownloadingFailed(e: Exception?) {
        runOnUiThread {
            Log.d("downloadFile", " onDownloadingFailed " + e?.message)
            mBinding.btnUpdateApp.visibility = View.VISIBLE
            mBinding.pbHorizontal.visibility = View.GONE
            mBinding.tvInfo.visibility = View.GONE
            mBinding.tvInfo.text = "Downloading Failed"
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            installApk()
        }
    }

    private fun installApk() {
        Log.d("downloadFile", " canRequestPackageInstalls ")
        Log.d("downloadFile", " installApk ")
        val intent = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val apkUri = FileProvider.getUriForFile(this, "$packageName.provider", apkFile!!)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            intent.data = apkUri
        } else {
            intent.setDataAndType(
                Uri.fromFile(apkFile),
                "application/vnd.android.package-archive"
            )
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Log.d("downloadFile", " installed ")
    }

    companion object {
        var PERMISSION_REQUEST_CODE = 11
        const val REQUEST_CODE = 1234
        const val IS_FORCE_UPDATE = "IS-FORCE-UPDATE"
        const val APP_URL = "APP-URL"
    }

    var apkFile: File? = null
    var isDownloadSuccess = false

    private fun createOutputFile() {
        apkFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "$mAppName.apk")
        if (apkFile?.exists() == true) {
            apkFile?.delete()
        }
    }

    private fun downloadFile(
        callback: OnFileDownloadingCallback
    ) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            handler.post { callback.onDownloadingStart() }
            try {
                val url = URL(intent?.getStringExtra(APP_URL).toString())
                val conection = url.openConnection()
                conection.connect()
                val lenghtOfFile = conection.contentLength
                val input: InputStream = BufferedInputStream(url.openStream(), 8192)
                val output = FileOutputStream(apkFile)
                val data = ByteArray(1024)
                var total: Long = 0
                while (true) {
                    val read = input.read(data)
                    if (read == -1) {
                        break
                    }
                    output.write(data, 0, read)
                    val j2 = total + read.toLong()
                    if (lenghtOfFile > 0) {
                        val progress = ((100 * j2 / lenghtOfFile.toLong()).toInt())
                        callback.onDownloadingProgress(progress)
                    }
                    total = j2
                }
                output.flush()
                output.close()
                input.close()
                isDownloadSuccess = true
                handler.post { callback.onDownloadingComplete() }
            } catch (e: Exception) {
                isDownloadSuccess = false
                handler.post { callback.onDownloadingFailed(e) }
            }
        }
    }

    override fun onResume() {
        super.onResume()

    }
}

internal interface OnFileDownloadingCallback {
    fun onDownloadingStart()
    fun onDownloadingProgress(progress: Int)
    fun onDownloadingComplete()
    fun onDownloadingFailed(e: Exception?)
}