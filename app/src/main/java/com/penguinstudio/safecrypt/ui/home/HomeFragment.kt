package com.penguinstudio.safecrypt.ui.home

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.penguinstudio.safecrypt.R
import com.penguinstudio.safecrypt.adapters.HomeTabPagerAdapter
import com.penguinstudio.safecrypt.ui.home.encrypted.EncryptedMediaViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(), LifecycleObserver {
    private lateinit var pagerAdapter: HomeTabPagerAdapter
    private lateinit var viewPager: ViewPager2
    private val encryptedViewModel: EncryptedMediaViewModel by activityViewModels()
    private val galleryViewModel: GalleryViewModel by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    // Handle the back button event
                    activity?.finishAndRemoveTask()
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    /**
     * After activity onCreate is completed
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreated(){
        (activity as AppCompatActivity).supportActionBar?.show()
        activity?.lifecycle?.removeObserver(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.lifecycle?.addObserver(this)
    }

    override fun onDetach() {
        super.onDetach()
        activity?.lifecycle?.removeObserver(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment


        return inflater.inflate(R.layout.fragment_home, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.show()

        val adRequest = AdRequest.Builder().build()
        requireView().findViewById<AdView>(R.id.adViewHome).loadAd(adRequest)

        pagerAdapter = HomeTabPagerAdapter(this)
        viewPager = view.findViewById(R.id.pager)

        // Disable user-swipe
        viewPager.isUserInputEnabled = false;
        viewPager.adapter = pagerAdapter

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when(position) {
                0 -> {
                    tab.icon =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_baseline_image_24)
                    tab.text = "Gallery"
                }
                1 -> {
                    tab.icon =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_baseline_lock_24)
                    tab.text = "Encrypted"
                }
            }
        }.attach()
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.app_name)
                (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

                when(position) {
                    0 -> {
                        galleryViewModel.getMedia()
                    }
                    1 -> {
                        encryptedViewModel.itemSelectionMode.postValue(false)
                    }
                }
            }
        })
    }
}