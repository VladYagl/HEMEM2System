import com.jessecorbett.diskord.api.common.Message
import com.jessecorbett.diskord.bot.BotContext
import com.jessecorbett.diskord.util.sendMessage
import com.petersamokhin.vksdk.core.client.VkApiClient
import com.petersamokhin.vksdk.core.model.VkSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val MORDA_ID = 83276396

suspend fun BotContext.morda(message: Message) {
    val morda = VkApiClient(MORDA_ID, VK_API_KEY, VkApiClient.Type.Community, VkSettings(httpClient))

    var lastPostId = morda.lastPost()?.id

    channel(message.channelId).sendMessage("Ждем новых постов от Морды...")

    mainScope.launch(Dispatchers.Default) {
        while (true) {
            delay(200000)
//                        delay(2000)

            morda.lastPost()?.let { lastPost ->
                if (lastPost.id != lastPostId) {
                    lastPostId = lastPost.id
                    println(lastPost)

                    channel(message.channelId).sendMessage("Морда запостил!\n\n${lastPost.text}\n${
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