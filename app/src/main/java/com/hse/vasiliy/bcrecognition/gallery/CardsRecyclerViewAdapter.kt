package com.hse.vasiliy.bcrecognition.gallery

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker


import com.hse.vasiliy.bcrecognition.gallery.CardGalleryFragment.OnListFragmentInteractionListener
import com.hse.vasiliy.bcrecognition.gallery.CardGalleryContent.CardContactItem
import com.hse.vasiliy.bcrecognition.R

import kotlinx.android.synthetic.main.card_contact_representation.view.*


class CardsRecyclerViewAdapter(
    private val mValues: MutableList<CardContactItem>,
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<CardsRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener
    var tracker: SelectionTracker<Long>? = null

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as Int
            mListener?.onListFragmentInteraction(item)
        }
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_contact_representation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.contactTitleView.text = item.getName()
        holder.cardContactImage.setImageBitmap(item.image)

        tracker?.let {
            holder.setActivatedState(it.isSelected(position.toLong()))

            it.addObserver(object : SelectionTracker.SelectionObserver<Any>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    mListener?.onSelectedItemsChanged(it, this@CardsRecyclerViewAdapter)
                }
            })
        }

        with(holder.mView) {
            tag = position
            setOnClickListener(mOnClickListener)
        }
    }

    fun removeAllSelected(){
        for (i in mValues.size - 1 downTo 0) {
            if (tracker?.isSelected(i.toLong())!!) {
                CardGalleryContent.eraseFromMemory(i)
                mValues.removeAt(i)
                notifyItemRemoved(i)
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int = mValues.size

    override fun getItemId(position: Int): Long = position.toLong()

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val contactTitleView: TextView = mView.card_title
        val cardContactImage: ImageView = mView.card_image

        fun setActivatedState(isActivated: Boolean = false) {
            if(isActivated) {
                mView.card_main_layout.background = ColorDrawable(
                   getColor(mView.context, R.color.secondaryColor)
                )
            } else {
                // Reset color to white if not selected
                mView.card_main_layout.background = ColorDrawable(Color.WHITE)
            }
            itemView.isActivated = isActivated
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = adapterPosition
                override fun getSelectionKey(): Long? = itemId
            }
    }
}
