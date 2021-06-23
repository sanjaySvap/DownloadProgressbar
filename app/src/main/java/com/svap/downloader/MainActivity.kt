package com.svap.downloader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        isUpdate(false,"https://www.play4xi.com/play4xi.apk")
    }

    private fun isUpdate(isForceUpdate: Boolean, url: String) {
        val intent = Intent(this, AppUpdateActivity::class.java)
        intent.putExtra(AppUpdateActivity.APP_URL, url)
        intent.putExtra(AppUpdateActivity.IS_FORCE_UPDATE, isForceUpdate)
        startActivity(intent)
        finish()
    }
}