package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val phone: String,
    val bio: String,
    val avatarUrl: String = "",
    val isContact: Boolean = false,
    val isOnline: Boolean = false,
    val lastSeenText: String = "online",
    val isBlocked: Boolean = false
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isGroup: Boolean,
    val avatarUrl: String = "",
    val lastMessageText: String = "",
    val lastMessageTime: Long = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val unreadCount: Int = 0,
    val typingUser: String? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String = "",
    val text: String,
    val timestamp: Long,
    val isMine: Boolean,
    val deliveryStatus: String = "sent", // "sending", "sent", "read"
    val isVoice: Boolean = false,
    val voiceDuration: Int = 0,
    val mediaUrl: String = "",
    val mediaType: String = "text", // "text", "image", "video", "file"
    val reaction: String = ""
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long,
    val isViewed: Boolean = false
)

@Entity(tableName = "calls")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String = "",
    val isVideo: Boolean,
    val isIncoming: Boolean,
    val duration: String = "0:00",
    val status: String, // "completed", "missed", "declined"
    val timestamp: Long
)

@Dao
interface AppDao {
    // Users
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isContact = 1")
    fun getContacts(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET isBlocked = :blocked WHERE id = :userId")
    suspend fun updateBlockedStatus(userId: String, blocked: Boolean)

    // Chats
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = :archived ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getChatsByArchiveState(archived: Boolean): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("UPDATE chats SET isPinned = :pinned WHERE id = :chatId")
    suspend fun updateChatPinned(chatId: String, pinned: Boolean)

    @Query("UPDATE chats SET isArchived = :archived WHERE id = :chatId")
    suspend fun updateChatArchived(chatId: String, archived: Boolean)

    @Query("UPDATE chats SET unreadCount = :unreadCount WHERE id = :chatId")
    suspend fun updateUnreadCount(chatId: String, unreadCount: Int)

    @Query("UPDATE chats SET lastMessageText = :msgText, lastMessageTime = :time WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, msgText: String, time: Long)

    // Messages
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    // Stories
    @Query("SELECT * FROM stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Query("UPDATE stories SET isViewed = 1 WHERE id = :storyId")
    suspend fun viewStory(storyId: String)

    // Calls
    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallLogEntity)
}

@Database(entities = [UserEntity::class, ChatEntity::class, MessageEntity::class, StoryEntity::class, CallLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aether_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
