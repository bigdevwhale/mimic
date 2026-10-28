package app.mimic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/** Screenshots from sessions, copied into the app's own folder at a sane size. */
object Shots {
    fun dir(context: Context): File = File(context.filesDir, "shots").apply { mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    fun save(context: Context, uri: Uri): String? = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.let { saveBytes(context, it) } }.getOrNull()

    fun saveFile(context: Context, f: File): String? = runCatching { saveBytes(context, f.readBytes()) }.getOrNull()

    private fun saveBytes(context: Context, bytes: ByteArray): String? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        if (o.outWidth <= 0) return null
        var s = 1
        while (o.outWidth / (s * 2) >= 1600 || o.outHeight / (s * 2) >= 1600) s *= 2
        val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s }) ?: return null
        val name = UUID.randomUUID().toString() + ".jpg"
        val rot = runCatching { androidx.exifinterface.media.ExifInterface(bytes.inputStream()).rotationDegrees }.getOrDefault(0)
        val up = if (rot == 0) b else Bitmap.createBitmap(b, 0, 0, b.width, b.height, android.graphics.Matrix().apply { postRotate(rot.toFloat()) }, true)
        file(context, name).outputStream().use { up.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return name
    }

    /** A small decode for thumbnails and the viewer. */
    fun bitmap(context: Context, name: String, max: Int): Bitmap? = runCatching {
        val f = file(context, name)
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        var s = 1
        while (o.outWidth / (s * 2) >= max && o.outHeight / (s * 2) >= max) s *= 2
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
    }.getOrNull()

    fun delete(context: Context, names: List<String>) = names.forEach { runCatching { file(context, it).delete() } }
}
