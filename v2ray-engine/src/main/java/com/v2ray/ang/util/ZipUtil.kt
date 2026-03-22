package com.v2ray.ang.util

import android.util.Log
import com.v2ray.ang.AppConfig
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ZipUtil {
    private const val BUFFER_SIZE = 4096

    fun zipFromFolder(folderPath: String, outputZipFilePath: String): Boolean {
        if (folderPath.isEmpty() || outputZipFilePath.isEmpty()) return false
        val directory = File(folderPath)
        if (!directory.isDirectory) return false
        val filesToCompress =
            directory.listFiles()?.filter { it.isFile }?.map { it.absolutePath }.orEmpty()
        if (filesToCompress.isEmpty()) return false

        return try {
            ZipOutputStream(FileOutputStream(outputZipFilePath)).use { zos ->
                val buffer = ByteArray(BUFFER_SIZE)
                for (path in filesToCompress) {
                    val file = File(path)
                    FileInputStream(file).use { inputStream ->
                        zos.putNextEntry(ZipEntry(file.name))
                        while (true) {
                            val len = inputStream.read(buffer)
                            if (len <= 0) break
                            zos.write(buffer, 0, len)
                        }
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: IOException) {
            Log.e(AppConfig.TAG, "Failed to zip folder", e)
            false
        }
    }

    fun unzipToFolder(zipFile: File, destDirectory: String): Boolean {
        File(destDirectory).apply {
            if (!exists()) mkdirs()
        }
        return try {
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        val filePath = destDirectory + File.separator + entry.name
                        if (entry.isDirectory) {
                            File(filePath).mkdirs()
                        } else {
                            extractFile(input, filePath)
                        }
                    }
                }
            }
            true
        } catch (e: IOException) {
            Log.e(AppConfig.TAG, "Failed to unzip file", e)
            false
        }
    }

    private fun extractFile(inputStream: InputStream, destFilePath: String) {
        val outFile = File(destFilePath)
        outFile.parentFile?.mkdirs()
        BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
            val bytesIn = ByteArray(BUFFER_SIZE)
            var read: Int
            while (inputStream.read(bytesIn).also { read = it } != -1) {
                bos.write(bytesIn, 0, read)
            }
        }
    }
}
