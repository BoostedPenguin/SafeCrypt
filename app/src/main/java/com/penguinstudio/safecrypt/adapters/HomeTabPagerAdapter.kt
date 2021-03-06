package com.penguinstudio.safecrypt.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.penguinstudio.safecrypt.ui.home.GalleryFragment
import com.penguinstudio.safecrypt.ui.home.encrypted.EncryptedMediaFragment


class HomeTabPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return if(position == 0) {
            GalleryFragment()
        } else {
            EncryptedMediaFragment()
        }
    }
}