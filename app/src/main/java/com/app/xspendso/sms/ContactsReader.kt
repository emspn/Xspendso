package com.app.xspendso.sms

import android.content.Context
import android.provider.ContactsContract

data class PhoneContact(
    val id: String,
    val name: String,
    val phone: String,
    val photoUri: String? = null
)

class ContactsReader(private val context: Context) {
    fun fetchContacts(): List<PhoneContact> {
        val contacts = mutableListOf<PhoneContact>()
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val phone = it.getString(phoneIndex)
                val photoUri = it.getString(photoIndex)
                val id = it.getString(idIndex)

                if (name != null && phone != null) {
                    contacts.add(PhoneContact(id, name, phone, photoUri))
                }
            }
        }
        return contacts.distinctBy { it.phone } // Filter duplicates
    }
}
