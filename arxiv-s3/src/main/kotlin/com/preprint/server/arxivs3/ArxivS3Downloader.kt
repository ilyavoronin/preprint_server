package com.preprint.server.arxivs3

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.lang.Exception

object ArxivS3Downloader {
    val logger = logger()
    const val bucketName = "arxiv"
    const val region = "us-east-1"
    const val manifestKey = "pdf/arXiv_pdf_manifest.xml"


    val credentials = try {
        ProfileCredentialsProvider().getCredentials();
    } catch (e: Exception) {
        throw AmazonClientException(
                "Cannot load the credentials from the credential profiles file. " +
                        "Please make sure that your credentials file is at the correct " +
                        "location (~/.aws/credentials), and is in valid format.", e)
    }

    val amazonS3 = AmazonS3ClientBuilder.standard()
        .withCredentials(AWSStaticCredentialsProvider(credentials))
        .withRegion(region)
        .build()


    fun downloadManifest(path : String) {

        logger.info("Begin manifest download")
        amazonS3.getObject(
            GetObjectRequest(bucketName, manifestKey, true), File(path)
        ) ?: throw DownloaderException("Failed to dowload")
        logger.info("Download finished")
    }

    fun download(pdfKey : String, path : String) {
        logger.info("Begin $pdfKey download")
        val pdf = amazonS3.getObject(
            GetObjectRequest(bucketName, pdfKey, true), File(path)
        ) ?: throw DownloaderException("Failed to download")
        logger.info("Donwload finished")
    }

    class DownloaderException(override val message : String) : Exception(message)
}