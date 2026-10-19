package com.r7tkeysx.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File

// Kho nhãn dán người dùng tự tạo: mỗi nhãn là một file PNG trong thư mục riêng của app (không cần quyền gì).
// Studio (Activity) và bàn phím (Service) chạy chung một tiến trình nên dùng chung được `revision`:
// Studio lưu xong thì tăng số này, bảng nhãn dán trên bàn phím tự vẽ lại.
object StickerStore {
    var revision by mutableIntStateOf(0)
        private set

    fun dir(context: Context): File = File(context.filesDir, "stickers").also { it.mkdirs() }

    // mới nhất đứng đầu
    fun list(context: Context): List<File> =
        dir(context).listFiles { f -> f.extension == "png" }.orEmpty().sortedByDescending { it.lastModified() }

    fun save(context: Context, bitmap: Bitmap): File {
        val file = File(dir(context), "stk_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        revision++
        return file
    }

    fun delete(file: File) {
        file.delete()
        revision++
    }

    // Đọc ảnh thu nhỏ cho ô xem trước (không nạp nguyên ảnh 512px cho mỗi ô)
    fun thumbnail(file: File, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSide) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
