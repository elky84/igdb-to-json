import com.api.igdb.apicalypse.APICalypse
import com.api.igdb.apicalypse.Sort
import com.api.igdb.exceptions.RequestException
import com.api.igdb.request.*
import com.fasterxml.jackson.databind.ObjectMapper
import dto.GameInfo
import proto.*
import java.io.File
import java.util.*

fun main(args: Array<String>) {
    val classLoader = Thread.currentThread().contextClassLoader
    val inputStream = classLoader.getResourceAsStream("application.properties")

    val properties = Properties()
    properties.load(inputStream)

    val clientId = properties.getProperty("igdb.client-id")
    val clientSecret = properties.getProperty("igdb.client-secret")

    val token = TwitchAuthenticator.requestTwitchToken(clientId, clientSecret)
    IGDBWrapper.setCredentials(clientId, token!!.access_token)

    val gameInfos = mutableListOf<GameInfo>()

    for(game in games("id = (115, 120176, 126459, 27789, 125174, 135842)")) {
        println(game)

        for(video in game.videosList){
            val videos = video(video.id)
            for(v in videos) {
                println("$v")
            }
        }

        for(artwork in game.artworksList){
            val artworks = artwork(artwork.id)
            for(artworkData in artworks) {
                println("https://images.igdb.com/igdb/image/upload/t_original/${artworkData.imageId}.jpg")
            }
        }

        val covers = cover(game.cover.id)
        for(cover in covers) {
            println("https://images.igdb.com/igdb/image/upload/t_original/${cover.imageId}.jpg")
        }

        val gameInfo = GameInfo(game.id, game.name,
            "https://images.igdb.com/igdb/image/upload/t_original/${covers.first().imageId}.jpg",
            "/game/icon/${covers.first().imageId}.jpg")

        gameInfos.add(gameInfo)
    }

    File("game_list.json").writeText(ObjectMapper().writeValueAsString(gameInfos))
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