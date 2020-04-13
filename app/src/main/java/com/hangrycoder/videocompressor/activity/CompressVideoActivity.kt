package com.hangrycoder.videocompressor.activity

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.hangrycoder.videocompressor.R
import com.hangrycoder.videocompressor.databinding.ActivityCompressVideoBindingImpl
import com.hangrycoder.videocompressor.utils.UriUtils
import com.hangrycoder.videocompressor.utils.Util
import com.hangrycoder.videocompressor.utils.VideoCompression
import com.hangrycoder.videocompressor.utils.createFolderIfDoesntExist
import kotlinx.android.synthetic.main.activity_compress_video.*
import java.io.File

class CompressVideoActivity : AppCompatActivity() {

    private var exoPlayer: SimpleExoPlayer? = null
    private val TAG = CompressVideoActivity::class.java.simpleName
    private var videoUri: Uri? = null

    private var compressedVideosFolder: File = File(
        Environment.getExternalStorageDirectory().path + File.separator.toString() + OUTPUT_FILE_DIRECTORY_NAME
    )
    val outputFileAbsolutePath = compressedVideosFolder.absolutePath +
            File.separator.toString() +
            System.currentTimeMillis() + FILE_EXTENTION

    private val progressDialog: ProgressDialog by lazy {
        ProgressDialog(this).apply {
            setMessage("Video compression is in progress. Please wait :)")
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }
    }

    private val videoCompression: VideoCompression by lazy {
        VideoCompression(this, object : VideoCompression.CompressionCallbacks {
            override fun onStart() {
                showLoader()
            }

            override fun onFinish() {
                hideLoader()
            }

            override fun onSuccess() {
                Util.showToast(applicationContext, "Video compressed successfully")
                val intent = Intent(
                    this@CompressVideoActivity,
                    PlayCompressedVideoActivity::class.java
                ).apply {
                    putExtra(
                        PlayCompressedVideoActivity.INTENT_COMPRESSED_VIDEO_PATH,
                        outputFileAbsolutePath
                    )
                }
                startActivity(intent)
                finish()
            }

            override fun onFailure() {
                Util.showToast(applicationContext, "Video compression failed")
            }

        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDataBinding()
        setIntentParams()
        initializeExoPlayerAndPlayVideo()
    }

    private fun initDataBinding() {
        val binding = DataBindingUtil.setContentView<ActivityCompressVideoBindingImpl>(
            this, R.layout.activity_compress_video
        )
        binding.setCompressVideoClick {
            createOutputFolderIfDoesntExistAndCompressVideo()
        }
    }

    private fun createOutputFolderIfDoesntExistAndCompressVideo() {
        val inputFilePath = UriUtils.getImageFilePath(this, videoUri)
        inputFilePath ?: return

        compressedVideosFolder.createFolderIfDoesntExist()

        videoCompression.compressVideo(
            bitrate = inputBitrate.text.toString(),
            inputFilePath = inputFilePath,
            outputFilePath = outputFileAbsolutePath
        )
    }

    private fun setIntentParams() {
        videoUri = Uri.parse(intent.extras?.getString(INTENT_VIDEO_URI))
        Util.showLogE(TAG, "VideoUri ${UriUtils.getImageFilePath(this, videoUri)}")
    }

    private fun initializeExoPlayerAndPlayVideo() {
        val trackSelector = DefaultTrackSelector(this)
        val loadControl = DefaultLoadControl()
        val renderersFactory = DefaultRenderersFactory(this)

        exoPlayer = ExoPlayerFactory.newSimpleInstance(
            this,
            renderersFactory, trackSelector, loadControl
        )

        val userAgent = "Compressed Video"
        val mediaSource = ExtractorMediaSource
            .Factory(DefaultDataSourceFactory(this, userAgent))
            .setExtractorsFactory(DefaultExtractorsFactory())
            .createMediaSource(videoUri)

        exoPlayer?.prepare(mediaSource)
        exoPlayer?.playWhenReady = true

        playerView.player = exoPlayer
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.release()
    }

    private fun hideLoader() {
        progressDialog.dismiss()
    }

    private fun showLoader() {
        progressDialog.show()
    }

    companion object {
        private const val FILE_EXTENTION = ".mp4"
        const val OUTPUT_FILE_DIRECTORY_NAME = "CompressedVideos"
        const val INTENT_VIDEO_URI = "video_uri"
    }
}