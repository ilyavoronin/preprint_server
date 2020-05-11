package com.preprint.server.arxivs3

import com.amazonaws.AmazonClientException
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.event.ProgressEvent
import com.amazonaws.event.ProgressListener
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.transfer.PersistableTransfer
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.amazonaws.services.s3.transfer.TransferProgress
import com.amazonaws.services.s3.transfer.internal.S3ProgressListener
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.lang.Thread.sleep
import kotlin.math.roundToInt


/**
 * Downloads archives with pdf and manifest from AmazonS3 requester pays bucket
 * In order to work with AmazonS3 you need to specify your credentials in `~/.aws/credentials`
 * IMPORTANT: You pay for downloading any data from AmazonS3 requester pays buckets
 */
object ArxivS3Downloader {
    val logger = logger()
    const val bucketName = "arxiv"
    const val region = "us-east-1"
    const val manifestKey = "pdf/arXiv_pdf_manifest.xml"


    private var credentials = try {
        ProfileCredentialsProvider().getCredentials();
    } catch (e: Exception) {
        throw AmazonClientException(
                "Cannot load the credentials from the credential profiles file. " +
                        "Please make sure that your credentials file is at the correct " +
                        "location (~/.aws/credentials), and is in valid format.", e)
    }

    private var amazonS3 = AmazonS3ClientBuilder.standard()
        .withCredentials(AWSStaticCredentialsProvider(credentials))
        .withRegion(region)
        .build()


    /**
     * Downloads arxiv's manifest.xml file into the given directory
     */
    fun downloadManifest(path: String) {
        logger.info("Begin manifest download")
        val request = GetObjectRequest(bucketName, manifestKey, true)
        val transferManager = TransferManagerBuilder.standard().withS3Client(amazonS3).build()
        val p = transferManager.download(request, File(path))
        while (!p.isDone) {
            println(p.progress.bytesTransferred / p.progress.totalBytesToTransfer)
            sleep(1000)
        }

        logger.info("Download finished")
    }


    /**
     * Download archive into the given directory
     */
    fun download(pdfKey: String, path: String) {
        reloadCredentials()
        logger.info("Begin $pdfKey download")
        val request = GetObjectRequest(bucketName, pdfKey, true)
        val transferManager = TransferManagerBuilder.standard().withS3Client(amazonS3).build()
        val p = transferManager.download(request, File(path))
        while (!p.isDone) {
            progress(pdfKey, p.progress)
            sleep(10000)
        }
        logger.info("Donwload finished")
    }

    private fun progress(pdfKey: String, progress: TransferProgress) {
        logger.info(pdfKey + " downloaded: "
               + (progress.bytesTransferred.toDouble() * 100 / progress.totalBytesToTransfer.toDouble())
                        .roundToInt().toString()
               + "%"
        )
    }

    private fun reloadCredentials() {
        credentials = try {
            ProfileCredentialsProvider().getCredentials();
        } catch (e: Exception) {
            throw AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.", e)
        }

        amazonS3 = AmazonS3ClientBuilder.standard()
                .withCredentials(AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build()
    }

    class DownloadFailedException(override val message : String) : Exception(message)
}