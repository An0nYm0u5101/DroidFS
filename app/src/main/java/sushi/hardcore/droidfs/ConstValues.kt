package sushi.hardcore.droidfs

import android.net.Uri
import java.io.File

class ConstValues {
    companion object {
        const val creator = "DroidFS"
        const val saved_volumes_key = "saved_volumes"
        const val sort_order_key = "sort_order"
        val fakeUri: Uri = Uri.parse("fakeuri://droidfs")
        const val MAX_KERNEL_WRITE = 128*1024
        const val wipe_passes = 2
        const val slideshow_delay: Long = 4000
        private val fileExtensions = mapOf(
            Pair("image", listOf("png", "jpg", "jpeg", "gif", "bmp")),
            Pair("video", listOf("mp4", "webm", "mkv", "mov")),
            Pair("audio", listOf("mp3", "ogg", "m4a", "wav", "flac")),
            Pair("text", listOf("txt", "json", "conf", "log", "xml", "java", "kt", "py", "pl", "rb", "go", "c", "h", "cpp", "hpp", "sh", "bat", "js", "html", "css", "php", "yml", "yaml", "ini", "md"))
        )

        fun isImage(path: String): Boolean {
            return fileExtensions["image"]?.contains(File(path).extension) ?: false
        }
        fun isVideo(path: String): Boolean {
            return fileExtensions["video"]?.contains(File(path).extension) ?: false
        }
        fun isAudio(path: String): Boolean {
            return fileExtensions["audio"]?.contains(File(path).extension) ?: false
        }
        fun isText(path: String): Boolean {
            return fileExtensions["text"]?.contains(File(path).extension) ?: false
        }
        fun getAssociatedDrawable(path: String): Int {
            return when {
                isAudio(path) -> R.drawable.icon_file_audio
                isImage(path) -> R.drawable.icon_file_image
                isVideo(path) -> R.drawable.icon_file_video
                isText(path) -> R.drawable.icon_file_text
                else -> R.drawable.icon_file_unknown
            }
        }
    }
}