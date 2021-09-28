import com.jessecorbett.diskord.api.channel.MessageEdit
import com.jessecorbett.diskord.api.common.GuildTextChannel
import com.jessecorbett.diskord.api.common.Message
import com.jessecorbett.diskord.bot.BotContext
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.coroutines.delay

private fun scanMessages(messages: List<Message>, guildId: String) {
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

suspend fun BotContext.img(message: Message) {
    channel(message.channelId).triggerTypingIndicator()
    guild(message.guildId!!).getChannels().forEach { channel ->
        if (!lastMessage.containsKey(channel.id) && channel is GuildTextChannel) {
            attachments.getOrPut(message.guildId!!) { ArrayList() }
            channel(channel.id).getMessages(1).lastOrNull()?.id?.let { last ->
                val status = channel(message.channelId).sendMessage("Сканируем канал ${channel.name}...")
                channel(message.channelId).triggerTypingIndicator()

                var current = last

                var cnt = 0
                while (true) {
                    cnt++
                    val messages = channel(channel.id).getMessagesBefore(100, current)
                    if (messages.isNotEmpty()) {
                        scanMessages(messages, message.guildId!!)

                        current = messages.last().id
                        if (cnt % 10 == 3) {
                            channel(message.channelId).editMessage(
                                status.id,
                                MessageEdit(
                                    "Сканируем канал ${channel.name}...\n Готово начиная с ${messages.last().sentAt}"
                                )
                            )
                        }
                        delay(860)
                    } else {
                        delay(1000)
                        break
                    }
                }

                lastMessage[channel.id] = last
                channel(message.channelId).editMessage(
                    status.id,
                    MessageEdit("Сканируем канал ${channel.name} | :white_check_mark:")
                )
            }

            delay(500)
        }
        while (true) {
            val messages = channel(channel.id).getMessagesAfter(
                100, lastMessage[channel.id]
                    ?: break
            )
            if (messages.isNotEmpty()) {
                scanMessages(messages, message.guildId!!)

                lastMessage[channel.id] = messages.first().id
                delay(800)
            } else {
                break
            }
        }

        save()
    }

    channel(message.channelId).sendMessage(
        "Тут ${attachments[message.guildId!!]!!.size} картинок\n" +
                attachments[message.guildId!!]!!.random()
    )
}