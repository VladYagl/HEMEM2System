import com.beust.klaxon.Klaxon
import com.jessecorbett.diskord.api.channel.ChannelClient
import com.jessecorbett.diskord.api.channel.MessageEdit
import com.jessecorbett.diskord.api.common.GuildTextChannel
import com.jessecorbett.diskord.api.common.Message
import com.jessecorbett.diskord.api.common.User
import com.jessecorbett.diskord.bot.bot
import com.jessecorbett.diskord.bot.classicCommands
import com.jessecorbett.diskord.bot.events
import com.jessecorbett.diskord.util.DiskordInternals
import com.jessecorbett.diskord.util.sendMessage
import com.petersamokhin.vksdk.core.client.VkApiClient
import com.petersamokhin.vksdk.core.model.VkSettings
import com.petersamokhin.vksdk.http.VkOkHttpClient
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

const val TOKEN = "ODgxMTE0NDY2ODcwMjM1MTc3.YSoHyw.spVIHFL9l_l-0HrHzT89ZWr3YRY"
const val ID = "881114466870235177"
const val VK_API_KEY = "df3d3b05df3d3b05df3d3b0563df445340ddf3ddf3d3b05be75a1a6df4c336dd181f1ea"

const val VK_GROUP_KEY = "94e9a1c0957ef54f695d5febe6e6222a333ee41d9e0b621a97c42b534f707d0a3a274f20b60516c2fd4db"
const val VK_GROUP_ID = 207396896

val TAG = ".*#[0-9]{4}".toRegex()

var attachments = HashMap<String, ArrayList<String>>()
var lastMessage = HashMap<String, String>()
var boys = ArrayList<Boy>()
var vkChannelId: String? = null

data class Boy(
        val tag: String,
        val chat: Int
) : Serializable


const val MORDA_ID = 83276396

val klaxon = Klaxon()
val mainScope = MainScope()

val User.tag get() = this.username + "#" + this.discriminator

val attFile = File("data.att")
val lastFile = File("data.last")
val boysFile = File("data.boys")
val vkFile = File("data.vk")

fun save() {
    println(runCatching {
        ObjectOutputStream(attFile.outputStream()).use { stream ->
            stream.writeObject(attachments)
        }
    })
    println(runCatching {
        ObjectOutputStream(lastFile.outputStream()).use { stream ->
            stream.writeObject(lastMessage)
        }
    })
    println(runCatching {
        ObjectOutputStream(boysFile.outputStream()).use { stream ->
            stream.writeObject(boys)
        }
    })
    println(runCatching {
        ObjectOutputStream(vkFile.outputStream()).use { stream ->
            stream.writeObject(vkChannelId)
        }
    })
}

fun scanMessages(messages: List<Message>, guildId: String) {
    val attachments = attachments[guildId]!!
    messages.forEach { message ->
        message.attachments.forEach {
            attachments.add(it.url)
        }
        message.embeds.forEach {
            it.url?.let { url ->
                attachments.add(url)
            }
        }
    }
}

suspend fun safe(channel: ChannelClient? = null, body: suspend () -> Unit) {
    try {
        body()
    } catch (e: Throwable) {
        println()
        println("=============================")
        println()
        e.printStackTrace()

        try {
            channel?.sendMessage("я хз что-то опять сломалось")
        } catch (e: Throwable) {
            println()
            println("NOT AGAIN!!!")
            println()
            e.printStackTrace()
        }
    }
}

// TODO: when group calls (@clasher)
// TODO: resend everything to vk

