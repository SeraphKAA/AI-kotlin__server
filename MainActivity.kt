package com.example.testpre1

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType



class MainActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private lateinit var button_start: Button
    private lateinit var editText: EditText

    private val GALLERY = 1
    private val CAMERA = 2
    private lateinit var videoUri: Uri
    private val url_to_server: String = "http://37.27.83.101:8000/upload/"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        requestMultiplePermissions()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val button_shoot: Button = findViewById(R.id.button01)
        val button_load: Button = findViewById(R.id.button02)
        button_start = findViewById(R.id.button03)

        videoView = findViewById<VideoView>(R.id.videoView)
        editText = findViewById<EditText>(R.id.edit_Text_url)

        button_load.setOnClickListener(View.OnClickListener { chooseVideoFromGallary() })
        button_shoot.setOnClickListener(View.OnClickListener { takeVideoFromCamera() })
        button_start.setOnClickListener(View.OnClickListener { playVideo() })
    }

    private fun chooseVideoFromGallary() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(galleryIntent, GALLERY)
    }

    private fun takeVideoFromCamera() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(intent, CAMERA)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("result", "" + resultCode)
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED) {
            Log.d("what", "cancle")
            return
        }
        if (requestCode == GALLERY) {
            Log.d("what", "gale")
            if (data != null) {
                val contentURI = data.data
                val selectedVideoPath = getPath(contentURI)
                Log.d("path", selectedVideoPath.toString())
                use_yolo_model_to_videoView(videoView, contentURI, data!!)
            }
        } else if (requestCode == CAMERA) {
            Log.d("what", "camera")
            val contentURI = data!!.data
            val recordedVideoPath = getPath(contentURI)
            Log.d("frrr", recordedVideoPath.toString())
            use_yolo_model_to_videoView(videoView, contentURI, data!!)
        }
    }

    fun getPath(uri: Uri?): String? {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = contentResolver.query(uri!!, projection, null, null, null)
        if (cursor != null) {
            val column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index)
        } else
            return null
    }

    private fun requestMultiplePermissions() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    // check if all permissions are granted
                    if (report.areAllPermissionsGranted()) {
                        Toast.makeText(
                            applicationContext,
                            "All permissions are granted by user!",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    // check for permanent denial of any permission
                    if (report.isAnyPermissionPermanentlyDenied) {
                        // show alert dialog navigating to Settings
                        //openSettingsDialog()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).withErrorListener {
                Toast.makeText(
                    applicationContext,
                    "Some Error! ",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .onSameThread()
            .check()
    }


    private fun use_yolo_model_to_videoView(videoview: VideoView, contentURI: Uri?, data: Intent) {
        editText.visibility = View.VISIBLE
        button_start.visibility = View.VISIBLE
        println(getPath(contentURI))
        val file = File(getPath(contentURI)!!)
        val client = OkHttpClient()
        val MEDIA_TYPE_MARKDOWN = "application/octet-stream; charset=utf-8".toMediaType()


        val request = Request.Builder()
            .url(url_to_server)
            .post(file.asRequestBody(MEDIA_TYPE_MARKDOWN))
            .addHeader("Content-Type", "video/mp4")
            .addHeader("User-Agent", "insomnia/9.2.0")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                println(response)
                if (response.isSuccessful) {
                    // Обработать успешный ответ
                    Log.d("VideoUpload", "Видео успешно отправлено!")
                } else {
                    // Обработать ошибочный ответ
                    Log.e("VideoUpload", "Ошибка отправки видео: ${response.code}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                // Обработать ошибку
                Log.e("VideoUpload", "Ошибка отправки видео: ${e.message}")
            }
        })
    }

    private fun playVideo() {
        val uri = editText.text.toString().toUri()
        editText.visibility = View.INVISIBLE
        button_start.visibility = View.INVISIBLE
        videoView.visibility = View.VISIBLE

//        Toast.makeText(applicationContext, "$uri", Toast.LENGTH_SHORT).show()
        videoView.setVideoURI(uri)
        videoView.requestFocus()
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        mediaController.setMediaPlayer(videoView)
        videoView.setMediaController(mediaController)
        videoView.start()
    }
}



// Расширение для OkHttpClient для перегрузки оператора get
operator fun OkHttpClient.get(url: String): Response {
    val request = Request.Builder()
        .url(url)
        .build()
    return this.newCall(request).execute()
}