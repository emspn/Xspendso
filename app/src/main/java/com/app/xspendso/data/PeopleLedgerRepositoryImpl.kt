package com.app.xspendso.data

import android.content.Context
import android.util.Log
import com.app.xspendso.domain.DomainError
import com.app.xspendso.domain.PeopleLedgerRepository
import com.app.xspendso.domain.Resource
import com.app.xspendso.domain.SyncStatus
import com.app.xspendso.sms.PeopleSyncWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleLedgerRepositoryImpl @Inject constructor(
    private val peopleDao: PeopleDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val prefsManager: PrefsManager,
    @param:ApplicationContext private val context: Context
) : PeopleLedgerRepository {

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    override fun getSyncStatus(): Flow<SyncStatus> = _syncStatus.asStateFlow()

    override fun getAllContacts(): Flow<List<ContactLedger>> = peopleDao.getAllContacts()

    override fun getTransactionsForContact(contactId: Long): Flow<List<LoanTransaction>> =
        peopleDao.getTransactionsByContactId(contactId)

    override suspend fun addContact(name: String, phone: String, photoUri: String?): Resource<Unit> =
        handleLocalOp {
            val contact = ContactLedger(name = name, phone = phone, photoUri = photoUri)
            peopleDao.insertContact(contact)
            pushContactToCloud(contact)
            triggerBackgroundSync()
        }

    override suspend fun updateContactUpi(contactId: Long, upiId: String): Resource<Unit> =
        handleLocalOp {
            val contact = peopleDao.getContactById(contactId)
            if (contact != null) {
                val updated = contact.copy(upiId = upiId.trim(), lastUpdated = System.currentTimeMillis())
                peopleDao.updateContact(updated)
                pushContactToCloud(updated)
                triggerBackgroundSync()
            }
        }

    override suspend fun updateContactName(contactId: Long, newName: String): Resource<Unit> =
        handleLocalOp {
            val contact = peopleDao.getContactById(contactId)
            if (contact != null) {
                val updated = contact.copy(name = newName, lastUpdated = System.currentTimeMillis())
                peopleDao.updateContact(updated)
                pushContactToCloud(updated)
                triggerBackgroundSync()
            }
        }

    override suspend fun deleteContact(contact: ContactLedger): Resource<Unit> =
        handleLocalOp {
            peopleDao.deleteContact(contact)
            deleteContactFromCloud(contact.uuid)
        }

    override suspend fun addLoanTransaction(
        contactId: Long,
        amount: Double,
        type: LoanType,
        remark: String?,
        date: Long
    ): Resource<Unit> = handleLocalOp {
        val contact = peopleDao.getContactById(contactId)
        val transaction = LoanTransaction(
            contactId = contactId,
            contactUuid = contact?.uuid ?: "",
            amount = amount,
            type = type,
            date = date,
            remark = remark,
            lastUpdated = System.currentTimeMillis()
        )
        peopleDao.addLoanTransactionAndUpdateBalance(transaction)
        pushLoanToCloud(transaction)
        triggerBackgroundSync()
    }

    override suspend fun updateLoanTransaction(transaction: LoanTransaction): Resource<Unit> =
        handleLocalOp {
            val updated = transaction.copy(lastUpdated = System.currentTimeMillis())
            peopleDao.updateLoanTransactionAndUpdateBalance(updated)
            pushLoanToCloud(updated)
            triggerBackgroundSync()
        }

    override suspend fun deleteLoanTransaction(transactionId: Long): Resource<Unit> =
        handleLocalOp {
            val transaction = peopleDao.getTransactionById(transactionId)
            if (transaction != null) {
                peopleDao.deleteLoanTransactionAndUpdateBalance(transactionId)
                deleteLoanFromCloud(transaction.contactUuid, transaction.uuid)
            }
        }

    override suspend fun deleteMultipleLoanTransactions(contactId: Long, transactionIds: List<Long>): Resource<Unit> =
        handleLocalOp {
            val transactions = transactionIds.mapNotNull { peopleDao.getTransactionById(it) }
            peopleDao.deleteMultipleTransactionsAndUpdateBalance(contactId, transactionIds)
            transactions.forEach { deleteLoanFromCloud(it.contactUuid, it.uuid) }
        }

    override suspend fun toggleSettlement(transactionId: Long): Resource<Unit> =
        handleLocalOp {
            peopleDao.toggleTransactionSettlement(transactionId)
            val updated = peopleDao.getTransactionById(transactionId)
            if (updated != null) {
                pushLoanToCloud(updated)
            }
            triggerBackgroundSync()
        }

    override suspend fun settlePartialAmount(
        contactId: Long,
        amount: Double,
        type: LoanType
    ): Resource<Unit> = handleLocalOp {
        peopleDao.settleLoanTransaction(contactId, amount, type)
        // Since this creates multiple internal updates, trigger a full background sync to reconcile
        triggerBackgroundSync()
    }

    private suspend fun pushContactToCloud(contact: ContactLedger) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("people").document(contact.uuid)
                .set(contact.toFirestoreMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("PeopleLedgerRepo", "Immediate contact push failed", e)
        }
    }

    private suspend fun pushLoanToCloud(loan: LoanTransaction) {
        val userId = auth.currentUser?.uid ?: return
        if (loan.contactUuid.isBlank()) return
        try {
            firestore.collection("users").document(userId)
                .collection("people").document(loan.contactUuid)
                .collection("loans").document(loan.uuid)
                .set(loan.toFirestoreMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("PeopleLedgerRepo", "Immediate loan push failed", e)
        }
    }

    private suspend fun handleLocalOp(op: suspend () -> Unit): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            op()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("PeopleLedgerRepo", "Database operation failed", e)
            Resource.Error(DomainError.SyncError.DatabaseWriteError)
        }
    }

    private fun triggerBackgroundSync() {
        PeopleSyncWorker.scheduleSync(context)
    }

    override suspend fun syncProfile(): Resource<Unit> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext Resource.Error(DomainError.AuthError.InvalidCredentials)
        
        try {
            val profileRef = firestore.collection("users").document(userId).collection("profile").document("info")
            
            // 1. Upload local profile
            val localProfile = mapOf(
                "userName" to (prefsManager.userName ?: ""),
                "userUpiId" to (prefsManager.userUpiId ?: ""),
                "lastUpdated" to System.currentTimeMillis()
            )
            profileRef.set(localProfile, SetOptions.merge()).await()
            
            // 2. Download cloud profile (if newer or local is default)
            val cloudProfile = profileRef.get().await()
            if (cloudProfile.exists()) {
                val cloudName = cloudProfile.getString("userName")
                val cloudUpi = cloudProfile.getString("userUpiId")
                
                if (!cloudName.isNullOrBlank() && (prefsManager.userName == "User" || prefsManager.userName.isNullOrBlank())) {
                    prefsManager.userName = cloudName
                }
                if (!cloudUpi.isNullOrBlank() && prefsManager.userUpiId.isNullOrBlank()) {
                    prefsManager.userUpiId = cloudUpi
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("PeopleLedgerRepo", "Profile sync failed", e)
            Resource.Error(DomainError.SyncError.FirestoreError)
        }
    }

    override suspend fun syncWithCloud(): Resource<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            Log.e("PeopleLedgerRepo", "Sync aborted: No Firebase user")
            return@withContext Resource.Error(DomainError.AuthError.InvalidCredentials)
        }
        val userId = user.uid
        
        _syncStatus.value = SyncStatus.SYNCING
        try {
            Log.d("PeopleLedgerRepo", "Starting Sync for user: $userId")
            
            // Sync profile first
            syncProfile()
            
            // 0. Preliminary check: Ensure all local records have UUIDs
            ensureLocalUuids()
            
            // 1. Upload local changes to Firestore
            uploadLocalChanges(userId)
            
            // 2. Download cloud changes from Firestore
            downloadCloudChanges(userId)
            
            Log.d("PeopleLedgerRepo", "Sync successful")
            _syncStatus.value = SyncStatus.SUCCESS
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("PeopleLedgerRepo", "Sync failed", e)
            _syncStatus.value = SyncStatus.ERROR
            Resource.Error(DomainError.SyncError.FirestoreError)
        }
    }

    private suspend fun ensureLocalUuids() {
        val contacts = peopleDao.getAllContactsList().filter { it.uuid.isBlank() }
        contacts.forEach { contact ->
            peopleDao.updateContact(contact.copy(uuid = UUID.randomUUID().toString()))
        }
        
        val loans = peopleDao.getAllLoanTransactionsList().filter { it.uuid.isBlank() || it.contactUuid.isBlank() }
        loans.forEach { loan ->
            val contact = peopleDao.getContactById(loan.contactId)
            peopleDao.updateLoanTransaction(loan.copy(
                uuid = loan.uuid.ifBlank { UUID.randomUUID().toString() },
                contactUuid = contact?.uuid ?: ""
            ))
        }
    }

    private suspend fun uploadLocalChanges(userId: String) {
        val contacts = peopleDao.getAllContactsList()
        val userRef = firestore.collection("users").document(userId)
        
        for (contact in contacts) {
            if (contact.uuid.isBlank()) continue
            
            Log.d("PeopleLedgerRepo", "Uploading contact: ${contact.name}")
            userRef.collection("people").document(contact.uuid)
                .set(contact.toFirestoreMap(), SetOptions.merge())
                .await()
            
            val loans = peopleDao.getAllTransactionsForContactInternal(contact.contactId)
            for (loan in loans) {
                if (loan.uuid.isBlank()) continue
                userRef.collection("people").document(contact.uuid)
                    .collection("loans").document(loan.uuid)
                    .set(loan.toFirestoreMap(), SetOptions.merge())
                    .await()
            }
        }
    }

    private suspend fun downloadCloudChanges(userId: String) {
        val userRef = firestore.collection("users").document(userId)
        val cloudPeopleSnapshot = userRef.collection("people").get().await()
        
        Log.d("PeopleLedgerRepo", "Downloaded ${cloudPeopleSnapshot.size()} people from cloud")
        
        for (doc in cloudPeopleSnapshot.documents) {
            val data = doc.data ?: continue
            val uuid = doc.id
            val cloudLastUpdated = (data["lastUpdated"] as? Number)?.toLong() ?: 0L
            
            val localContact = peopleDao.getContactByUuid(uuid)
            
            // If doesn't exist locally OR cloud is newer
            if (localContact == null || cloudLastUpdated > localContact.lastUpdated) {
                val newContact = ContactLedger(
                    contactId = localContact?.contactId ?: 0,
                    uuid = uuid,
                    name = data["name"] as? String ?: "Unknown",
                    phone = data["phone"] as? String ?: "",
                    photoUri = data["photoUri"] as? String,
                    upiId = data["upiId"] as? String,
                    totalLent = (data["totalLent"] as? Number)?.toDouble() ?: 0.0,
                    totalBorrowed = (data["totalBorrowed"] as? Number)?.toDouble() ?: 0.0,
                    netBalance = (data["netBalance"] as? Number)?.toDouble() ?: 0.0,
                    lastUpdated = cloudLastUpdated
                )
                val localId = peopleDao.insertContact(newContact)
                downloadLoansForContact(userId, uuid, localId)
            } else {
                downloadLoansForContact(userId, uuid, localContact.contactId)
            }
        }
    }

    private suspend fun downloadLoansForContact(userId: String, contactUuid: String, localContactId: Long) {
        val loansRef = firestore.collection("users").document(userId)
            .collection("people").document(contactUuid).collection("loans")
        val cloudLoansSnapshot = loansRef.get().await()
        
        for (doc in cloudLoansSnapshot.documents) {
            val data = doc.data ?: continue
            val loanUuid = doc.id
            val cloudLastUpdated = (data["lastUpdated"] as? Number)?.toLong() ?: 0L
            
            val localLoan = peopleDao.getTransactionByUuid(loanUuid)
            
            if (localLoan == null || cloudLastUpdated > localLoan.lastUpdated) {
                val typeStr = data["type"] as? String ?: LoanType.LENT.name
                val newLoan = LoanTransaction(
                    id = localLoan?.id ?: 0,
                    uuid = loanUuid,
                    contactId = localContactId,
                    contactUuid = contactUuid,
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    type = try { LoanType.valueOf(typeStr) } catch(_: Exception) { LoanType.LENT },
                    date = (data["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    remark = data["remark"] as? String,
                    isSettled = data["isSettled"] as? Boolean ?: false,
                    partialSettledAmount = (data["partialSettledAmount"] as? Number)?.toDouble() ?: 0.0,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    lastUpdated = cloudLastUpdated,
                    sourceTransactionId = null
                )
                peopleDao.insertLoanTransaction(newLoan)
            }
        }
        peopleDao.recalculateContactBalance(localContactId)
    }

    private suspend fun deleteContactFromCloud(uuid: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("people").document(uuid).delete().await()
        } catch (e: Exception) {
            Log.e("PeopleLedgerRepo", "Immediate cloud delete failed", e)
        }
    }

    private suspend fun deleteLoanFromCloud(contactUuid: String, loanUuid: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("people").document(contactUuid)
                .collection("loans").document(loanUuid).delete().await()
        } catch (e: Exception) {
            Log.e("PeopleLedgerRepo", "Immediate cloud loan delete failed", e)
        }
    }

    private fun ContactLedger.toFirestoreMap() = mapOf(
        "name" to name,
        "phone" to phone,
        "photoUri" to photoUri,
        "upiId" to upiId,
        "totalLent" to totalLent,
        "totalBorrowed" to totalBorrowed,
        "netBalance" to netBalance,
        "lastUpdated" to lastUpdated
    )

    private fun LoanTransaction.toFirestoreMap() = mapOf(
        "contactUuid" to contactUuid,
        "amount" to amount,
        "type" to type.name,
        "date" to date,
        "remark" to remark,
        "isSettled" to isSettled,
        "partialSettledAmount" to partialSettledAmount,
        "createdAt" to createdAt,
        "lastUpdated" to lastUpdated
    )
}
