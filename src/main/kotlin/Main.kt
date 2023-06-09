import com.api.igdb.apicalypse.APICalypse
import com.api.igdb.apicalypse.Sort
import com.api.igdb.exceptions.RequestException
import com.api.igdb.request.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dto.GameInfo
import proto.*
import java.io.File
import java.util.*

fun mergeJsonLists(jsonList1: String, jsonList2: String, prioritizeFirst: List<String>, idColumn: String): String {
    val mapper = ObjectMapper().registerKotlinModule()
    val list1 = mapper.readValue<List<LinkedHashMap<String, Any>>>(jsonList1)
    val list2 = mapper.readValue<List<LinkedHashMap<String, Any>>>(jsonList2)

    val mergedList = mutableListOf<Map<String, Any>>()

    // Merge items by ID column
    val map1 = list1.associateBy { it[idColumn] }
    val map2 = list2.associateBy { it[idColumn] }
    val ids = (map1.keys + map2.keys).distinct()
    for (id in ids) {
        val item1 = map1[id]
        val item2 = map2[id]
        val mergedItem = LinkedHashMap<String, Any>()
        if(item1 != null) {
            if(item1[idColumn] != null)
            {
                mergedItem[idColumn] = item1[idColumn] as Any
            }

            item1.keys.filter { it != idColumn && prioritizeFirst.contains(it) }.forEach { key ->
                mergedItem[key] = item1[key]!!
            }
        }

        item2?.keys?.filter { !prioritizeFirst.contains(it) }?.forEach { key ->
            mergedItem[key] = item2[key]!!
        }

        mergedList.add(mergedItem)
    }

    return mapper.writeValueAsString(mergedList)
}

fun main(args: Array<String>) {
    val classLoader = Thread.currentThread().contextClassLoader
    val inputStream = classLoader.getResourceAsStream("application.properties")

    val properties = Properties()
    properties.load(inputStream)

    val clientId = properties.getProperty("igdb.client-id")
    val clientSecret = properties.getProperty("igdb.client-secret")
    val iconBaseUrl = properties.getProperty("igdb.icon.base-url")
    val gameIds = properties.getProperty("igdb.game.id").split(",").toList()
    val getArtwork = properties.getProperty("igdb.artwork")?.toBoolean() ?: false
    val imageSize = properties.getProperty("igdb.image.size")
    val resultGameListFile = properties.getProperty("game-list.file.result")
    val baseGameListFile = properties.getProperty("game-list.file.base")

    val token = TwitchAuthenticator.requestTwitchToken(clientId, clientSecret)
    IGDBWrapper.setCredentials(clientId, token!!.access_token)

    val gameInfos = mutableListOf<GameInfo>()

    gameIds.chunked(10).forEach { chunk ->
        for(game in games("id = (${chunk.joinToString("," )})")) {
            println(game)

            for(video in game.videosList){
                val videos = video(video.id)
                for(v in videos) {
                    println("$v")
                }
            }

            if(getArtwork) {
                for(artwork in game.artworksList){
                    val artworks = artwork(artwork.id)
                    for(artworkData in artworks) {
                        println("https://images.igdb.com/igdb/image/upload/t_original/${artworkData.imageId}.jpg")
                    }
                }
            }

            var coverData: Cover? = null
            val covers = cover(game.cover.id)
            for(cover in covers) {
                println("https://images.igdb.com/igdb/image/upload/${imageSize}/${cover.imageId}.jpg")
                coverData = cover
                break
            }

            val gameInfo = GameInfo(game.id, game.name,
                "https://images.igdb.com/igdb/image/upload/${imageSize}/${coverData?.imageId}.jpg",
                "${iconBaseUrl}/${coverData?.imageId}.png")

            gameInfos.add(gameInfo)
        }
    }

    val objectMapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
    val resultGameListJson = objectMapper.writeValueAsString(gameInfos)
    File(resultGameListFile).writeText(resultGameListJson)

    val baseGameListJson = File(baseGameListFile).readText()

    val final = mergeJsonLists(baseGameListJson, resultGameListJson, listOf("name"), "gameId")
    File(baseGameListFile).writeText(final)
}


fun video(id: Long): List<GameVideo> {
    val apiCalypse = APICalypse().fields("*").where("id = $id")
    return try{
        IGDBWrapper.gameVideos(apiCalypse)
    } catch(e: RequestException) {
        println(e.message)
        emptyList()
    } catch(e: RuntimeException) {
        println(e.message)
        emptyList()
    }
}

fun artwork(id: Long): List<Artwork> {
    val apiCalypse = APICalypse().fields("*").where("id = $id")
    return try{
        IGDBWrapper.artworks(apiCalypse)
    } catch(e: RequestException) {
        println(e.message)
        emptyList()
    } catch(e: RuntimeException) {
        println(e.message)
        emptyList()
    }
}

fun cover(id: Long): List<Cover> {
    val apiCalypse = APICalypse().fields("*").where("id = $id")
    return try{
        IGDBWrapper.covers(apiCalypse)
    } catch(e: RequestException) {
        println(e.message)
        emptyList()
    } catch(e: RuntimeException) {
        println(e.message)
        emptyList()
    }
}

fun search(query: String): List<Search>? {
    val apiCalypse = APICalypse().search(query).fields("*")
    return try{
        IGDBWrapper.search(apiCalypse)
    } catch(e: RequestException) {
        println(e.message)
        null
    } catch(e: RuntimeException) {
        println(e.message)
        null
    }
}

fun games(query: String) : List<Game> {
    val apiCalypse = APICalypse().fields("*").sort("release_dates.date", Sort.DESCENDING)
        .where(query)
    return try{
        IGDBWrapper.games(apiCalypse)

    } catch(e: RequestException) {
        println(e.message)
        emptyList()
    } catch(e: RuntimeException) {
        println(e.message)
        emptyList()
    }
}

fun game(id: Long) : Game? {
    val apiCalypse = APICalypse().fields("*").where("id = $id")
    return try{
        IGDBWrapper.games(apiCalypse)[0]
    } catch(e: RequestException) {
        println(e.message)
        null
    } catch(e: RuntimeException) {
        println(e.message)
        null
    }
}