import com.jessecorbett.diskord.api.channel.ChannelClient
import com.jessecorbett.diskord.api.common.User
import com.jessecorbett.diskord.bot.bot
import com.jessecorbett.diskord.bot.classicCommands
import com.jessecorbett.diskord.bot.events
import com.jessecorbett.diskord.util.DiskordInternals
import com.jessecorbett.diskord.util.sendMessage
import com.petersamokhin.vksdk.http.VkOkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.Serializable

val TAG = ".*#[0-9]{4}".toRegex()
val User.tag get() = this.username + "#" + this.discriminator

data class Boy(
    val tag: String,
    val chat: Int
) : Serializable

var attachments = HashMap<String, ArrayList<String>>()
var lastMessage = HashMap<String, String>()
var boys = ArrayList<Boy>()
var vkChannelId: String? = null

val mainScope = MainScope()

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

val httpClient = VkOkHttpClient()
//    val httpClient = httpClientWithLog()

@DiskordInternals
suspend fun main() {
    var vkJob: Job? = null
    val vkBot = VKBot()
    load()

    bot(TOKEN) {
        events {
            onReady {
                println("\n\n\t\tNew onReady\n\n")
                if (vkJob == null) {
                    vkJob = mainScope.launch(Dispatchers.Default) {
                        safe {
                            vkBot.init(this@events)
                        }
                    }
                }
            }

            onMessageCreate { safe(channel(it.channelId)) { vkBot.onMessage(it) } }
        }

        classicCommands(commandPrefix = "!") {

            command("help") { context ->
                channel(context.channelId).sendMessage(
                    """**!vk** - линка чтобы получать сообщения через меня в ВК
**!vk-init** - пересылать сообщения из ВК в этот канал
**!img** - рандомная пикча с сервера
**!morda** - репостить морду в этот канал *(WARNING: можно случайно дважды запустить на один канал)*"""
                )
            }

            command("vk") { context ->
                channel(context.channelId).sendMessage("https://vk.com/im?sel=-207396896")
            }

            command("vk-init") { context ->
                vkChannelId = context.channelId
                save()
                channel(context.channelId).sendMessage("OK [вк -> дискорд] здесь")
            }

            command("morda") { safe(channel(it.channelId)) { morda(it) } }


            command("img") { safe(channel(it.channelId)) { img(it) } }
        }
    }
}
