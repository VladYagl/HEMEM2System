import com.jessecorbett.diskord.api.channel.ChannelClient
import com.jessecorbett.diskord.api.common.Message
import com.jessecorbett.diskord.bot.BotContext
import com.jessecorbett.diskord.util.sendMessage
import com.petersamokhin.vksdk.core.client.VkApiClient
import com.petersamokhin.vksdk.core.model.VkSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VKBot {
    lateinit var context: BotContext
    private val vkClient = VkApiClient(VK_GROUP_ID, VK_GROUP_KEY, VkApiClient.Type.Community, VkSettings(httpClient))

    suspend fun init(context: BotContext) {
        this.context = context
        vkClient.onMessage { messageEvent ->
            mainScope.launch(Dispatchers.Default) {
                safe {
                    val text = messageEvent.message.text
                    val id = messageEvent.message.peerId

                    boys.find { it.chat == id }?.let { boy ->
                        if (text.startsWith("!help")) {
                            vkClient.sendMessage { peerId = id; message = "!quit - прекратить пересылку" }.execute()
                            return@let
                        }
                        if (text.startsWith("!quit")) {
                            boys.remove(boy)
                            save()
                            vkClient.sendMessage { peerId = id; message = "оке я о тебе забыл" }.execute()
                            return@let
                        }

                        vkChannelId?.let {
                            context.channel(it).sendMessage(boy.tag + ": " + text)
                            vkClient.sendMessage { peerId = id; message = "ушло" }.execute()
                        } ?: run {
                            vkClient.sendMessage {
                                peerId = id; message = "[вк -> дискорд] не настроен (!vk-init)"
                            }.execute()
                        }
                    } ?: run {
                        if (text.matches(TAG)) {
                            boys.add(Boy(text, id))
                            save()
                            vkClient.sendMessage { peerId = id; message = "гж" }.execute()
                        } else {
                            vkClient.sendMessage { peerId = id; message = "дискорд тэг плз" }
                                .execute()
                        }
                    }
                }
            }
        }

        vkClient.startLongPolling()
    }

    private suspend fun informBoy(m: Message, boy: Boy) {
        vkClient.sendMessage {
            peerId = boy.chat
            var text = m.content
            m.usersMentioned.forEach {
                text = text.replace("<@!${it.id}>", "@" + it.username)
            }

            message = m.author.tag + ": " + text
        }.execute()
    }

    private suspend fun addReaction(reaction: String, m: Message) {
        with(context) {
            channel(m.channelId).addMessageReaction(
                m.id,
                guild(m.guildId!!).getEmoji().find { it.name == reaction }!!
            )
        }
    }

    suspend fun onMessage(m: Message) {
        with(context) {
            if (m.mentionsEveryone) {
                boys.forEach { informBoy(m, it) }

                addReaction("WutFace", m)
                addReaction("Vjuh", m)
            }
            m.usersMentioned.forEach { user ->
                boys.find { it.tag == user.tag }?.let { boy ->
                    channel(m.channelId).triggerTypingIndicator()
                    informBoy(m, boy)

                    addReaction("Vjuh", m)
//                    channel(m.channelId).sendMessage("Передал пидорасу в вк")
                }
            }

            if (m.rolesIdsMentioned.isNotEmpty()) {
                val members = guild(m.guildId!!).getMembers()

                m.rolesIdsMentioned.forEach { roleId ->
                    members.filter { it.roleIds.contains(roleId) }
                        .mapNotNull { member -> boys.find { it.tag == member.user?.tag } }
                        .forEach { informBoy(m, it) }
                }
            }
        }
    }
}