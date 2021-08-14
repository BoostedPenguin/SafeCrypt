package com.penguinstudio.safecrypt.ui.home

import androidx.lifecycle.*
import com.penguinstudio.safecrypt.models.AlbumModel
import com.penguinstudio.safecrypt.models.MediaModel
import com.penguinstudio.safecrypt.repository.MediaRepository
import com.penguinstudio.safecrypt.services.MediaEncryptionService
import com.penguinstudio.safecrypt.utilities.CollectionResponse
import com.penguinstudio.safecrypt.utilities.EncryptionResource
import com.penguinstudio.safecrypt.utilities.Resource
import com.penguinstudio.safecrypt.utilities.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

interface ISelectionViewModel<T> {
    var itemSelectionMode: Boolean

    fun clearSelections()

    val selectedItems: MutableList<T>

    fun addMediaToSelection(position: Int, media: T)

    fun addAllMediaToSelection(media: ArrayList<T>)

    fun removeMediaFromSelection(position: Int, media: T)
}

interface IPicturesViewModel : ISelectionViewModel<MediaModel> {
    fun getMedia()

    fun setSelectedMedia(selectedMedia: MediaModel)

    val albums: LiveData<Resource<CollectionResponse<AlbumModel>>>

    val selectedAlbum: LiveData<Resource<AlbumModel>>

    fun setSelectedAlbum(selectedItem: AlbumModel)

    fun clearSelectedAlbum()

    fun encryptSelectedMedia()

    val encryptionStatus: LiveData<EncryptionResource?>

    fun clearEncryptionStatus()
}

interface ISelectedMediaViewModel {
    val selectedMedia: MediaModel?

    fun setSelectedMedia(selectedMedia: MediaModel)

    fun clearSelectedMedia()
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val mediaEncryptionService: MediaEncryptionService
) :
    ViewModel(), IPicturesViewModel, ISelectedMediaViewModel {

    /**
     * Get media from Phone media folders
     */
    private val _albums = MutableLiveData<Resource<CollectionResponse<AlbumModel>>>()
    override val albums: LiveData<Resource<CollectionResponse<AlbumModel>>> = _albums


    override fun getMedia() {
        viewModelScope.launch {

            // Use same value or null while loading to prevent observers from catching nulls
            _albums.postValue(Resource.loading(_albums.value?.data))

            mediaRepository.getMedia().let {
                _albums.postValue(it)
            }
        }
    }

    /**
     * Encryption
     */
    private val _encryptionStatus = MutableLiveData<EncryptionResource?>()
    override val encryptionStatus: LiveData<EncryptionResource?>
        get() {
            return _encryptionStatus
        }
    override fun clearEncryptionStatus() {
        _encryptionStatus.postValue(null)
    }


    override fun encryptSelectedMedia() {
        viewModelScope.launch {

            _encryptionStatus.postValue(EncryptionResource.loading())

            mediaRepository.encryptSelectedMedia(selectedItems.toList()).let {
                _encryptionStatus.postValue(it)
            }
        }
    }

    /**
     * Pictures fragment data
     */
    override val selectedAlbum: LiveData<Resource<AlbumModel>> = Transformations.map(_albums) {
        _selectedAlbumName ?: return@map null

        return@map when(_albums.value?.status) {
            Status.SUCCESS -> {
                val result = it.data?.collection?.find { album ->
                    album.name == _selectedAlbumName
                }
                Resource.success(result)
            }
            Status.LOADING -> {
                Resource.loading(null)
            }
            null, Status.ERROR -> {
                Resource.error("An unknown error occurred", null)
            }
        }
    }

    private var _selectedAlbumName: String? = null

    override fun setSelectedAlbum(selectedItem: AlbumModel) {
        _selectedAlbumName = selectedItem.name
    }

    override fun clearSelectedAlbum() {
        _selectedAlbumName = null
    }

    override var itemSelectionMode = false
    override val selectedItems: MutableList<MediaModel> = mutableListOf()


    override fun clearSelections() {
        selectedItems.clear()
    }

    override fun addMediaToSelection(position: Int, media: MediaModel) {
        media.isSelected = true

        selectedItems.add(media)
    }

    override fun addAllMediaToSelection(media: ArrayList<MediaModel>) {
        selectedItems.let {
            for(obj in media) {
                if(!it.contains(obj)) {
                    it.add(obj)
                    obj.isSelected = true
                }
            }
        }
    }

    override fun removeMediaFromSelection(position: Int, media: MediaModel) {
        selectedItems.removeAll {
            it.id == media.id
        }
        media.isSelected = false
    }

    /**
     * Selected Media Implementation
     */
    private var _selectedMedia: MediaModel? = null
    override val selectedMedia: MediaModel?
        get() {
            return _selectedMedia
        }

    override fun setSelectedMedia(selectedMedia: MediaModel) {
        this._selectedMedia = selectedMedia
    }

    override fun clearSelectedMedia() {
        _selectedMedia = null
    }


    companion object {
        /**
         * Re-assigns value to itself to trigger observers
         */
        fun <T> MutableLiveData<T>.notifyObserver() {
            this.value = this.value
        }
    }
}