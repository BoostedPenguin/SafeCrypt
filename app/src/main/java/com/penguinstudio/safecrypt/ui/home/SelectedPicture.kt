package com.penguinstudio.safecrypt.ui.home

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.databinding.FragmentSelectedPictureBinding
import com.penguinstudio.safecrypt.models.MediaType


class SelectedPicture : Fragment() {
    private lateinit var binding: FragmentSelectedPictureBinding

    private val _model: GalleryViewModel by activityViewModels()
    private val model: ISelectedMediaViewModel
        get() {
            return _model
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectedPictureBinding.inflate(layoutInflater, container, false)
        (activity as AppCompatActivity).supportActionBar?.hide()

        when(model.selectedMedia?.mediaType) {
            MediaType.IMAGE -> {
                binding.selectedPicture.visibility = View.VISIBLE

                createPhotoSetup()
            }
            MediaType.VIDEO -> {
                binding.selectedVideo.visibility = View.VISIBLE

                createVideoPlayback()
            }
            else -> {
                Log.d("error", "Image Media type not provided")
            }
        }
        return binding.root
    }

    private fun createPhotoSetup() {

        binding.selectedFragmentToolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.selectedFragmentToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        hideActionBarWithDelay()


        Glide.with(this)
            .load(model.selectedMedia?.mediaUri)
            .thumbnail(0.1f)
            .into(binding.selectedPicture)

        binding.selectedPicture.setOnClickListener {
            hideActionBarWithDelay()
        }
    }
    private val handler = Handler()
    val r: Runnable = Runnable { binding.selectedFragmentToolbar.visibility = View.INVISIBLE }
    private fun hideActionBarWithDelay(delay: Long = 2000) {
        handler.removeCallbacks(r)

        binding.selectedFragmentToolbar.visibility = View.VISIBLE
        handler.postDelayed(r, delay)
    }

    private fun createVideoPlayback() {

        val player = SimpleExoPlayer.Builder(requireContext()).build()

        // Bind the player to the view.
        binding.selectedVideo.player = player;

        val mediaItem: MediaItem = MediaItem.fromUri(model.selectedMedia?.mediaUri!!)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        binding.selectedVideo.findViewById<ImageButton>(R.id.video_back).setOnClickListener {
            player.stop()
            player.clearMediaItems()
            player.release()
            findNavController().popBackStack()
        }
    }
}