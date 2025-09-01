package com.ericp.e_hub

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.adapters.PhotoAdapter
import com.ericp.e_hub.config.NextCloudConfig
import com.ericp.e_hub.network.ApiManager
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class NextCloudGymActivity : AppCompatActivity() {
    private lateinit var backButton: Button
    private lateinit var settingsButton: Button
    private lateinit var selectPhotosButton: Button
    private lateinit var connectionStatusText: TextView
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var uploadStatusText: TextView
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var uploadButton: Button

    private lateinit var nextCloudConfig: NextCloudConfig
    private lateinit var apiManager: ApiManager

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var selectedPhotos = mutableListOf<Uri>()

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val data = result.data
            val clipData = data?.clipData
            val selectedUris = mutableListOf<Uri>()

            if (clipData != null) {
                // Multiple photos selected
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    selectedUris.add(uri)
                }
            } else {
                // Single photo selected
                data?.data?.let { uri ->
                    selectedUris.add(uri)
                }
            }

            if (selectedUris.isNotEmpty()) {
                selectedPhotos.addAll(selectedUris)
                photoAdapter.addPhotos(selectedUris)
                uploadStatusText.visibility = View.VISIBLE
                uploadStatusText.text = getString(R.string.ready_to_upload, selectedPhotos.size)
                uploadButton.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nextcloud_gym)

        initializeComponents()
        setupListeners()
        checkConnection()
    }

    private fun initializeComponents() {
        backButton = findViewById(R.id.backButton)
        settingsButton = findViewById(R.id.settingsButton)
        selectPhotosButton = findViewById(R.id.selectPhotosButton)
        connectionStatusText = findViewById(R.id.connectionStatusText)
        uploadProgressBar = findViewById(R.id.uploadProgressBar)
        uploadStatusText = findViewById(R.id.uploadStatusText)
        photoRecyclerView = findViewById(R.id.photoRecyclerView)
        uploadButton = findViewById(R.id.uploadButton)

        nextCloudConfig = NextCloudConfig(this)
        apiManager = ApiManager.getInstance()

        // Setup RecyclerView
        photoAdapter = PhotoAdapter()
        photoRecyclerView.layoutManager = GridLayoutManager(this, 2)
        photoRecyclerView.adapter = photoAdapter

        // Initially hide the upload button until photos are selected
        uploadButton.visibility = View.GONE
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, NextCloudGymSettingsActivity::class.java)
            startActivity(intent)
        }

        selectPhotosButton.setOnClickListener {
            openGallery()
        }

        uploadButton.setOnClickListener {
            uploadPhotos()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private fun checkConnection() {
        connectionStatusText.text = getString(R.string.checking_connection)
        selectPhotosButton.isEnabled = false

        if (!nextCloudConfig.isConfigured()) {
            connectionStatusText.text = getString(R.string.nextcloud_not_configured)
            return
        }

        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    apiManager.testNextCloudConnection(this@NextCloudGymActivity, nextCloudConfig)
                }

                when (result) {
                    is ApiManager.ApiResult.Success -> {
                        connectionStatusText.text = getString(R.string.connected_to_nextcloud)
                        selectPhotosButton.isEnabled = true
                    }
                    is ApiManager.ApiResult.Error -> {
                        connectionStatusText.text = getString(R.string.connection_error, result.message)
                    }
                }
            } catch (e: Exception) {
                connectionStatusText.text = getString(R.string.connection_error, e.message)
            }
        }
    }

    fun uploadPhotos() {
        if (selectedPhotos.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_photos_selected), Toast.LENGTH_SHORT).show()
            return
        }

        uploadProgressBar.visibility = View.VISIBLE
        uploadProgressBar.max = selectedPhotos.size
        uploadProgressBar.progress = 0
        uploadStatusText.visibility = View.VISIBLE
        uploadStatusText.text = getString(R.string.upload_starting)
        uploadButton.isEnabled = false

        coroutineScope.launch {
            var successCount = 0
            var failCount = 0

            for ((index, uri) in selectedPhotos.withIndex()) {
                try {
                    val fileName = getFileNameFromUri(uri)
                    val file = createTempFileFromUri(uri)

                    if (file != null) {
                        val result = uploadFileToNextCloud(file, fileName)
                        if (result) successCount++ else failCount++
                    } else {
                        failCount++
                    }

                    file?.delete() // Clean up temp file

                    // Update progress
                    withContext(Dispatchers.Main) {
                        uploadProgressBar.progress = index + 1
                        uploadStatusText.text = getString(
                            R.string.upload_progress,
                            index + 1,
                            selectedPhotos.size,
                            successCount,
                            failCount
                        )
                    }
                } catch (e: Exception) {
                    failCount++
                    withContext(Dispatchers.Main) {
                        uploadProgressBar.progress = index + 1
                        uploadStatusText.text = getString(
                            R.string.upload_progress,
                            index + 1,
                            selectedPhotos.size,
                            successCount,
                            failCount
                        )
                    }
                }
            }

            withContext(Dispatchers.Main) {
                uploadStatusText.text = getString(
                    R.string.upload_complete,
                    successCount,
                    failCount
                )

                // Clear selected photos after successful upload
                if (successCount == selectedPhotos.size) {
                    selectedPhotos.clear()
                    photoAdapter.setPhotos(emptyList())
                    uploadButton.visibility = View.GONE
                }

                uploadButton.isEnabled = true
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        // Fallback to a timestamp-based name if we can't get the original name
        return "photo_${System.currentTimeMillis()}.jpg"
    }

    private suspend fun createTempFileFromUri(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
            val tempFile = File(cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")

            FileOutputStream(tempFile).use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }

            return@withContext tempFile
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private suspend fun uploadFileToNextCloud(file: File, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseUrl = nextCloudConfig.getServerUrl().trimEnd('/')
            val webdavPath = nextCloudConfig.getWebdavEndpoint()
            val username = nextCloudConfig.getUsername()
            val password = nextCloudConfig.getPassword()

            // Create the full URL for the file
            val targetUrl = "$baseUrl$webdavPath/$fileName"

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(targetUrl)
                .header("Authorization", Credentials.basic(username, password))
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh connection status when returning from settings
        checkConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
