package com.hse.vasiliy.bcrecognition

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView


import com.hse.vasiliy.bcrecognition.CardGalleryFragment.OnListFragmentInteractionListener
import com.hse.vasiliy.bcrecognition.CardGalleryContent.CardContactItem

import kotlinx.android.synthetic.main.card_contact_representation.view.*

class CardsRecyclerViewAdapter(
    private val mValues: List<CardContactItem>,
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<CardsRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as CardContactItem
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_contact_representation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.contactTitleView.text = item.content
        holder.cardContactImage.setImageBitmap(item.image)

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val contactTitleView: TextView = mView.card_title
        val cardContactImage: ImageView = mView.card_image
    }
}
