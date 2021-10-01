import com.jessecorbett.diskord.api.channel.ChannelClient
import com.jessecorbett.diskord.api.common.User
import com.jessecorbett.diskord.bot.bot
import com.jessecorbett.diskord.bot.classicCommands
import com.jessecorbett.diskord.bot.events
import com.jessecorbett.diskord.util.DiskordInternals
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.isFromBot
import com.jessecorbett.diskord.util.sendMessage
import com.petersamokhin.vksdk.http.VkOkHttpClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.io.Serializable
import java.io.StringReader

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

val gitHttpClient = HttpClient(CIO)
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

            onMessageCreate {
                safe(channel(it.channelId)) {
                    if (!it.isFromBot) {
                        vkBot.onMessage(it)
                    }
                }
            }
        }

        classicCommands(commandPrefix = "!") {

            command("help") { context ->
                channel(context.channelId).sendMessage(
                        """
***Discord Команды***
**!vk** - линка чтобы получать сообщения через меня в ВК
**!vk-init** - пересылать сообщения из ВК в этот канал

**!img** - рандомная пикча с сервера
**!morda** - репостить морду в этот канал *(WARNING: можно случайно дважды запустить на один канал)*

**!git** - линк на гитхаб
**!issues** - список issues на гитхабе
**!issue-add <title>** - добавить новую issue с названием <title>


***ВК Команды***
**!quit** - прекратить пересылку""")
            }

            command("vk") { context ->
                channel(context.channelId).sendMessage("https://vk.com/im?sel=-207396896")
            }

            command("git") { context ->
                channel(context.channelId).sendMessage("https://github.com/VladYagl/HEMEM2System")
            }

            command("vk-init") { context ->
                vkChannelId = context.channelId
                save()
                channel(context.channelId).sendMessage("OK [вк -> дискорд] здесь")
            }

            command("morda") { safe(channel(it.channelId)) { morda(it) } }


            command("img") { safe(channel(it.channelId)) { img(it) } }

            command("issues") { message ->
                safe(channel(message.channelId)) {
                    val req = gitHttpClient.get<String>(ISSUES) {
                        headers {
                            append("Accept", "application/vnd.github.v3+json")
                        }
                    }

                    channel(message.channelId).sendMessage((
                            klaxon.parseArray<Issue>(StringReader(req))?.map {
                                "> **${it.title}** | *${it.state}*\n" + (it.body?.let { body ->
                                    "$body\n"
                                } ?: "")
                            }?.joinToString("\n")
                                    ?: "ну бля! опять сломалось") + "https://github.com/VladYagl/HEMEM2System/issues")
                }
            }

            command("issue-add") { message ->
                safe(channel(message.channelId)) {
                    val req = gitHttpClient.post<HttpResponse>(ISSUES) {
                        headers {
                            append("Accept", "application/vnd.github.v3+json")
                            append("Authorization", "token $GITHUB_API_TOKEN")
                        }
                        body = """{"title":"${message.content.split(" ").drop(1).joinToString(" ")}"}"""
                    }

                    if (req.status == HttpStatusCode.Created) {
                        vkBot.addReaction("windchair", message)
                    }
                }
            }
        }
    }
}
