package com.app.xspendso.ui.people

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.xspendso.data.*
import com.app.xspendso.domain.TransactionRepository
import com.app.xspendso.sms.ContactsReader
import com.app.xspendso.sms.PhoneContact
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

enum class PeopleSortOrder {
    RECENT, NAME, BALANCE
}

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val peopleDao: PeopleDao,
    private val transactionRepository: TransactionRepository,
    private val prefsManager: PrefsManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val contactsReader = ContactsReader(context)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(PeopleSortOrder.RECENT)
    val sortOrder: StateFlow<PeopleSortOrder> = _sortOrder.asStateFlow()

    private val _contactPermissionGranted = MutableStateFlow(false)
    val contactPermissionGranted: StateFlow<Boolean> = _contactPermissionGranted.asStateFlow()

    private val _phoneContacts = MutableStateFlow<List<PhoneContact>>(emptyList())
    val phoneContacts: StateFlow<List<PhoneContact>> = _phoneContacts.asStateFlow()

    // Debounced search for better performance on large contact lists
    private val debouncedSearchQuery = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val allContacts: StateFlow<List<ContactLedger>> = combine(
        peopleDao.getAllContacts(),
        debouncedSearchQuery,
        _sortOrder
    ) { contacts, query, order ->
        val filtered = if (query.isBlank()) contacts
        else contacts.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.phone.contains(query) 
        }
        
        when (order) {
            PeopleSortOrder.RECENT -> filtered.sortedByDescending { it.lastUpdated }
            PeopleSortOrder.NAME -> filtered.sortedBy { it.name }
            PeopleSortOrder.BALANCE -> filtered.sortedByDescending { abs(it.netBalance) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalToReceive: StateFlow<Double> = peopleDao.getAllContacts().map { contacts ->
        contacts.filter { it.netBalance > 0 }.sumOf { it.netBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalToPay: StateFlow<Double> = peopleDao.getAllContacts().map { contacts ->
        contacts.filter { it.netBalance < 0 }.sumOf { abs(it.netBalance) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortOrderChange(order: PeopleSortOrder) {
        _sortOrder.value = order
    }

    fun setContactPermissionGranted(granted: Boolean) {
        _contactPermissionGranted.value = granted
        if (granted) {
            refreshPhoneContacts()
        }
    }

    fun refreshPhoneContacts() {
        if (!_contactPermissionGranted.value) return
        
        viewModelScope.launch {
            val fetched = withContext(Dispatchers.IO) {
                contactsReader.fetchContacts()
            }
            _phoneContacts.value = fetched
            syncContactDetails(fetched)
        }
    }

    private suspend fun syncContactDetails(fetched: List<PhoneContact>) {
        withContext(Dispatchers.IO) {
            val stored = peopleDao.getAllContacts().first()
            stored.forEach { ledger ->
                val match = fetched.find { it.phone.replace(" ", "") == ledger.phone.replace(" ", "") }
                if (match != null) {
                    if (match.name != ledger.name || match.photoUri != ledger.photoUri) {
                        peopleDao.updateContact(ledger.copy(name = match.name, photoUri = match.photoUri))
                    }
                }
            }
        }
    }

    fun addContactFromPhone(name: String, phone: String, photoUri: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = peopleDao.getContactByPhone(phone)
                if (existing == null) {
                    peopleDao.insertContact(ContactLedger(name = name, phone = phone, photoUri = photoUri))
                } else {
                    if (existing.photoUri != photoUri) {
                        peopleDao.updateContact(existing.copy(photoUri = photoUri))
                    }
                }
            }
        }
    }

    fun addManualContact(name: String, phone: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = peopleDao.getContactByPhone(phone)
                if (existing == null) {
                    peopleDao.insertContact(ContactLedger(name = name, phone = phone))
                }
            }
        }
    }

    fun updateContactUpi(contactId: Long, upiId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val contact = peopleDao.getContactById(contactId)
                if (contact != null) {
                    peopleDao.updateContact(contact.copy(upiId = upiId.trim()))
                }
            }
        }
    }

    fun updateContactName(contactId: Long, newName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val contact = peopleDao.getContactById(contactId)
                if (contact != null) {
                    peopleDao.updateContact(contact.copy(name = newName))
                }
            }
        }
    }

    fun addTransaction(contactId: Long, amount: Double, type: LoanType, remark: String?, date: Long) {
        if (amount <= 0) return
        
        viewModelScope.launch {
            val transaction = LoanTransaction(
                contactId = contactId,
                amount = String.format("%.2f", amount).toDouble(),
                type = type,
                date = date,
                remark = remark
            )
            withContext(Dispatchers.IO) {
                peopleDao.addLoanTransactionAndUpdateBalance(transaction)
            }
        }
    }

    fun updateTransaction(transaction: LoanTransaction) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                peopleDao.updateLoanTransactionAndUpdateBalance(transaction)
            }
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                peopleDao.deleteLoanTransactionAndUpdateBalance(transactionId)
            }
        }
    }

    fun deleteMultipleTransactions(contactId: Long, transactionIds: List<Long>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                peopleDao.deleteMultipleTransactionsAndUpdateBalance(contactId, transactionIds)
            }
        }
    }

    fun toggleSettlement(transactionId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                peopleDao.toggleTransactionSettlement(transactionId)
            }
        }
    }

    fun settlePartialAmount(contactId: Long, amount: Double, type: LoanType) {
        if (amount <= 0) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                peopleDao.settleLoanTransaction(contactId, amount, type)
            }
        }
    }

    fun getTransactionsForContact(contactId: Long): Flow<List<LoanTransaction>> {
        return peopleDao.getTransactionsByContactId(contactId)
    }

    fun deleteContact(contact: ContactLedger) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                peopleDao.deleteContact(contact)
            }
        }
    }

    fun copyToClipboard(text: String, label: String = "UPI ID") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun initiateUpiPayment(contact: ContactLedger) {
        val amount = abs(contact.netBalance)
        if (amount <= 0) return
        
        if (contact.netBalance < 0) {
            val recipientUpi = contact.upiId?.trim() ?: run {
                Toast.makeText(context, "Add a UPI ID for ${contact.name} to continue", Toast.LENGTH_LONG).show()
                return
            }

            // AUTO-COPY: Fallback for security warnings
            copyToClipboard(recipientUpi)

            val uriString = "upi://pay?pa=$recipientUpi&pn=${Uri.encode(contact.name)}&am=${String.format(Locale.US, "%.2f", amount)}&cu=INR"

            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Toast.makeText(context, "UPI ID copied! Paste it if payment is blocked.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                try {
                    val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(uriString)), "Pay with")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } catch (e2: Exception) {
                    shareViaWhatsApp(contact)
                }
            }
        } else {
            shareRequestLink(contact)
        }
    }

    private fun shareRequestLink(contact: ContactLedger) {
        val amount = abs(contact.netBalance)
        val userUpi = prefsManager.userUpiId?.trim()
        val userName = prefsManager.userName ?: "Xpendso User"
        
        if (userUpi.isNullOrBlank()) {
            Toast.makeText(context, "Set your UPI ID in Settings to send payment requests", Toast.LENGTH_LONG).show()
            shareViaWhatsApp(contact)
            return
        }

        val upiUri = "upi://pay?pa=$userUpi&pn=${Uri.encode(userName)}&am=${String.format(Locale.US, "%.2f", amount)}&cu=INR"

        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val amountStr = currencyFormatter.format(amount)
        val message = "Hi ${contact.name}, please settle the dues of $amountStr. You can pay me by clicking this link: $upiUri"

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val cleanPhone = contact.phone.filter { it.isDigit() }
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
            intent.data = Uri.parse(url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            shareReminder(contact)
        }
    }

    fun shareViaWhatsApp(contact: ContactLedger) {
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val amountStr = currencyFormatter.format(abs(contact.netBalance))
        val message = if (contact.netBalance > 0) {
            "Hi ${contact.name}, just a friendly reminder regarding the pending amount of $amountStr owed to me. Thanks! - Shared via Xpendso"
        } else {
            "Hi ${contact.name}, I've recorded a payment of $amountStr that I owe you in my Xpendso ledger. I'll settle it soon! Thanks."
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val cleanPhone = contact.phone.filter { it.isDigit() }
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
            intent.data = Uri.parse(url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            shareReminder(contact)
        }
    }

    fun shareReminder(contact: ContactLedger) {
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val amountStr = currencyFormatter.format(abs(contact.netBalance))
        
        val message = if (contact.netBalance > 0) {
            "Hi ${contact.name}, just a friendly reminder regarding the pending amount of $amountStr owed to me. Thanks! - Shared via Xspendso"
        } else {
            "Hi ${contact.name}, I've recorded $amountStr that I owe you. Thanks."
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Reminder")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun exportContactLedger(contact: ContactLedger, transactions: List<LoanTransaction>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                
                val csvHeader = "Date,Type,Amount,Status,Notes\n"
                val csvRows = transactions.joinToString("\n") { tx ->
                    "${dateFormat.format(Date(tx.date))},${if (tx.type == LoanType.LENT) "Money Given" else "Money Taken"},${tx.amount},${if (tx.isSettled) "Settled" else "Pending"},${tx.remark ?: ""}"
                }
                val content = "Ledger Report for ${contact.name}\nPhone: ${contact.phone}\nNet Balance: ${currencyFormatter.format(contact.netBalance)}\n\n$csvHeader$csvRows"
                
                val fileName = "Ledger_${contact.name.replace(" ", "_")}.csv"
                val file = File(context.getExternalFilesDir(null), fileName)
                
                try {
                    FileOutputStream(file).use { it.write(content.toByteArray()) }
                    shareFile(file)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val shareIntent = Intent.createChooser(intent, "Share Ledger Report")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun scanAndImportP2PTransactions(contact: ContactLedger) {
        viewModelScope.launch {
            val allTransactions = transactionRepository.getAllTransactions().first()
            val existingLoanTxs = peopleDao.getTransactionsByContactId(contact.contactId).first()
            
            withContext(Dispatchers.IO) {
                allTransactions.forEach { tx ->
                    val counterparty = tx.counterparty
                    if (counterparty.contains(contact.name, ignoreCase = true) || contact.name.contains(counterparty, ignoreCase = true)) {
                        val alreadyImported = existingLoanTxs.any { 
                            it.amount == tx.amount && abs(it.date - tx.timestamp) < 60000 
                        }
                        
                        if (!alreadyImported) {
                            val type = if (tx.type == "DEBIT") LoanType.BORROWED else LoanType.LENT
                            peopleDao.addLoanTransactionAndUpdateBalance(
                                LoanTransaction(
                                    contactId = contact.contactId,
                                    amount = tx.amount,
                                    type = type,
                                    date = tx.timestamp,
                                    remark = "Auto-imported: ${tx.remark ?: tx.counterparty}"
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
