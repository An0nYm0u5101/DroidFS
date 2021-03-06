package sushi.hardcore.droidfs.file_viewers

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import kotlinx.android.synthetic.main.activity_image_viewer.*
import sushi.hardcore.droidfs.ConstValues
import sushi.hardcore.droidfs.R
import sushi.hardcore.droidfs.explorers.ExplorerElement
import sushi.hardcore.droidfs.util.MiscUtils
import sushi.hardcore.droidfs.util.PathUtils
import sushi.hardcore.droidfs.widgets.ColoredAlertDialogBuilder
import sushi.hardcore.droidfs.widgets.ZoomableImageView
import java.io.File
import java.security.MessageDigest
import kotlin.math.abs

class ImageViewer: FileViewerActivity() {
    companion object {
        private const val hideDelay: Long = 3000
        private const val MIN_SWIPE_DISTANCE = 150
    }
    private lateinit var fileName: String
    private lateinit var glideImage: RequestBuilder<Drawable>
    private var x1 = 0F
    private var x2 = 0F
    private val mappedImages = mutableListOf<ExplorerElement>()
    private lateinit var sortOrder: String
    private var wasMapped = false
    private var slideshowActive = false
    private var currentMappedImageIndex = -1
    private var rotationAngle: Float = 0F
    private val handler = Handler()
    private val hideUI = Runnable {
        action_buttons.visibility = View.GONE
        action_bar.visibility = View.GONE
    }
    override fun viewFile() {
        setContentView(R.layout.activity_image_viewer)
        image_viewer.setOnInteractionListener(object : ZoomableImageView.OnInteractionListener {
            override fun onSingleTap(event: MotionEvent?) {
                handler.removeCallbacks(hideUI)
                if (action_buttons.visibility == View.GONE) {
                    action_buttons.visibility = View.VISIBLE
                    action_bar.visibility = View.VISIBLE
                    handler.postDelayed(hideUI, hideDelay)
                } else {
                    hideUI.run()
                }
            }

            override fun onTouch(event: MotionEvent?) {
                if (!image_viewer.isZoomed) {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            x1 = event.x
                        }
                        MotionEvent.ACTION_UP -> {
                            x2 = event.x
                            val deltaX = x2 - x1
                            if (abs(deltaX) > MIN_SWIPE_DISTANCE) {
                                swipeImage(deltaX)
                            }
                        }
                    }
                }
            }
        })
        loadImage()
        handler.postDelayed(hideUI, hideDelay)
    }

    private fun loadImage(){
        loadWholeFile(filePath)?.let {
            glideImage = Glide.with(this).load(it)
            glideImage.into(image_viewer)
            fileName = File(filePath).name
            text_filename.text = fileName
            rotationAngle = 0F
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        handler.removeCallbacks(hideUI)
        handler.postDelayed(hideUI, hideDelay)
    }

    private fun swipeImage(deltaX: Float){
        if (!wasMapped){
            for (e in gocryptfsVolume.recursiveMapFiles(PathUtils.getParentPath(filePath))){
                if (e.isRegularFile && ConstValues.isImage(e.name)){
                    mappedImages.add(e)
                }
            }
            sortOrder = intent.getStringExtra("sortOrder") ?: "name"
            ExplorerElement.sortBy(sortOrder, mappedImages)
            for ((i, e) in mappedImages.withIndex()){
                if (filePath == e.fullPath){
                    currentMappedImageIndex = i
                    break
                }
            }
            wasMapped = true
        }
        currentMappedImageIndex = if (deltaX < 0){
            MiscUtils.incrementIndex(currentMappedImageIndex, mappedImages)
        } else {
            MiscUtils.decrementIndex(currentMappedImageIndex, mappedImages)
        }
        filePath = mappedImages[currentMappedImageIndex].fullPath
        loadImage()
    }

    fun onClickDelete(view: View) {
        ColoredAlertDialogBuilder(this)
            .keepFullScreen()
            .setTitle(R.string.warning)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (gocryptfsVolume.removeFile(filePath)){
                    currentMappedImageIndex = MiscUtils.decrementIndex(currentMappedImageIndex, mappedImages)
                    mappedImages.clear()
                    wasMapped = false
                    swipeImage(-1F)
                } else {
                    ColoredAlertDialogBuilder(this)
                        .keepFullScreen()
                        .setTitle(R.string.error)
                        .setMessage(getString(R.string.remove_failed, fileName))
                        .setPositiveButton(R.string.ok, null)
                        .show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setMessage(getString(R.string.single_delete_confirm, fileName))
            .show()
    }

    fun onClickSlideshow(view: View) {
        if (!slideshowActive){
            slideshowActive = true
            Thread {
                Thread.sleep(ConstValues.slideshow_delay)
                while (slideshowActive){
                    runOnUiThread { swipeImage(-1F) }
                    Thread.sleep(ConstValues.slideshow_delay)
                }
            }.start()
            hideUI.run()
            Toast.makeText(this, R.string.slideshow_started, Toast.LENGTH_SHORT).show()
        } else {
            stopSlideshow()
        }
    }

    private fun stopSlideshow(){
        slideshowActive = false
        Toast.makeText(this, R.string.slideshow_stopped, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (slideshowActive){
            stopSlideshow()
        } else {
            super.onBackPressed()
        }
    }

    class RotateTransformation(private val rotationAngle: Float): BitmapTransformation() {

        override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(rotationAngle)
            return Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true)
        }

        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update("rotate$rotationAngle".toByteArray())
        }
    }

    private fun rotateImage(){
        image_viewer.restoreZoomNormal()
        glideImage.transform(RotateTransformation(rotationAngle)).into(image_viewer)
    }
    fun onCLickRotateRight(view: View){
        rotationAngle += 90
        rotateImage()
    }
    fun onClickRotateLeft(view: View){
        rotationAngle -= 90
        rotateImage()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        image_viewer.restoreZoomNormal()
    }
}