package com.hse.vasiliy.bcrecognition

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import java.io.File
import java.util.*
import com.google.gson.GsonBuilder
import java.io.FileWriter
import java.io.FileOutputStream
import java.io.FileReader
import android.graphics.BitmapFactory


object CardGalleryContent {

    private var applicationTag = "CardGalleryContent"
    private val galleryMetadataPath = Environment.getExternalStorageDirectory().toString() + CARD_GALLERY_PATH + CARD_GALLERY_PATH_METADATA
    private val galleryImagesPath = Environment.getExternalStorageDirectory().toString() + CARD_GALLERY_PATH + CARD_GALLERY_PATH_IMAGES

    val ITEMS: MutableList<CardContactItem> = ArrayList()

    init {
        try {
            val metadataDir = File(galleryMetadataPath)
            val galleryImagesDir = File(galleryImagesPath)
            if (!metadataDir.exists() || !galleryImagesDir.exists()) {
                if (!metadataDir.mkdirs() || !galleryImagesDir.mkdirs()) {
                    throw Exception("Cannot create gallery directory")
                }
            }
            val files = metadataDir.listFiles()
            for (file in files) {
                val reader = FileReader(file)
                val dataItem = GsonBuilder().create().fromJson(reader, ParcelableJsonItem::class.java)
                reader.close()
                val imagePath = galleryImagesPath + "/" + dataItem.uniqueID + ".png"
                if (File(imagePath).exists()) {
                    try {
                        val options = BitmapFactory.Options()
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888
                        val bitmap = BitmapFactory.decodeFile(imagePath, options)
                        val item = CardContactItem(bitmap)
                        item.data = dataItem
                        ITEMS.add(item)
                    } catch (exc: Exception) {
                        continue
                    }
                } else {
                    continue
                }
            }
        } catch (exc: Exception) {
            Log.e(applicationTag, exc.toString())
        }
    }

    fun addItem(item : CardContactItem) {
        ITEMS.add(item)
        saveToMemory(item)
    }

    private fun saveToMemory(item: CardContactItem) {
        val metadataFilePath = galleryMetadataPath + "/" + item.uniqueID + ".json"
        FileWriter(metadataFilePath).use { writer ->
            val gson = GsonBuilder().create()
            gson.toJson(item.data, writer)
            writer.close()
        }
        val imageFilePath = galleryImagesPath + "/" + item.uniqueID + ".png"
        val imageFile = File(imageFilePath)
        if (imageFile.createNewFile()) {
            val out = FileOutputStream(imageFile)
            item.image.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        }
    }

    class CardContactItem(@Transient var image: Bitmap) {
        val uniqueID = UUID.randomUUID().toString()
        var data = ParcelableJsonItem(uniqueID, "<NO_NAME>")

        fun setName(name: String): CardContactItem {
            data.name = name
            return this
        }

        fun setCompany(company: String): CardContactItem {
            data.company = company
            return this
        }

        fun setPhone(phone: String): CardContactItem {
            data.phone = phone
            return this
        }

        fun setEmail(email: String): CardContactItem {
            data.email = email
            return this
        }

        fun setAddress(address: String): CardContactItem {
            data.address = address
            return this
        }

        fun getName(): String {
            return data.name
        }
    }


    data class ParcelableJsonItem(val uniqueID: String,
                                  var name: String = "",
                                  var company: String = "",
                                  var phone: String = "",
                                  var email: String = "",
                                  var address: String = "")
}
