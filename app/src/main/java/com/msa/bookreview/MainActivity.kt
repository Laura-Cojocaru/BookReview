package com.msa.bookreview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


import android.content.pm.PackageManager
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer

import kotlin.math.min


class MainActivity : AppCompatActivity() {

    private lateinit var svScanner: SurfaceView
    private lateinit var tvText: TextView

    private lateinit var cameraSource: CameraSource
    private lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        setSupportActionBar(toolbar)

        svScanner = findViewById(R.id.sv_scanner)
        tvText = findViewById(R.id.tv_text)

        textRecognizer = TextRecognizer.Builder(this).build()
        if (textRecognizer.isOperational){
            //start scanning
            startScanner()
        } else {
            Log.w("Hi", "Your text recognizer is not operational")
        }
    }

    private fun startScanner() {
        cameraSource = CameraSource.Builder(this, textRecognizer)
            .setFacing(CameraSource.CAMERA_FACING_BACK).setRequestedFps(30f)
            .setRequestedPreviewSize(1024, 768).setAutoFocusEnabled(true).build()
        svScanner.holder.addCallback(object : SurfaceHolder.Callback2{
            override fun surfaceRedrawNeeded(holder: SurfaceHolder?) {}
            override fun surfaceChanged(holder: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                cameraSource.stop()
            }

            override fun surfaceCreated(holder: SurfaceHolder?) {
                if(ActivityCompat.checkSelfPermission(this@MainActivity,
                        android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraSource.start(holder)
                } else {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.CAMERA), 123)
                }
            }
        })
        textRecognizer.setProcessor(object : Detector.Processor<TextBlock> {
            override fun release() {}
            override fun receiveDetections(detections: Detector.Detections<TextBlock>?) {
                val textItems = detections?.detectedItems
                val builder = StringBuilder()
                for (i in 0..min(textItems!!.size() - 1, 5)){
                    var item = textItems.valueAt(i)
                    builder.append(item.getValue())
                    builder.append("\n")
                }
                tvText.text = builder.toString()
            }

        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode ==123 && grantResults.isNotEmpty()){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                cameraSource.start(svScanner.holder)
            } else {
                Toast.makeText(this, "scanner won't work without permission", Toast.LENGTH_SHORT )
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textRecognizer.release()
        cameraSource.stop()
        cameraSource.release()
    }

}
