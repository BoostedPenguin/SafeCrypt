package com.penguinstudio.safecrypt.ui.home

import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.penguinstudio.safecrypt.NavGraphDirections
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.PhotoGridAdapter
import com.penguinstudio.safecrypt.databinding.FragmentPicturesBinding
import com.penguinstudio.safecrypt.models.MediaModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PicturesFragment : Fragment() {
    private lateinit var binding: FragmentPicturesBinding
    private lateinit var photoAdapter: PhotoGridAdapter
    private val _model: GalleryViewModel by activityViewModels()
    private val model: IPicturesViewModel
        get() {
            return _model
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    // Handle the back button event
                    if(model.itemSelectionMode) {
                        exitSelectMode()
                        return
                    }
                    findNavController().popBackStack()
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentPicturesBinding.inflate(layoutInflater, container, false)
        setHasOptionsMenu(true)

        (activity as AppCompatActivity?)?.supportActionBar?.title = model.selectedAlbum?.name
        (activity as AppCompatActivity).supportActionBar?.show()

        initGrid()

        registerEvents()

        photoAdapter.setImages(model.selectedAlbum?.albumMedia)

        return binding.root
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_select_all -> {
                model.addAllMediaToSelection(photoAdapter.getImages())
                photoAdapter.notifyItemRangeChanged(0, photoAdapter.itemCount)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun initGrid() {
        photoAdapter = PhotoGridAdapter(object: PhotoGridAdapter.AdapterListeners {
            override fun onClickListener(position: Int, media: MediaModel) {

                // If in selection mode add / remove, else trigger normal action
                if(model.itemSelectionMode) {

                    if(model.selectedItems.value?.contains(media) == true) {
                        model.removeMediaFromSelection(position, media)
                        media.isSelected = false
                    }
                    else {
                        model.addMediaToSelection(position, media)
                        media.isSelected = true
                    }

                    // Notify adapter that this item has changed
                    photoAdapter.notifyItemChanged(position)
                }
                else {
                    model.setSelectedMedia(media)
                    findNavController().navigate(R.id.action_picturesFragment_to_selectedPicture)
                }
            }
            override fun onLongClickListener(position: Int, media: MediaModel) {
                if(model.itemSelectionMode) return
                photoAdapter.toggleSelectionMode(true)

                model.itemSelectionMode = true

                model.addMediaToSelection(position, media)
                (activity as AppCompatActivity?)?.supportActionBar?.title = "Select Media"
                // Notify adapter that this item has changed
                activity?.invalidateOptionsMenu()
                media.isSelected = true
                photoAdapter.notifyItemChanged(position)
            }
        })
        binding.picturesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.picturesRecyclerView.adapter = photoAdapter
    }

    private fun registerEvents() {
        /**
         * If the selected items become 0,
         * Disable selection mode
         */
        model.selectedItems.observe(viewLifecycleOwner, {
            if(it != null && it.size == 0) {
                photoAdapter.toggleSelectionMode(false)

                exitSelectMode()
            }
        })
    }

    private fun exitSelectMode() {
        (activity as AppCompatActivity?)?.supportActionBar?.title = model.selectedAlbum?.name

        activity?.invalidateOptionsMenu()
        model.itemSelectionMode = false
        model.selectedItems.value?.clear()
        photoAdapter.toggleSelectionMode(false)
    }

    /**
     * Triggers only once when fragment loads
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    /**
     * Triggers programmatically and on every menu item click
     */
    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
        super.onPrepareOptionsMenu(menu)

        if(model.itemSelectionMode) {
            activity?.menuInflater?.inflate(R.menu.item_selected_menu , menu)
        }
        else {
            activity?.menuInflater?.inflate(R.menu.main_menu , menu)
        }
    }
}