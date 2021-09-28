import com.beust.klaxon.Klaxon
import com.petersamokhin.vksdk.core.client.VkApiClient
import com.petersamokhin.vksdk.core.http.paramsOf
import com.petersamokhin.vksdk.http.VkKtorHttpClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.logging.*
import kotlinx.coroutines.Dispatchers
import java.io.StringReader

private val klaxon = Klaxon()

data class Size(
        val url: String,
        val width: Int,
        val height: Int
)

data class PhotoSize(
        val type: String,
        val url: String,
        val width: Int,
        val height: Int
)

data class Photo(
        val id: Int,
        val sizes: List<PhotoSize>
)

data class Video(
        val id: Int,
        val firstFrame: List<Size>? = null,
        val image: List<Size>
)

data class Attachment(
        val type: String,
        val photo: Photo? = null,
        val video: Video? = null
)

data class Post(
        val id: Int,
        val owner_id: Int,
        val from_id: Int,
        val created_by: Int? = null,
        val date: Int,
        val text: String,
        val reply_owner_id: Int? = null,
        val attachments: List<Attachment>? = null,
        val is_pinned: Int? = null
)

suspend fun VkApiClient.lastPost(): Post? {
    val req = this.call("wall.get", paramsOf("owner_id" to -MORDA_ID, "count" to 10)).execute()
    val obj = klaxon
            .parseJsonObject(StringReader(req))
            .obj("response")
            ?.array<Post>("items")
            ?.let {
                klaxon.parseFromJsonArray<Post>(it)?.filter { post -> post.is_pinned == null }
            }
    return obj?.first()
}

fun httpClientWithLog(): VkKtorHttpClient {
    return VkKtorHttpClient(Dispatchers.Default, overrideClient = HttpClient(CIO) {
        engine {
            // because of long polling:
            // in case of zero events, requests will last 25 sec each
            requestTimeout = 30_000
        }
        Logging {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
        }
    })
}