@ExperimentalCoroutinesApi
@DiskordInternals
suspend fun main() {
    @Suppress("UNCHECKED_CAST")
    println(runCatching {
        ObjectInputStream(attFile.inputStream()).use { stream ->
            attachments = stream.readObject() as HashMap<String, ArrayList<String>>
        }
    })
    println(runCatching {
        ObjectInputStream(lastFile.inputStream()).use { stream ->
            lastMessage = stream.readObject() as HashMap<String, String>
        }
    })
    println(runCatching {
        ObjectInputStream(boysFile.inputStream()).use { stream ->
            boys = stream.readObject() as ArrayList<Boy>
        }
    })
    println(runCatching {
        ObjectInputStream(vkFile.inputStream()).use { stream ->
            vkChannelId = stream.readObject() as String?
        }
    })

//    println(attachments)
//    println(lastMessage)
//    return

    val httpClient = VkOkHttpClient()
//    val httpClient = httpClientWithLog()
    val client = VkApiClient(VK_GROUP_ID, VK_GROUP_KEY, VkApiClient.Type.Community, VkSettings(httpClient))
    var vkJob: Job? = null

    bot(TOKEN) {
        events {
            //TODO: bot starts multiple times
            //TODO: in general test bot restarts
            onReady {
                println("\n\n\t\tNew onReady\n\n")
                if (vkJob == null) {
                    vkJob = mainScope.launch(Dispatchers.Default) {
                        safe {
                            client.onMessage { messageEvent ->
                                mainScope.launch(Dispatchers.Default) {
                                    safe {
                                        val text = messageEvent.message.text
                                        val id = messageEvent.message.peerId

                                        boys.find { it.chat == id }?.let { boy ->
                                            vkChannelId?.let {
                                                channel(it).sendMessage(boy.tag + ": " + text)
                                                client.sendMessage { peerId = id; message = "ушло" }.execute()
                                            } ?: run {
                                                client.sendMessage { peerId = id; message = "[вк -> дискорд] не настроен (!vk-init)" }.execute()
                                            }
                                        } ?: run {
                                            if (text.matches(TAG)) {
                                                boys.add(Boy(text, id))
                                                save()
                                                client.sendMessage { peerId = id; message = "гж" }.execute()
                                            } else {
                                                client.sendMessage { peerId = id; message = "дискорд тэг плз" }.execute()
                                            }
                                        }
                                    }
                                }
                            }

                            client.startLongPolling()
                        }
                    }
                }
            }

            onMessageCreate { m ->
                safe(channel(m.channelId)) {
                    m.usersMentioned.forEach { user ->
                        boys.find { it.tag == user.tag }?.let { boy ->
                            channel(m.channelId).triggerTypingIndicator()
                            client.sendMessage {
                                peerId = boy.chat;
                                var text = m.content
                                m.usersMentioned.forEach {
                                    text = text.replace("<@!${it.id}>", "@" + it.username)
                                }

                                message = m.author.tag + ": " + text
                            }.execute()

                            channel(m.channelId).addMessageReaction(m.id, guild(m.guildId!!).getEmoji().find { it.name == "Vjuh" }!!)
//                        channel(m.channelId).sendMessage("Передал пидорасу в вк")
                        }
                    }

                }
            }
        }

        classicCommands(commandPrefix = "!") {

            command("help") { context ->
                channel(context.channelId).sendMessage("""**!vk** - линка чтобы получать сообщения через меня в ВК
**!vk-init** - пересылать сообщения из ВК в этот канал
**!img** - рандомная пикча с сервера
**!morda** - репостить морду в этот канал *(WARNING: можно случайно дважды запустить на один канал)*""")
            }

            command("vk") { context ->
                channel(context.channelId).sendMessage("https://vk.com/im?sel=-207396896")
            }

            command("vk-init") { context ->
                vkChannelId = context.channelId
                save()
                channel(context.channelId).sendMessage("OK [вк -> дискорд] здесь")
            }

            command("morda") { context ->
                safe(channel(context.channelId)) {
                    val morda = VkApiClient(MORDA_ID, VK_API_KEY, VkApiClient.Type.Community, VkSettings(httpClient))

                    var lastPostId = morda.lastPost()?.id

                    channel(context.channelId).sendMessage("Ждем новых постов от Морды...")

                    mainScope.launch(Dispatchers.Default) {
                        while (true) {
                            delay(200000)
//                        delay(2000)

                            morda.lastPost()?.let { lastPost ->
                                if (lastPost.id != lastPostId) {
                                    lastPostId = lastPost.id
                                    println(lastPost)

                                    channel(context.channelId).sendMessage("Морда запостил!\n\n${lastPost.text}\n${
                                        lastPost.attachments?.map { att ->
                                            when (att.type) {
                                                "photo" -> att.photo?.sizes?.maxByOrNull { it.width }?.url ?: ""
                                                "video" -> att.video?.image?.maxByOrNull { it.width }?.url ?: ""
                                                else -> " <<Unknown attachment type : ${att.type}>> "
                                            }
                                        }
                                    }"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            command("img") { context ->
                safe(channel(context.channelId)) {
                    channel(context.channelId).triggerTypingIndicator()
                    guild(context.guildId!!).getChannels().forEach { channel ->
                        if (!lastMessage.containsKey(channel.id) && channel is GuildTextChannel) {
                            attachments.getOrPut(context.guildId!!) { ArrayList() }
                            channel(channel.id).getMessages(1).lastOrNull()?.id?.let { last ->
                                val status = channel(context.channelId).sendMessage("Сканируем канал ${channel.name}...")
                                channel(context.channelId).triggerTypingIndicator()

                                var current = last

                                var cnt = 0
                                while (true) {
                                    cnt++
                                    val messages = channel(channel.id).getMessagesBefore(100, current)
                                    if (messages.isNotEmpty()) {
                                        scanMessages(messages, context.guildId!!)

                                        current = messages.last().id
                                        if (cnt % 10 == 3) {
                                            channel(context.channelId).editMessage(
                                                    status.id,
                                                    MessageEdit(
                                                            "Сканируем канал ${channel.name}...\n Готово начиная с ${messages.last().sentAt}"
                                                    )
                                            )
                                        }
                                        delay(860)
                                    } else {
                                        delay(1000);
                                        break
                                    }
                                }

                                lastMessage[channel.id] = last
                                channel(context.channelId).editMessage(
                                        status.id,
                                        MessageEdit("Сканируем канал ${channel.name} | :white_check_mark:")
                                )
                            }

                            delay(500)
                        }
                        while (true) {
                            val messages = channel(channel.id).getMessagesAfter(100, lastMessage[channel.id]
                                    ?: break)
                            if (messages.isNotEmpty()) {
                                scanMessages(messages, context.guildId!!)

                                lastMessage[channel.id] = messages.first().id
                                delay(800)
                            } else {
                                break
                            }
                        }

                        save()
                    }

                    channel(context.channelId).sendMessage("Тут ${attachments[context.guildId!!]!!.size} картинок\n" +
                            attachments[context.guildId!!]!!.random())
                }
            }
        }
    }
}
