package com.devfest.runtime.engine.blocks

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.devfest.runtime.engine.FlowBlockHandler
import com.devfest.runtime.engine.FlowExecutionInput
import com.devfest.runtime.engine.FlowExecutionState
import com.devfest.runtime.engine.FlowStepResult
import com.devfest.runtime.engine.FlowStepStatus
import com.devfest.runtime.model.FlowBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraCaptureHandler(
    private val context: Context
) : FlowBlockHandler {

    companion object {
        private const val TAG = "CameraCaptureHandler"
        /**
         * Delay after binding camera before taking photo.
         * Emulators need extra time for camera pipeline initialization.
         */
        private const val CAMERA_WARM_UP_MS = 3000L
        /** Number of capture attempts before giving up. */
        private const val MAX_RETRIES = 3
        /** Delay between retry attempts. */
        private const val RETRY_DELAY_MS = 1500L
    }

    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        return try {
            Log.d(TAG, "Starting camera capture (lens=${block.params["lens"] ?: "front"})")
            val photoFile = takePhotoWithRetry(block.params)
            Log.d(TAG, "Photo captured successfully: ${photoFile.absolutePath} (${photoFile.length()} bytes)")
            val galleryPath = saveToGallery(photoFile)
            state.variables["last_photo"] = galleryPath
            Log.d(TAG, "Photo saved to gallery: $galleryPath")
            FlowStepResult(block.id, FlowStepStatus.SUCCESS, "Photo saved: ${photoFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Photo capture failed after $MAX_RETRIES attempts", e)
            FlowStepResult(block.id, FlowStepStatus.FAILED, "Photo failed: ${e.message}")
        }
    }

    /**
     * Attempts photo capture with retries.
     * Emulator camera can be flaky — retry gives it additional chances to initialise.
     */
    private suspend fun takePhotoWithRetry(params: Map<String, String>): File {
        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                Log.d(TAG, "Capture attempt $attempt/$MAX_RETRIES")
                return takePhoto(params)
            } catch (e: Exception) {
                Log.w(TAG, "Attempt $attempt failed: ${e.message}")
                lastException = e
                if (attempt < MAX_RETRIES) {
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        throw lastException ?: RuntimeException("Photo capture failed after $MAX_RETRIES attempts")
    }

    /**
     * Saves the captured photo to the device gallery so it appears in the Photos app.
     * Uses MediaStore on API 29+ (scoped storage) and MediaScanner on older versions.
     * Returns the final path/URI string for reference.
     */
    private fun saveToGallery(photoFile: File): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val displayName = "LoginSecurity_$timestamp"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+: Use MediaStore (scoped storage)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/LoginSecurity")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    photoFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                // Clean up the temp file
                photoFile.delete()
                return uri.toString()
            }
        }

        // Fallback for API < 29: copy to public Pictures and scan
        val picturesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "LoginSecurity"
        )
        picturesDir.mkdirs()
        val destFile = File(picturesDir, "$displayName.jpg")
        photoFile.copyTo(destFile, overwrite = true)
        photoFile.delete()

        // Notify media scanner so it shows in gallery
        MediaScannerConnection.scanFile(
            context,
            arrayOf(destFile.absolutePath),
            arrayOf("image/jpeg"),
            null
        )
        return destFile.absolutePath
    }

    private suspend fun takePhoto(params: Map<String, String>): File = withContext(Dispatchers.Main) {
        // Create our own LifecycleOwner so CameraX works from any context (Activity, Service, etc.)
        val serviceLifecycleOwner = ServiceLifecycleOwner()
        serviceLifecycleOwner.start()
        Log.d(TAG, "ServiceLifecycleOwner started")

        val cameraProvider = getCameraProvider(context)
        Log.d(TAG, "CameraProvider obtained")

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val lensFacing = when (params["lens"]?.lowercase()) {
            "back", "rear" -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> CameraSelector.DEFAULT_FRONT_CAMERA
        }

        try {
            // Unbind any existing use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to our service lifecycle owner
            val camera = cameraProvider.bindToLifecycle(
                serviceLifecycleOwner,
                lensFacing,
                imageCapture
            )
            Log.d(TAG, "Camera bound to lifecycle, state: ${camera.cameraInfo.cameraState.value}")

            // Give camera pipeline time to initialise before capturing
            Log.d(TAG, "Waiting ${CAMERA_WARM_UP_MS}ms for camera warm-up…")
            delay(CAMERA_WARM_UP_MS)

            // Create temp output file (will be moved to gallery after capture)
            val outputDir = context.cacheDir
            val photoFile = File(
                outputDir,
                "security_" + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                    .format(System.currentTimeMillis()) + ".jpg"
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            Log.d(TAG, "Taking picture…")
            suspendCoroutine { cont ->
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e(TAG, "Image capture error: ${exc.imageCaptureError}", exc)
                            cont.resumeWithException(exc)
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d(TAG, "Image saved to temp: ${photoFile.absolutePath} (${photoFile.length()} bytes)")
                            cont.resume(photoFile)
                        }
                    }
                )
            }
        } finally {
            // Always clean up: unbind camera and destroy lifecycle
            Log.d(TAG, "Cleaning up camera resources")
            cameraProvider.unbindAll()
            serviceLifecycleOwner.stop()
        }
    }

    private suspend fun getCameraProvider(context: Context): ProcessCameraProvider =
        suspendCoroutine { cont ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    cont.resume(cameraProviderFuture.get())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get CameraProvider", e)
                    cont.resumeWithException(e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
}
