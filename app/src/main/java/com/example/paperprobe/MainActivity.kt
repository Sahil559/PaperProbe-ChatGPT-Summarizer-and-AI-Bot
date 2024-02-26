package com.example.paperprobe

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.paperprobe.databinding.LayoutBinding
import com.google.firebase.storage.FirebaseStorage
import java.util.*



class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()


    private var _binding:LayoutBinding? = null
    private val binding:LayoutBinding
        get() = _binding!!


    private val storage = FirebaseStorage.getInstance()

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            it?.let {
                if (it) {
                    this.makeToast("permission granted")
                }
            }
        }

    private val fileAccess =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.data?.let {
                val inputStream = this.contentResolver.openInputStream(it)
                inputStream?.readBytes()?.let {
                    uploadFile(it)
                }
            }
        }


    // creating variables on below line.
    lateinit var txtResponse: TextView
    lateinit var idTVQuestion: TextView
    lateinit var etQuestion: TextInputEditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout)

        _binding = LayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnUploadPdf.setOnClickListener {
            sdkIntOverO(this) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                fileAccess.launch(intent)
            }
        }

        binding.btnViewPdf.setOnClickListener {
            val intent = Intent(this, PdfSummaizerActivity::class.java)
            startActivity(intent)
        }



        etQuestion = findViewById<TextInputEditText>(R.id.etQuestion)
        idTVQuestion = findViewById<TextView>(R.id.idTVQuestion)
        txtResponse = findViewById<TextView>(R.id.txtResponse)
        etQuestion.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {

                // setting response tv on below line.
                txtResponse.text = "Please wait.."

                // validating text
                val question = etQuestion.text.toString().trim()
                Toast.makeText(this, question, Toast.LENGTH_SHORT).show()
                if (question.isNotEmpty()) {
                    getResponse(question) { response ->
                        runOnUiThread {
                            txtResponse.text = response
                        }
                    }
                }
                return@OnEditorActionListener true
            }
            false
        })


    }

        fun getResponse(question: String, callback: (String) -> Unit) {

            // setting text on for question on below line.
            idTVQuestion.text = question
            etQuestion.setText("")

            val apiKey = "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"
            val url = "https://api.openai.com/v1/completions"

            val requestBody = """
            {
            "model": "gpt-3.5-turbo-instruct",
            "prompt": "$question",
            "max_tokens": 500,
            "temperature": 0.7
            }
            """.trimIndent()


            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("error", "API failed", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (body != null) {
                        Log.v("data", body)
                    } else {
                        Log.v("data", "empty")
                    }
                    val jsonObject = JSONObject(body)
                    val jsonArray: JSONArray = jsonObject.getJSONArray("choices")
                    val textResult = jsonArray.getJSONObject(0).getString("text")
                    callback(textResult)
                }
            })
        }

    fun uploadFile(byteArray: ByteArray) {
        val storageRef = storage.reference
        val storageRef2 = storageRef.child("uploads/${Date().time}")
        storageRef2.putBytes(byteArray)
            .addOnSuccessListener {
                this.makeToast("PDF uploaded successfully")

                // taking the public url
                storageRef2.downloadUrl.addOnSuccessListener {
                }

            }
            .addOnFailureListener {
                this.makeToast("failure")
            }
            .addOnCompleteListener {
                this.makeToast("complete")
            }

    }

    fun sdkIntOverO(context: Context, call: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                call.invoke()
            } else {
                requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }


}

fun Context.makeToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}