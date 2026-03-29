package com.visionassist.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.visionassist.R
import com.visionassist.emergency.EmergencyContact
import com.visionassist.emergency.EmergencyContactManager
import com.google.android.material.appbar.MaterialToolbar
import timber.log.Timber

/**
 * Activity for managing Emergency Contacts
 */
class EmergencyContactActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EmergencyContactActivity"
    }

    private lateinit var contactManager: EmergencyContactManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EmergencyContactAdapter
    private lateinit var emptyView: View
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var progressLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_contacts)

        contactManager = EmergencyContactManager(this)

        setupUI()
        loadContacts()
    }

    private fun setupUI() {
        recyclerView = findViewById(R.id.recycler_contacts)
        emptyView = findViewById(R.id.empty_view)
        fabAdd = findViewById(R.id.fab_add_contact)
        progressLoading = findViewById(R.id.progress_loading)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        adapter = EmergencyContactAdapter(
            onContactClick = { contact -> showContactDetails(contact) },
            onContactSetPrimary = { contact -> setPrimaryContact(contact) },
            onContactDelete = { contact -> confirmDeleteContact(contact) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun loadContacts() {
        progressLoading.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
        val contacts = contactManager.getAllContacts()
        updateUI(contacts)
    }

    private fun updateUI(contacts: List<EmergencyContact>) {
        progressLoading.visibility = View.GONE
        adapter.submitList(contacts)
        emptyView.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (contacts.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddContactDialog(existingContact: EmergencyContact? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.input_contact_name)
        val phoneInput = dialogView.findViewById<TextInputEditText>(R.id.input_phone_number)
        val isPrimaryCheck = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkbox_primary)

        // Pre-fill if editing
        existingContact?.let {
            nameInput.setText(it.name)
            phoneInput.setText(it.phoneNumber)
            isPrimaryCheck.isChecked = it.isPrimary
        }

        val title = if (existingContact != null) "编辑联系人" else "添加联系人"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val name = nameInput.text?.toString()?.trim() ?: ""
                val phone = phoneInput.text?.toString()?.trim() ?: ""
                val isPrimary = isPrimaryCheck.isChecked

                when {
                    name.isEmpty() -> {
                        Toast.makeText(this, "请输入姓名", Toast.LENGTH_SHORT).show()
                    }
                    phone.isEmpty() -> {
                        Toast.makeText(this, "请输入电话号码", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        if (existingContact != null) {
                            updateContact(existingContact.copy(
                                name = name,
                                phoneNumber = phone,
                                isPrimary = isPrimary
                            ))
                        } else {
                            addContact(name, phone, isPrimary)
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addContact(name: String, phoneNumber: String, isPrimary: Boolean) {
        val id = contactManager.addContact(name, phoneNumber, isPrimary)
        if (id > 0) {
            Toast.makeText(this, "联系人已添加", Toast.LENGTH_SHORT).show()
            loadContacts()
        } else {
            Toast.makeText(this, "添加失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateContact(contact: EmergencyContact) {
        if (contactManager.updateContact(contact)) {
            Toast.makeText(this, "联系人已更新", Toast.LENGTH_SHORT).show()
            loadContacts()
        } else {
            Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showContactDetails(contact: EmergencyContact) {
        val type = when {
            contact.isEmergencyNumber() -> "紧急号码"
            contact.isPrimary -> "主要联系人"
            else -> "普通联系人"
        }

        val message = buildString {
            append("姓名: ${contact.name}\n")
            append("电话: ${contact.phoneNumber}\n")
            append("类型: $type")
        }

        AlertDialog.Builder(this)
            .setTitle(contact.name)
            .setMessage(message)
            .setNeutralButton("编辑") { _, _ ->
                showAddContactDialog(contact)
            }
            .setPositiveButton("确定", null)
            .show()
    }

    private fun setPrimaryContact(contact: EmergencyContact) {
        if (contactManager.updateContact(contact.copy(isPrimary = true))) {
            Toast.makeText(this, "已设置为主要联系人", Toast.LENGTH_SHORT).show()
            loadContacts()
        }
    }

    private fun confirmDeleteContact(contact: EmergencyContact) {
        AlertDialog.Builder(this)
            .setTitle("删除联系人")
            .setMessage("确定要删除 \"${contact.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                if (contactManager.deleteContact(contact.id)) {
                    Toast.makeText(this, "联系人已删除", Toast.LENGTH_SHORT).show()
                    loadContacts()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

/**
 * RecyclerView adapter for emergency contacts list
 */
class EmergencyContactAdapter(
    private val onContactClick: (EmergencyContact) -> Unit,
    private val onContactSetPrimary: (EmergencyContact) -> Unit,
    private val onContactDelete: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder>() {

    private var contacts: List<EmergencyContact> = emptyList()

    fun submitList(newContacts: List<EmergencyContact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: android.widget.TextView = itemView.findViewById(R.id.text_contact_name)
        private val textPhone: android.widget.TextView = itemView.findViewById(R.id.text_phone_number)
        private val badgePrimary: android.widget.TextView = itemView.findViewById(R.id.badge_primary)
        private val badgeEmergency: android.widget.TextView = itemView.findViewById(R.id.badge_emergency)
        private val btnSetPrimary: android.widget.ImageButton = itemView.findViewById(R.id.btn_set_primary)
        private val btnDelete: android.widget.ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(contact: EmergencyContact) {
            textName.text = contact.name
            textPhone.text = contact.phoneNumber

            badgePrimary.visibility = if (contact.isPrimary) View.VISIBLE else View.GONE
            badgeEmergency.visibility = if (contact.isEmergencyNumber()) View.VISIBLE else View.GONE

            btnSetPrimary.visibility = if (contact.isPrimary) View.GONE else View.VISIBLE

            itemView.setOnClickListener { onContactClick(contact) }
            btnSetPrimary.setOnClickListener { onContactSetPrimary(contact) }
            btnDelete.setOnClickListener { onContactDelete(contact) }
        }
    }
}
