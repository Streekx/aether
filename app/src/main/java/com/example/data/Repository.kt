package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val dao: AppDao) {
    
    val allChats: Flow<List<ChatEntity>> = dao.getAllChats()
    
    val allContacts: Flow<List<UserEntity>> = dao.getContacts()
    
    val allStories: Flow<List<StoryEntity>> = dao.getAllStories()
    
    val allCalls: Flow<List<CallLogEntity>> = dao.getAllCalls()
    
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = dao.getMessagesForChat(chatId)
    
    suspend fun insertUser(user: UserEntity) = dao.insertUser(user)
    
    suspend fun insertUsers(users: List<UserEntity>) = dao.insertUsers(users)
    
    suspend fun updateBlockedStatus(userId: String, blocked: Boolean) = dao.updateBlockedStatus(userId, blocked)
    
    suspend fun insertChat(chat: ChatEntity) = dao.insertChat(chat)
    
    suspend fun updateChatPinned(chatId: String, pinned: Boolean) = dao.updateChatPinned(chatId, pinned)
    
    suspend fun updateChatArchived(chatId: String, archived: Boolean) = dao.updateChatArchived(chatId, archived)
    
    suspend fun updateUnreadCount(chatId: String, count: Int) = dao.updateUnreadCount(chatId, count)

    suspend fun viewStory(storyId: String) = dao.viewStory(storyId)

    suspend fun insertStory(story: StoryEntity) = dao.insertStory(story)

    suspend fun insertCall(call: CallLogEntity) = dao.insertCall(call)
    
    suspend fun sendMessage(message: MessageEntity) {
        dao.insertMessage(message)
        dao.updateLastMessage(message.chatId, message.text, message.timestamp)
    }
    
    suspend fun deleteMessage(messageId: String) {
        dao.deleteMessage(messageId)
    }

    suspend fun addMessageDirectly(message: MessageEntity) {
        dao.insertMessage(message)
    }

    suspend fun updateLastMessage(chatId: String, text: String, timestamp: Long) = dao.updateLastMessage(chatId, text, timestamp)
}
