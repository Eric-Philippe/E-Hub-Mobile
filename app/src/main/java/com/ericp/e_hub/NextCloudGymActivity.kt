package com.ericp.e_hub

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

class NextCloudGymActivity : Activity() {
    private lateinit var backButton: Button
    private lateinit var settingsButton: Button
    private lateinit var selectPhotosButton: Button
    private lateinit var connectionStatusText: TextView
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var uploadStatusText: TextView
    private lateinit var uploadStageText: TextView
    private lateinit var uploadProgressSection: LinearLayout
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var uploadButton: Button

    private lateinit var nextCloudConfig: NextCloudConfig
    private lateinit var apiManager: ApiManager

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var selectedPhotos = mutableListOf<Uri>()

    companion object {
        private const val REQUEST_SELECT_PHOTOS = 1001
        private const val REQUEST_DELETE_PHOTOS = 1002
        private const val REQUEST_PHOTO_PERMISSION = 2001
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
        uploadStageText = findViewById(R.id.uploadStageText)
        uploadProgressSection = findViewById(R.id.uploadProgressSection)
        photoRecyclerView = findViewById(R.id.photoRecyclerView)
        uploadButton = findViewById(R.id.uploadButton)

        nextCloudConfig = NextCloudConfig(this)
        apiManager = ApiManager.getInstance()

        // Setup RecyclerView with photo removal callback
        photoAdapter = PhotoAdapter { uri, position ->
            removePhoto(uri, position)
        }
        photoRecyclerView.layoutManager = GridLayoutManager(this, 2)
        photoRecyclerView.adapter = photoAdapter

        // Initially hide the upload button and progress section until photos are selected
        uploadButton.visibility = View.GONE
        uploadProgressSection.visibility = View.GONE
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
            checkPhotoPermissionAndOpenGallery()
        }

        uploadButton.setOnClickListener {
            uploadPhotos()
        }
    }

    private fun checkPhotoPermissionAndOpenGallery() {
        // Android 14+: request selected photos access
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                REQUEST_PHOTO_PERMISSION
            )
        } else {
            openGallery()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PHOTO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                showPhotoPermissionDeniedDialog()
            }
        }
    }

    private fun showPhotoPermissionDeniedDialog() {
        val message =
            "Please allow access to selected photos in your device settings. On Android 14+, you may need to select specific photos for this app."
        AlertDialog.Builder(this)
            .setTitle("Photo Access Required")
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        // For Android 14+, this will show the "Selected Photos" picker
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(intent, REQUEST_SELECT_PHOTOS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SELECT_PHOTOS && resultCode == RESULT_OK && data != null) {
            val clipData = data.clipData
            val newSelectedUris = mutableListOf<Uri>()

            if (clipData != null) {
                // Multiple photos selected
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    // Only add if not already selected
                    if (!selectedPhotos.contains(uri)) {
                        // Persist permission grants
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        newSelectedUris.add(uri)
                    }
                }
            } else {
                // Single photo selected
                data.data?.let { uri ->
                    // Only add if not already selected
                    if (!selectedPhotos.contains(uri)) {
                        // Persist permission grants
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        newSelectedUris.add(uri)
                    }
                }
            }

            if (newSelectedUris.isNotEmpty()) {
                selectedPhotos.addAll(newSelectedUris)
                photoAdapter.addPhotos(newSelectedUris)
                updateUploadStatus()
            } else if (data.clipData != null || data.data != null) {
                // All selected photos were duplicates
                Toast.makeText(this, "Photos already selected", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQUEST_DELETE_PHOTOS) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Successfully deleted photos.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Could not delete all photos.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUploadStatus() {
        uploadStatusText.visibility = View.VISIBLE
        uploadStatusText.text = getString(R.string.ready_to_upload, selectedPhotos.size)
        uploadButton.visibility = View.VISIBLE
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

        // Show progress section and initialize
        uploadProgressSection.visibility = View.VISIBLE
        uploadProgressBar.max = selectedPhotos.size
        uploadProgressBar.progress = 0
        uploadStageText.text = getString(R.string.uploading_photos)
        uploadStatusText.text = getString(R.string.upload_starting)
        uploadButton.isEnabled = false

        coroutineScope.launch {
            var successCount = 0
            var failCount = 0
            val successfullyUploadedUris = mutableListOf<Uri>()

            // Upload Phase
            for ((index, uri) in selectedPhotos.withIndex()) {
                try {
                    val fileName = getFileNameFromUri(uri)
                    val file = createTempFileFromUri(uri)

                    if (file != null) {
                        val result = uploadFileToNextCloud(file, fileName)
                        if (result) {
                            successCount++
                            successfullyUploadedUris.add(uri)
                        } else {
                            failCount++
                        }
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
                } catch (_: Exception) {
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

            // Switch to deletion phase
            withContext(Dispatchers.Main) {
                uploadStageText.text = getString(R.string.deleting_photos)
                uploadStatusText.text = getString(R.string.preparing_to_delete, successfullyUploadedUris.size)
            }

            // Delete successfully uploaded photos from device
            deletePhotosFromDevice(successfullyUploadedUris)

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
                    uploadProgressSection.visibility = View.GONE
                } else {
                    // Remove only successfully uploaded photos from selection
                    selectedPhotos.removeAll(successfullyUploadedUris)
                    photoAdapter.setPhotos(selectedPhotos)
                }

                uploadButton.isEnabled = true
            }
        }
    }

    private fun deletePhotosFromDevice(uris: List<Uri>) {
        if (uris.isEmpty()) return

        try {
            val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
            startIntentSenderForResult(pendingIntent.intentSender, REQUEST_DELETE_PHOTOS, null, 0, 0, 0)
        } catch (_: IllegalArgumentException) {
            // This can happen if URIs are not from MediaStore (e.g. Google Photos).
            // Fallback to individual deletion.
            var couldNotDelete = false
            for (uri in uris) {
                try {
                    if (!DocumentsContract.deleteDocument(contentResolver, uri)) {
                        couldNotDelete = true
                    }
                } catch (_: Exception) {
                    // If DocumentsContract fails, try contentResolver.delete as a last resort.
                    try {
                        if (contentResolver.delete(uri, null, null) == 0) {
                            couldNotDelete = true
                        }
                    } catch (e2: Exception) {
                        couldNotDelete = true
                        e2.printStackTrace()
                    }
                }
            }
            if (couldNotDelete) {
                Toast.makeText(this, "Could not delete all photos.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Successfully deleted photos.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Could not start delete request.", Toast.LENGTH_SHORT).show()
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
            val baseUrl = nextCloudConfig.getServerUrl()?.trimEnd('/')
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
                .header("Authorization", Credentials.basic(username ?: "", password ?: ""))
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

    private fun removePhoto(uri: Uri, position: Int) {
        selectedPhotos.remove(uri)
        photoAdapter.removePhoto(position)

        // Update upload status
        if (selectedPhotos.isEmpty()) {
            uploadButton.visibility = View.GONE
            uploadStatusText.visibility = View.GONE
        } else {
            updateUploadStatus()
        }

        Toast.makeText(this, getString(R.string.photo_removed), Toast.LENGTH_SHORT).show()
    }
}
