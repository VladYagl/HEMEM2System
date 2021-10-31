import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.system.exitProcess

private val attFile = File("data.att")
private val lastFile = File("data.last")
private val boysFile = File("data.boys")
private val vkFile = File("data.vk")

private fun save(file: File, obj: Any?) {
    print(runCatching {
        ObjectOutputStream(file.outputStream()).use { stream ->
            stream.writeObject(obj)
        }
    })
    print(", ")
}

@Suppress("UNCHECKED_CAST")
private fun <T> load(file: File, default: T): T {
    var obj: T = default
    print(runCatching {
        ObjectInputStream(file.inputStream()).use { stream ->
            obj = stream.readObject() as T
        }
    })
    print(", ")
    return obj
}

fun save() {
    save(attFile, attachments)
    save(lastFile, lastMessage)
    save(boysFile, boys)
    save(vkFile, vkChannelId)
    println()
}

fun load() {
    attachments = load(attFile, attachments)
    lastMessage = load(lastFile, lastMessage)
    boys = load(boysFile, boys)
    vkChannelId = load(vkFile, vkChannelId)
    println()

    for (boy in boys) {
        println(boy)
    }
}
