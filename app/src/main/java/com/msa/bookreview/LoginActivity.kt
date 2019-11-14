package com.msa.bookreview

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*
import kotlin.math.min

class LoginActivity : AppCompatActivity() {

    lateinit var providers : List<AuthUI.IdpConfig>
    val MY_REQUEST_CODE: Int = 7117

    private lateinit var svScanner: SurfaceView
    private lateinit var tvText: TextView

    private lateinit var cameraSource: CameraSource
    private lateinit var textRecognizer: TextRecognizer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        svScanner = findViewById(R.id.sv_scanner)
        tvText = findViewById(R.id.tv_text)

        textRecognizer = TextRecognizer.Builder(this).build()
        if (textRecognizer.isOperational){
            //start scanning
            startScanner()
        } else {
            Log.w("Hi", "Your text recognizer is not operational")
        }

        //Init
        providers = Arrays.asList<AuthUI.IdpConfig>(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build()
            )
        showSignInOptions()


        //Event
        btn_sign_out.setOnClickListener {
            //sign out
            AuthUI.getInstance().signOut(this@LoginActivity)
                .addOnCompleteListener{
                    btn_sign_out.isEnabled = false
                    showSignInOptions()
                }
                .addOnFailureListener{
                    e-> Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
                }        }

//        btn_scan.setOnClickListener {
//            val intent = Intent(this, BookActivity::class.java)
//            // start your next activity
//            startActivity(intent)
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == MY_REQUEST_CODE){
            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK){
                val user = FirebaseAuth.getInstance().currentUser
                Toast.makeText(this, ""+user!!.email,Toast.LENGTH_SHORT).show()

                btn_sign_out.isEnabled = true
            }else{
                Toast.makeText(this, ""+response!!.error!!.message,Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSignInOptions() {
        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.MyTheme)
            .build(),MY_REQUEST_CODE)

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
                if(ActivityCompat.checkSelfPermission(this@LoginActivity,
                        android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraSource.start(holder)
                } else {
                    ActivityCompat.requestPermissions(this@LoginActivity, arrayOf(android.Manifest.permission.CAMERA), 123)
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
