package com.hse.vasiliy.bcrecognition.gallery

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy

import com.hse.vasiliy.bcrecognition.gallery.CardGalleryContent.CardContactItem
import com.hse.vasiliy.bcrecognition.R

class CardGalleryFragment : Fragment() {

    private var listener: OnListFragmentInteractionListener? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.card_list_fragment, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter =
                    CardsRecyclerViewAdapter(CardGalleryContent.ITEMS, listener)
                (adapter as CardsRecyclerViewAdapter).tracker = setupTracker(view)
            }
        }
        return view
    }

    private fun setupTracker(view: RecyclerView) : SelectionTracker<Long> {
        return SelectionTracker.Builder<Long>(
            "CardSelection",
            view,
            StableIdKeyProvider(view),
            CardLookup(view),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: CardContactItem?)
        fun onSelectedItemsChanged(tracker: SelectionTracker<Long>)
    }
}
