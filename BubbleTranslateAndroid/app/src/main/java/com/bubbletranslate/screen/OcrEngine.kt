package com.bubbletranslate.screen

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RecognizedBlock(
    val originalText: String,
    var translatedText: String = "",
    val boundingBox: Rect
)

object OcrEngine {

    private val latinRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val chineseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    private val japaneseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    private val koreanRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    private fun getRecognizer(langCode: String): TextRecognizer {
        return when (langCode.uppercase()) {
            "ZH" -> chineseRecognizer
            "JA" -> japaneseRecognizer
            "KO" -> koreanRecognizer
            else -> latinRecognizer
        }
    }

    suspend fun recognizeText(bitmap: Bitmap, fromLang: String = "EN"): List<RecognizedBlock> = withContext(Dispatchers.Default) {
        val blocks = mutableListOf<RecognizedBlock>()
        try {
            val recognizer = getRecognizer(fromLang)
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = Tasks.await(recognizer.process(image))

            for (textBlock in visionText.textBlocks) {
                for (line in textBlock.lines) {
                    val box = line.boundingBox
                    val text = line.text.trim()
                    if (box != null && text.isNotEmpty() && box.width() > 10 && box.height() > 8) {
                        blocks.add(
                            RecognizedBlock(
                                originalText = text,
                                translatedText = "",
                                boundingBox = box
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext blocks
    }
}
