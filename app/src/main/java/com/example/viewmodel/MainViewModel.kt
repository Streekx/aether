package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AuthState {
    object Splash : AuthState
    object Onboarding : AuthState
    object Login : AuthState
    object SignUp : AuthState
    object OtpVerification : AuthState
    object Authenticated : AuthState
}

data class AccountProfile(
    val userId: String,
    val username: String,
    val displayName: String,
    val phone: String,
    val bio: String,
    val avatarUrl: String = ""
)

// Active Call state
data class CallState(
    val isActive: Boolean = false,
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val isVideo: Boolean = false,
    val isIncoming: Boolean = false,
    val isConnected: Boolean = false,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val durationSec: Int = 0
)

// Active Story display state
data class ActiveStoryState(
    val isOpen: Boolean = false,
    val stories: List<StoryEntity> = emptyList(),
    val currentIndex: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(database.dao())

    // --- Authentication & Account State ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Splash)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Multi-account profiles support
    val accounts = listOf(
        AccountProfile("me_fox", "fox_desire", "Fox Desire", "+91 9354771016", "Obsidian Core developer. Building the future in Royal Purple.", ""),
        AccountProfile("me_ansh", "ansh_escrow", "Ansh Escrow", "+91 7481992015", "Secure transactions, escrow specialist. Living the dream.", "")
    )
    
    private val _currentAccountIdx = MutableStateFlow(0)
    val currentAccountIdx: StateFlow<Int> = _currentAccountIdx.asStateFlow()
    
    val currentUserProfile = _currentAccountIdx.map { accounts[it] }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), accounts[0]
    )

    // --- Call State ---
    private val _callState = MutableStateFlow(CallState())
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    // --- Stories State ---
    private val _storyState = MutableStateFlow(ActiveStoryState())
    val storyState: StateFlow<ActiveStoryState> = _storyState.asStateFlow()

    // --- Search Query ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Reactive Database Observables ---
    val allChats: StateFlow<List<ChatEntity>> = repository.allChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<UserEntity>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stories: StateFlow<List<StoryEntity>> = repository.allStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLogEntity>> = repository.allCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Locked / Passcode state
    private val _isPasscodeLocked = MutableStateFlow(false)
    val isPasscodeLocked: StateFlow<Boolean> = _isPasscodeLocked.asStateFlow()

    private val _passcode = MutableStateFlow("1234") // Simulated Passcode
    val passcode: StateFlow<String> = _passcode.asStateFlow()

    // General Settings preferences loaded in memory
    private val _isNotificationsEnabled = MutableStateFlow(true)
    val isNotificationsEnabled = _isNotificationsEnabled.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage = _selectedLanguage.asStateFlow()

    // Typing simulated events
    private val _typingStatusMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val typingStatusMap = _typingStatusMap.asStateFlow()

    init {
        // Run Splash animation then transition
        viewModelScope.launch {
            delay(2200) // Splash screens runs for 2.2 seconds for nice branding
            _authState.value = AuthState.Onboarding
        }
        
        // Seed initial mock data into database if empty
        seedDataIfNeeded()
    }

    // --- Database Seeding ---
    private fun seedDataIfNeeded() {
        viewModelScope.launch {
            repository.allChats.first().let { currentChats ->
                if (currentChats.isEmpty()) {
                    // Seed Users
                    val defaultUsers = listOf(
                        UserEntity("user_aurora", "aura_core", "Aura Core Assistant", "*000", "Futuristic AI chat assistant powered by Gemini API.", isContact = true, isOnline = true),
                        UserEntity("user_grim", "grim_dev", "Grim Dev", "+1 310 934 1120", "Building next-gen applications. Keep code clean.", isContact = true, isOnline = true),
                        UserEntity("user_sophia", "sophia_vance", "Sophia Vance", "+1 415 889 0014", "Visual designer | Minimalist advocate. Creative director @Aether.", isContact = true, isOnline = true),
                        UserEntity("user_liam", "liam_sterling", "Liam Sterling", "+1 650 310 9802", "Cybersecurity and server architectures.", isContact = true, isOnline = false, lastSeenText = "last seen 3 hours ago"),
                        UserEntity("user_aria", "aria_pulse", "Aria Pulse", "+44 7911 123456", "Vibe curators. Music is life.", isContact = true, isOnline = true),
                        UserEntity("user_blocked", "blocked_spammer", "Blocked Contact", "+99 999 999 99", "I spam links.", isContact = true, isBlocked = true)
                    )
                    repository.insertUsers(defaultUsers)

                    // Seed Chats
                    val defaultChats = listOf(
                        ChatEntity("chat_aura", "Aura Core AI", isGroup = false, lastMessageText = "Welcome to Aether! I'm your AI controller.", lastMessageTime = System.currentTimeMillis() - 600000, unreadCount = 0),
                        ChatEntity("chat_grim", "Grim Dev", isGroup = false, lastMessageText = "Suggest Me First Video Topic", lastMessageTime = System.currentTimeMillis() - 1200000, unreadCount = 1),
                        ChatEntity("chat_grix_world", "GRix World", isGroup = true, lastMessageText = "Channel created and first patch uploaded.", lastMessageTime = System.currentTimeMillis() - 1800000, unreadCount = 0),
                        ChatEntity("chat_grim_dev_grp", "Grim Dev chat", isGroup = true, lastMessageText = "Please explain or define your idea detailed", lastMessageTime = System.currentTimeMillis() - 2400000, unreadCount = 0),
                        ChatEntity("chat_sophia", "Sophia Vance", isGroup = false, lastMessageText = "The high-contrast royal purple layout looks flawless!", lastMessageTime = System.currentTimeMillis() - 200000, unreadCount = 0),
                        ChatEntity("chat_nova", "Nova Tech Club", isGroup = true, lastMessageText = "Aria Pulse shared a cosmic synth loop.", lastMessageTime = System.currentTimeMillis() - 86400000, unreadCount = 0)
                    )
                    for (c in defaultChats) {
                        repository.insertChat(c)
                    }

                    // Seed Messages
                    val messages = listOf(
                        // Aura Core Messages
                        MessageEntity("m_aur1", "chat_aura", "user_aurora", "Aura Core Assistant", "", "Hello, developer. I am Aura. Ready to demo the multi-modal Gemini interface?", System.currentTimeMillis() - 700000, isMine = false),
                        MessageEntity("m_aur2", "chat_aura", "me_fox", "Fox Desire", "", "Absolutely! Tell me about Aether's tech design.", System.currentTimeMillis() - 650000, isMine = true),
                        MessageEntity("m_aur3", "chat_aura", "user_aurora", "Aura Core Assistant", "", "Aether utilizes SQLite Room db for reactive local offline persistence, a forced obsidian black UI with rich royal purple accents, and edge-to-edge screens.", System.currentTimeMillis() - 600000, isMine = false),
                        
                        // Grim Dev Messages
                        MessageEntity("m_g1", "chat_grim", "me_fox", "Fox Desire", "", "Hey Grim, did you run the unit tests?", System.currentTimeMillis() - 1400000, isMine = true),
                        MessageEntity("m_g2", "chat_grim", "user_grim", "Grim Dev", "", "Yes, built and ran standard suite without errors. Suggest Me First Video Topic", System.currentTimeMillis() - 1220000, isMine = false, deliveryStatus = "sent"),
                        
                        // GRix World Channel
                        MessageEntity("m_gx1", "chat_grix_world", "user_grim", "Grim Dev", "", "Channel created and first patch uploaded.", System.currentTimeMillis() - 1800000, isMine = false),

                        // Grim Dev chat Group
                        MessageEntity("m_gg1", "chat_grim_dev_grp", "user_aria", "Aria Pulse", "", "We need a futuristic dark layout", System.currentTimeMillis() - 2500000, isMine = false),
                        MessageEntity("m_gg2", "chat_grim_dev_grp", "user_grim", "Grim Dev", "", "Please explain or define your idea detailed", System.currentTimeMillis() - 2400000, isMine = false),

                        // Sophia Vance Messages
                        MessageEntity("m_s1", "chat_sophia", "user_sophia", "Sophia Vance", "", "Hey Fox! Just reviewed the new dark mode aesthetics.", System.currentTimeMillis() - 400000, isMine = false),
                        MessageEntity("m_s2", "chat_sophia", "me_fox", "Fox Desire", "", "Is the royal purple color high-contrast enough?", System.currentTimeMillis() - 300000, isMine = true),
                        MessageEntity("m_s3", "chat_sophia", "user_sophia", "Sophia Vance", "", "Yes! The high-contrast royal purple layout looks flawless!", System.currentTimeMillis() - 200000, isMine = false)
                    )
                    for (m in messages) {
                        repository.addMessageDirectly(m)
                    }

                    // Seed Stories
                    val storiesMock = listOf(
                        StoryEntity(id = "s1", userId = "user_sophia", userName = "Sophia Vance", userAvatar = "", text = "Designing neon cyber aesthetics!", timestamp = System.currentTimeMillis() - 3600000),
                        StoryEntity(id = "s2", userId = "user_grim", userName = "Grim Dev", userAvatar = "", text = "Midnight coding session...", timestamp = System.currentTimeMillis() - 7200000),
                        StoryEntity(id = "s3", userId = "user_aria", userName = "Aria Pulse", userAvatar = "", text = "Cosmic beats unlocked 🎵", timestamp = System.currentTimeMillis() - 10800000)
                    )
                    for (str in storiesMock) {
                        repository.insertStory(str)
                    }

                    // Seed Calls
                    val mockCalls = listOf(
                        CallLogEntity("c1", "user_sophia", "Sophia Vance", "", isVideo = true, isIncoming = true, duration = "12:14", status = "completed", timestamp = System.currentTimeMillis() - 3600000),
                        CallLogEntity("c2", "user_grim", "Grim Dev", "", isVideo = false, isIncoming = false, duration = "2:45", status = "completed", timestamp = System.currentTimeMillis() - 14400000),
                        CallLogEntity("c3", "user_liam", "Liam Sterling", "", isVideo = false, isIncoming = true, duration = "0:00", status = "missed", timestamp = System.currentTimeMillis() - 86400000)
                    )
                    for (cl in mockCalls) {
                        repository.insertCall(cl)
                    }
                }
            }
        }
    }

    // --- Actions ---

    fun completeOnboarding() {
        _authState.value = AuthState.Login
    }

    fun login() {
        _authState.value = AuthState.SignUp
    }

    fun signUp() {
        _authState.value = AuthState.OtpVerification
    }

    fun verifyOtp() {
        _authState.value = AuthState.Authenticated
        // Simulate background automated incoming typing events on start to make app lively!
        triggerTypingSimulations()
    }

    fun logout() {
        _authState.value = AuthState.Login
    }

    fun switchAccount(index: Int) {
        if (index in accounts.indices) {
            _currentAccountIdx.value = index
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPasscodeLocked(locked: Boolean) {
        _isPasscodeLocked.value = locked
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _isNotificationsEnabled.value = enabled
    }

    fun selectLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    // --- Chat Actions ---

    fun getMessages(chatId: String): Flow<List<MessageEntity>> {
        return repository.getMessagesForChat(chatId)
    }

    fun sendMessage(chatId: String, text: String, isVoice: Boolean = false, voiceDuration: Int = 0) {
        viewModelScope.launch {
            val user = currentUserProfile.value
            val message = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = user.userId,
                senderName = user.displayName,
                senderAvatar = user.avatarUrl,
                text = text,
                timestamp = System.currentTimeMillis(),
                isMine = true,
                deliveryStatus = "sending",
                isVoice = isVoice,
                voiceDuration = voiceDuration
            )
            repository.sendMessage(message)

            // Auto status update from 'sending' to 'sent'
            delay(500)
            val sentMsg = message.copy(deliveryStatus = "sent")
            repository.sendMessage(sentMsg)

            // If user messaged Aura AI core, let's trigger Gemini core response!
            if (chatId == "chat_aura") {
                simulateAuraTypingAndRespond(chatId, text)
            } else {
                // Generic contact auto responder for simulation!
                simulateContactResponder(chatId, text)
            }
        }
    }

    private fun simulateAuraTypingAndRespond(chatId: String, text: String) {
        viewModelScope.launch {
            delay(800)
            _typingStatusMap.value = _typingStatusMap.value + (chatId to "Aura Core computing...")
            
            // Get conversation history for context
            val currentMsgs = repository.getMessagesForChat(chatId).first()
            val history = currentMsgs.takeLast(10).map { it.text to it.isMine }

            val response = GeminiClient.chatWithAura(history, text)
            
            delay(1200)
            _typingStatusMap.value = _typingStatusMap.value - chatId

            val reply = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = "user_aurora",
                senderName = "Aura Core Assistant",
                text = response,
                timestamp = System.currentTimeMillis(),
                isMine = false,
                deliveryStatus = "read"
            )
            repository.sendMessage(reply)
        }
    }

    private fun simulateContactResponder(chatId: String, text: String) {
        viewModelScope.launch {
            delay(1200)
            val senderName = when(chatId) {
                "chat_grim" -> "Grim Dev"
                "chat_sophia" -> "Sophia Vance"
                else -> "Aether Core"
            }
            val senderId = when(chatId) {
                "chat_grim" -> "user_grim"
                "chat_sophia" -> "user_sophia"
                else -> "user_aurora"
            }
            
            _typingStatusMap.value = _typingStatusMap.value + (chatId to "$senderName is typing...")
            delay(2000)
            _typingStatusMap.value = _typingStatusMap.value - chatId

            val responseText = when {
                text.lowercase().contains("hello") || text.lowercase().contains("hi") -> "Hey! Good to hear from you."
                text.lowercase().contains("why") || text.lowercase().contains("how") -> "Let me check the core repo. Give me a sec."
                text.lowercase().contains("design") -> "The obsidian black combined with royal purple is extremely polished, outstanding design work!"
                text.lowercase().contains("stories") -> "Have you viewed my story? Posted a sneak peek earlier."
                else -> "Acknowledged: \"$text\". I'll analyze this in detail."
            }

            val reply = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                text = responseText,
                timestamp = System.currentTimeMillis(),
                isMine = false,
                deliveryStatus = "read"
            )
            repository.sendMessage(reply)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun pinChat(chatId: String, pin: Boolean) {
        viewModelScope.launch {
            repository.updateChatPinned(chatId, pin)
        }
    }

    fun archiveChat(chatId: String, archive: Boolean) {
        viewModelScope.launch {
            repository.updateChatArchived(chatId, archive)
        }
    }

    fun blockUser(userId: String, block: Boolean) {
        viewModelScope.launch {
            repository.updateBlockedStatus(userId, block)
        }
    }

    // --- Story Actions ---

    fun postStory(text: String, imageUri: String = "") {
        viewModelScope.launch {
            val user = currentUserProfile.value
            val story = StoryEntity(
                id = UUID.randomUUID().toString(),
                userId = user.userId,
                userName = user.displayName,
                userAvatar = user.avatarUrl,
                text = text,
                imageUrl = imageUri,
                timestamp = System.currentTimeMillis()
            )
            repository.insertStory(story)
        }
    }

    fun openStoryView(storiesToView: List<StoryEntity>, startIndex: Int) {
        viewModelScope.launch {
            _storyState.value = ActiveStoryState(isOpen = true, stories = storiesToView, currentIndex = startIndex)
            // Automatically mark story as read
            val cur = storiesToView.getOrNull(startIndex)
            if (cur != null) {
                repository.viewStory(cur.id)
            }
        }
    }

    fun nextStory() {
        val state = _storyState.value
        if (state.currentIndex < state.stories.lastIndex) {
            val nextIdx = state.currentIndex + 1
            _storyState.value = state.copy(currentIndex = nextIdx)
            viewModelScope.launch {
                state.stories.getOrNull(nextIdx)?.let { repository.viewStory(it.id) }
            }
        } else {
            closeStoryView()
        }
    }

    fun previousStory() {
        val state = _storyState.value
        if (state.currentIndex > 0) {
            val prevIdx = state.currentIndex - 1
            _storyState.value = state.copy(currentIndex = prevIdx)
        }
    }

    fun closeStoryView() {
        _storyState.value = ActiveStoryState(isOpen = false)
    }

    // --- Contact Sync & Addition ---

    fun addContact(displayName: String, phone: String, username: String) {
        viewModelScope.launch {
            val newUser = UserEntity(
                id = "user_" + UUID.randomUUID().toString().take(6),
                username = username,
                displayName = displayName,
                phone = phone,
                bio = "New contact in Aether network.",
                isContact = true,
                isOnline = false,
                lastSeenText = "joined recently"
            )
            repository.insertUser(newUser)
        }
    }

    // --- Call Actions ---

    fun startCall(userId: String, isVideo: Boolean) {
        viewModelScope.launch {
            val contact = contacts.value.find { it.id == userId }
            val name = contact?.displayName ?: "Aether User"
            val avatar = contact?.avatarUrl ?: ""
            _callState.value = CallState(
                isActive = true,
                userId = userId,
                userName = name,
                userAvatar = avatar,
                isVideo = isVideo,
                isIncoming = false,
                isConnected = false
            )
            
            // Log call immediately
            val callLog = CallLogEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = name,
                userAvatar = avatar,
                isVideo = isVideo,
                isIncoming = false,
                duration = "0:00",
                status = "completed",
                timestamp = System.currentTimeMillis()
            )
            repository.insertCall(callLog)

            // Simulate call connecting...
            delay(2500)
            if (_callState.value.isActive) {
                _callState.value = _callState.value.copy(isConnected = true)
                startCallTimer()
            }
        }
    }

    fun triggerIncomingCall(userId: String, isVideo: Boolean) {
        viewModelScope.launch {
            val contact = contacts.value.find { it.id == userId }
            val name = contact?.displayName ?: "Sophia Vance"
            val avatar = contact?.avatarUrl ?: ""
            _callState.value = CallState(
                isActive = true,
                userId = userId,
                userName = name,
                userAvatar = avatar,
                isVideo = isVideo,
                isIncoming = true,
                isConnected = false
            )
        }
    }

    fun answerCall() {
        _callState.value = _callState.value.copy(isConnected = true, isIncoming = false)
        // Log incoming call log
        viewModelScope.launch {
            val cur = _callState.value
            val log = CallLogEntity(
                id = UUID.randomUUID().toString(),
                userId = cur.userId,
                userName = cur.userName,
                userAvatar = cur.userAvatar,
                isVideo = cur.isVideo,
                isIncoming = true,
                duration = "0:58",
                status = "completed",
                timestamp = System.currentTimeMillis()
            )
            repository.insertCall(log)
        }
        startCallTimer()
    }

    fun hangUpCall() {
        _callState.value = CallState(isActive = false)
    }

    private fun startCallTimer() {
        viewModelScope.launch {
            while (_callState.value.isActive && _callState.value.isConnected) {
                delay(1000)
                if (_callState.value.isActive && _callState.value.isConnected) {
                    val nextSec = _callState.value.durationSec + 1
                    _callState.value = _callState.value.copy(durationSec = nextSec)
                }
            }
        }
    }

    fun toggleMuteCall() {
        _callState.value = _callState.value.copy(isMuted = !_callState.value.isMuted)
    }

    fun toggleCameraCall() {
        _callState.value = _callState.value.copy(isCameraOn = !_callState.value.isCameraOn)
    }

    // Simulated Typing loop when arriving on screen so it looks highly interactive
    private fun triggerTypingSimulations() {
        viewModelScope.launch {
            delay(10000)
            _typingStatusMap.value = mapOf("chat_grim" to "Grim Dev is typing...")
            delay(3000)
            _typingStatusMap.value = emptyMap()
            // Add automated trigger message
            val autoMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = "chat_grim",
                senderId = "user_grim",
                senderName = "Grim Dev",
                text = "Hey Ansh, check out the new design patch! Does it look good?",
                timestamp = System.currentTimeMillis(),
                isMine = false,
                deliveryStatus = "unread"
            )
            repository.addMessageDirectly(autoMsg)
            repository.updateLastMessage("chat_grim", autoMsg.text, autoMsg.timestamp)
            repository.updateUnreadCount("chat_grim", 1)
        }
    }
}
