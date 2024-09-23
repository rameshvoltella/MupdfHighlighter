package com.rameshvoltella.pdfeditorpro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.rameshvoltella.pdfeditorpro.constants.Constants
import com.rameshvoltella.pdfeditorpro.ui.component.PdfEditorProActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        copyFileFromAssetsToInternal(this, "example.pdf")
        copyFileFromAssetsToInternal(this, "testfile.pdf")
        startActivity(Intent(this@MainActivity, PdfEditorProActivity::class.java).apply {
            putExtra(Constants.PDF_FILE_PATH, "${filesDir.path}/testfile.pdf")
            putExtra(Constants.DOC_ID, -1L)
            putExtra(Constants.DIRECT_DOC_EDIT_OPEN, false)
            putExtra(Constants.DOC_NAME, "testfile.pdf")
        })
    }

    private fun copyFileFromAssetsToInternal(context: Context, assetFileName: String): String? {
        val inputStream: InputStream
        val outputStream: OutputStream
        try {
            inputStream = context.assets.open(assetFileName)
            val outputFile = File(context.filesDir, assetFileName)
            outputStream = FileOutputStream(outputFile)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            return outputFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

}