package com.simplemobiletools.contacts.activities

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CONTACTS
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.CONTACT_ID
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.activity_contact.*
import kotlinx.android.synthetic.main.item_email.view.*
import kotlinx.android.synthetic.main.item_phone_number.view.*

class ContactActivity : SimpleActivity() {
    private var wasActivityInitialized = false
    private var contact: Contact? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_cross)

        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    if (it) {
                        initContact()
                    } else {
                        toast(R.string.no_contacts_permission)
                        finish()
                    }
                }
            } else {
                toast(R.string.no_contacts_permission)
                finish()
            }
        }
    }

    private fun initContact() {
        val contactId = intent.getIntExtra(CONTACT_ID, 0)
        if (contactId != 0) {
            contact = ContactsHelper(this).getContactWithId(contactId)
            if (contact == null) {
                toast(R.string.unknown_error_occurred)
                finish()
                return
            }
        }

        if (contact == null) {
            setupNewContact()
        } else {
            setupEditContact()
        }

        contact_send_sms.beVisibleIf(contact!!.phoneNumbers.isNotEmpty())
        contact_start_call.beVisibleIf(contact!!.phoneNumbers.isNotEmpty())
        contact_send_email.beVisibleIf(contact!!.emails.isNotEmpty())

        contact_photo.background = ColorDrawable(config.primaryColor)

        if (contact!!.photoUri.isEmpty()) {
            applyPhotoPlaceholder()
        } else {
            val options = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .centerCrop()

            Glide.with(this)
                    .load(contact!!.photoUri)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(options)
                    .listener(object : RequestListener<Drawable> {
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean) = false

                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            applyPhotoPlaceholder()
                            return true
                        }
                    }).into(contact_photo)
        }

        val textColor = config.textColor
        contact_send_sms.applyColorFilter(textColor)
        contact_start_call.applyColorFilter(textColor)
        contact_send_email.applyColorFilter(textColor)
        contact_name_image.applyColorFilter(textColor)
        contact_number_image.applyColorFilter(textColor)
        contact_email_image.applyColorFilter(textColor)
        contact_source_image.applyColorFilter(textColor)
        contact_number_add_new.applyColorFilter(config.primaryColor)
        contact_number_add_new.background.applyColorFilter(textColor)
        contact_email_add_new.applyColorFilter(config.primaryColor)
        contact_email_add_new.background.applyColorFilter(textColor)

        contact_photo.setOnClickListener { }
        //contact_send_sms.setOnClickListener { sendSMSIntent(contact!!.number) }
        //contact_start_call.setOnClickListener { startCallIntent(contact!!.number) }
        //contact_send_email.setOnClickListener { sendEmailIntent(contact!!.email) }
        contact_source.setOnClickListener { showAccountSourcePicker() }
        contact_number_add_new.setOnClickListener { addNewPhoneNumberField() }
        contact_email_add_new.setOnClickListener { addNewEmailField() }

        updateTextColors(contact_scrollview)
        wasActivityInitialized = true
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_contact, menu)
        if (wasActivityInitialized) {
            menu.findItem(R.id.delete).isVisible = contact?.id != 0
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save -> saveContact()
            R.id.delete -> deleteContact()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setupEditContact() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        supportActionBar?.title = resources.getString(R.string.edit_contact)
        contact_first_name.setText(contact!!.firstName)
        contact_middle_name.setText(contact!!.middleName)
        contact_surname.setText(contact!!.surname)
        contact_source.text = contact!!.source

        contact!!.phoneNumbers.forEachIndexed { index, number ->
            var numberHolder = contact_numbers_holder.getChildAt(index)
            if (numberHolder == null) {
                numberHolder = layoutInflater.inflate(R.layout.item_phone_number, contact_numbers_holder, false)
                contact_numbers_holder.addView(numberHolder)
            }

            (numberHolder as? ViewGroup)?.apply {
                contact_number.setText(number.value)
                contact_number_type.setText(number.getTextId())
            }
        }

        contact!!.emails.forEachIndexed { index, email ->
            var emailHolder = contact_emails_holder.getChildAt(index)
            if (emailHolder == null) {
                emailHolder = layoutInflater.inflate(R.layout.item_email, contact_emails_holder, false)
                contact_emails_holder.addView(emailHolder)
            }

            (emailHolder as? ViewGroup)?.apply {
                contact_email.setText(email.value)
                contact_email_type.setText(email.getTextId())
            }
        }
    }

    private fun setupNewContact() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        supportActionBar?.title = resources.getString(R.string.new_contact)
        contact = Contact(0, "", "", "", "", ArrayList(), ArrayList(), "")
    }

    private fun applyPhotoPlaceholder() {
        val placeholder = resources.getColoredBitmap(R.drawable.ic_person, config.primaryColor.getContrastColor())
        val padding = resources.getDimension(R.dimen.activity_margin).toInt()
        contact_photo.setPadding(padding, padding, padding, padding)
        contact_photo.setImageBitmap(placeholder)
    }

    private fun saveContact() {
        contact!!.apply {
            firstName = contact_first_name.value
            middleName = contact_middle_name.value
            surname = contact_surname.value
            //number = contact_number.value
            //email = contact_email.value

            if (ContactsHelper(this@ContactActivity).updateContact(this)) {
                finish()
            }
        }
    }

    private fun addNewPhoneNumberField() {

    }

    private fun addNewEmailField() {

    }

    private fun deleteContact() {
        ConfirmationDialog(this) {
            ContactsHelper(this).deleteContact(contact!!)
            finish()
        }
    }

    private fun showAccountSourcePicker() {

    }
}