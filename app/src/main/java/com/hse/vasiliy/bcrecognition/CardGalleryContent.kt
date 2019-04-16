package com.hse.vasiliy.bcrecognition

import android.graphics.Bitmap
import android.util.Log
import java.util.ArrayList
import java.util.HashMap

object CardGalleryContent {

    val ITEMS: MutableList<CardContactItem> = ArrayList()
    val ITEM_MAP: MutableMap<String, CardContactItem> = HashMap()

    init {
        // Add some sample items
        createDummyItem(1)
    }

    private fun addItem(item: CardContactItem) {
        ITEMS.add(item)
        ITEM_MAP[item.id] = item
    }

    private fun createDummyItem(position: Int): CardContactItem {
        return CardContactItem(
            position.toString(),
            "Item $position",
            Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        )
    }

    data class CardContactItem(val id: String, val content: String, val image: Bitmap) {
        override fun toString(): String = content
    }
}
