package com.example.photogalleryapp.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.photogalleryapp.R
import com.example.photogalleryapp.databinding.FragmentHomeBinding
import com.example.photogalleryapp.model.FlickrPhoto
import com.example.photogalleryapp.model.FlickrPhotos
import com.example.photogalleryapp.model.FlickrResponse
import com.example.photogalleryapp.viewmodel.GalleryViewModel

class HomeFragment : Fragment() {

    private lateinit var viewModel: GalleryViewModel
    private lateinit var flickrResponse: MutableLiveData<FlickrResponse>
    private lateinit var error: MutableLiveData<Boolean>
    private lateinit var PAGE_NO: MutableLiveData<Int>
    private val staggeredRecyclerViewAdapter = StaggeredRecyclerViewAdapter(arrayListOf())
    private val NUM_COLUMNS = 2

    private lateinit var manager: StaggeredGridLayoutManager
    private var isScrolling = false

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG = HomeFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(GalleryViewModel::class.java)
        flickrResponse = viewModel.getFlickrResponse()
        error = viewModel.error
        PAGE_NO = viewModel.PAGE_NO

        observeViewModel()

        manager = StaggeredGridLayoutManager(NUM_COLUMNS, LinearLayout.VERTICAL)
        _binding!!.recyclerView.apply {
            layoutManager = manager
            adapter = staggeredRecyclerViewAdapter
        }

        _binding?.recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrolling = true
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val totalItems = manager.itemCount
                val currentItems = manager.childCount
                val scrollOutItems = manager.findFirstVisibleItemPositions(null)

                if (isScrolling && (scrollOutItems[0] + currentItems >= totalItems)) {
                    isScrolling = false
                    Log.i(TAG, "$totalItems $currentItems (${scrollOutItems[0]}, ${scrollOutItems[1]})")
                    viewModel.loadNextPage()
                }
            }
        })


        _binding!!.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observeViewModel() {
        flickrResponse.observe(viewLifecycleOwner, Observer { flickrResponse ->
            flickrResponse?.let {
                val flickrPhotos: FlickrPhotos = it.photos!!
                val listFlickrPhotos: List<FlickrPhoto> = flickrPhotos.photo!!
                staggeredRecyclerViewAdapter.updatePhoto(listFlickrPhotos, PAGE_NO.value!!)
                _binding!!.swipeRefreshLayout.isRefreshing = false
                for (i in listFlickrPhotos) {
                    Log.i(TAG, "${i.url}")
                }
            }
        })

        error.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (it)
                    _binding!!.swipeRefreshLayout.isRefreshing = false
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (it) {
                    _binding!!.progressBar.visibility = View.VISIBLE
                } else {
                    _binding!!.progressBar.visibility = View.GONE
                }
            }
        })
    }
}
