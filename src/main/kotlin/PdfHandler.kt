package testpdf

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result;
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File
import java.lang.Thread.sleep

object PdfHandler {
    fun getFullInfo(recordList : List <ArxivData>, outputPath : String) {
        for ((i, record) in recordList.withIndex()) {
            println("downloading: $i: ${record.id}")
            println(record.pdf)
            if (record.pdf == "") {
                File(outputPath + "failed.txt").writeText("${record.id}\n")
                continue
            }
            val pdf = downloadPdf(record.pdf) ?: return
            File("$outputPath${record.id}.pdf").writeBytes(pdf)
            val (pdfText, pageWidth) = parsePdf(pdf)
            record.refList = ReferenceExtractor.extract(pdfText, pageWidth)
            sleep(3000)
        }
    }

    fun downloadPdf(url : String) : ByteArray? {
        val (request, response, result) = url
            .httpGet()
            .response()
        return when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                println(ex)
                null
            }
            is Result.Success -> {
                println("Success")
                result.get()
            }
        }
    }
    fun parsePdf(pdf : ByteArray) : Pair<String, Double> {
        val pdfStripper = PDFBoldTextStripper()
        val doc = PDDocument.load(pdf)
        val pageWidth = doc.pages[0].mediaBox.width.toDouble()
        val text = pdfStripper.getText(doc)
        doc.close()
        return Pair(text, pageWidth)
    }
}