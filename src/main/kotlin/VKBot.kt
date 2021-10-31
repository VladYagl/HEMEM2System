import com.jessecorbett.diskord.api.channel.ChannelClient
import com.jessecorbett.diskord.api.common.Emoji
import com.jessecorbett.diskord.api.common.Message
import com.jessecorbett.diskord.bot.BotContext
import com.jessecorbett.diskord.util.sendMessage
import com.petersamokhin.vksdk.core.client.VkApiClient
import com.petersamokhin.vksdk.core.model.VkSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

class VKBot {
    lateinit var context: BotContext
    private val vkClient = VkApiClient(VK_GROUP_ID, VK_GROUP_KEY, VkApiClient.Type.Community, VkSettings(httpClient))
    private val emojis = HashMap<String, List<Emoji>>()

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
//                            vkClient.sendMessage { peerId = id; message = "ушло" }.execute()
                            vkClient.sendMessage { peerId = id; message = "\uD83D\uDCE8" }.execute()
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
        vkClient.onEachEvent {event ->
            val boy = boys.find { event.`object`.toString().contains(it.chat.toString()) }
            println("VK EVENT |${boy}|:\n$event\n")
        }

        vkClient.startLongPolling()
    }

    private suspend fun informBoy(m: Message, boy: Boy) {
        vkClient.sendMessage {
            peerId = boy.chat
            var text = m.content
            m.usersMentioned.forEach {
                text = text.replace("<@!${it.id}>", "@" + it.tag)
            }

            message = m.author.tag + ": " + text
        }.execute()
    }

    suspend fun addReaction(reaction: String, m: Message) {
        with(context) {
            emojis.getOrPut(m.guildId!!) {
                guild(m.guildId!!).getEmoji()
            }.find { it.name == reaction }?.let {
                channel(m.channelId).addMessageReaction(
                    m.id, it
                )
            } ?: run {
                // Use unicode emoji if server doesn't have 'Vjuh' (e.g. my test server)
                channel(m.channelId).addMessageReaction(
                    m.id, "\uD83D\uDC4C"
                )
            }
        }
    }

    suspend fun onMessage(m: Message) {
        if (m.mentionsEveryone) {
            boys.forEach { informBoy(m, it) }

            addReaction("WutFace", m)
            addReaction("Vjuh", m)
        }
        m.usersMentioned.forEach { user ->
            println("${user.tag} was mentioned")
            boys.find { it.tag == user.tag }?.let { boy ->
                println("$boy was matched\n")
                // not sure about typing
//                    channel(m.channelId).triggerTypingIndicator()
                informBoy(m, boy)

                addReaction("Vjuh", m)
//                    channel(m.channelId).sendMessage("Передал пидорасу в вк")
            }
        }

        //TODO: group mentions

//        if (m.rolesIdsMentioned.isNotEmpty()) {
//            val members = guild(m.guildId!!).getMembers()
//
//            m.rolesIdsMentioned.forEach { roleId ->
//                members.filter { it.roleIds.contains(roleId) }
//                    .mapNotNull { member -> boys.find { it.tag == member.user?.tag } }
//                    .forEach { informBoy(m, it) }
//            }
//        }
    }
}