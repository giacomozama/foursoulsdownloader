/* You can modify, redistribute and/or use this tool and/or its respective source code in your projects
* as long as you give credit. Do not redisitribute the pictures you download with this tool without
* explicit authorization from whomever holds their copyright. */

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.jsoup.Jsoup
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO


data class Card(
    val id : Int,
    val name : String,
    val picUrl : String
)

data class CardCategory (
    val name : String,
    val cards : MutableList<Card>
)

data class CardCategoryCollection (
    val categories : MutableList<CardCategory>
)

fun main(args: Array<String>) {
    val idFrom : Int
    val idTo : Int

    if (args.count() < 2) {
        idFrom = 70
        idTo = 790
        println("No valid ID Range provided. Using default range: [$idFrom, $idTo]")
    } else {
        idFrom = args[0].toInt()
        idTo = args[1].toInt()
        if (idTo < idFrom) {
            println("ERROR: Range start must not be greater than range end")
            return
        }
    }

    lateinit var cardCategories : MutableList<CardCategory>
    lateinit var cardCategoryCollection : CardCategoryCollection

    fun tryGetCardFromPage(cardID : Int) : Boolean {
        return Jsoup.parse(Jsoup.connect("http://pop-life.com/foursouls/card.php?id=$cardID")
            .get()
            .outerHtml())
            .getElementsByClass("pageContent").first().let {
                val cat = it.getElementsByTag("h1").first().html().trim()
                if (cat.isNotEmpty()) {
                    val targetCat =
                        cardCategories.find { c -> c.name == cat }
                            ?: CardCategory(cat, mutableListOf()).let { c ->
                                cardCategories.add(c)
                                c
                            }
                    val name = it.getElementsByTag("h2").first().html().trim()
                    println("FOUND CARD $name")
                    val picUrl = it.getElementsByClass("img-responsive").first().attr("src").drop(1)
                    targetCat.cards.add(Card(cardID, name, "http://pop-life.com/foursouls$picUrl"))
                    true
                } else {
                    println("NO CARD FOUND")
                    false
                }
        }
    }

    println("Four Souls Card Downloader by Shivs")
    println("Will check from ID $idFrom to ID $idTo, export known card information, download and crop pictures.")

    val file = File("cards.json")
    val gson = Gson()
    if (file.exists()) {
        try {
            cardCategories =
                gson.fromJson(file.readText(),
                    CardCategoryCollection::class.java)
                    ?.categories?.toMutableList()
                    ?: mutableListOf<CardCategory>().let { file.writeText(""); it }
        }
        catch (e : JsonSyntaxException) {
            cardCategories = mutableListOf()
            file.writeText("")
        }
    } else {
        if (!file.createNewFile()) {
            println("ERROR: Couldn't create cards.json")
            return
        } else {
            cardCategories = mutableListOf()
        }
    }

    cardCategoryCollection = CardCategoryCollection(cardCategories)

    for (i in idFrom..idTo) {
        print("Checking if card with ID $i exists... ")
        val allCards = cardCategories.flatMap { it.cards }
        allCards.find { it.id == i }?.let {
            println("FOUND CARD ${it.name}")
        } ?: if (tryGetCardFromPage(i))
            file.writeText(gson.toJson(cardCategoryCollection))
    }

    var mainOutDir = File("FOURSOULS_DOWNLOADS")
    var i = 0
    while (true) {
        if (!mainOutDir.exists()) {
            mainOutDir.mkdir()
            break
        } else if (!mainOutDir.isDirectory) {
            mainOutDir = File("FOURSOULS_DOWNLOADS_${i++}")
        } else break
    }

    cardCategories.forEach {
        val dir = File("${mainOutDir.name}/${it.name}")
        if (!dir.exists())
            dir.mkdir()
        else if (!dir.isDirectory) {
            println("ERROR: Conflicting download folder structure")
            return
        }
    }

    println("\nFound ${cardCategories.flatMap{it.cards}.count()} cards in the given range. Starting download phase.\n")

    cardCategories.forEach {
        val fileList = File("${mainOutDir.name}/${it.name}").list()
        it.cards.forEach {c ->
            println("Downloading card ID ${c.id}: ${it.name}/${c.name}")
            try {
                if (!fileList?.contains("${c.name}.png")!!) {
                    BufferedInputStream(URL(c.picUrl.replace(" ", "%20")).openStream()).use { inputStream ->
                        FileOutputStream("${mainOutDir.name}/${it.name}/${c.name.replace(Regex("""[\\/:*?"<>|]"""), "_")}.png").use { fileOS ->
                            val data = ByteArray(1024)
                            var byteContent: Int
                            while (inputStream.read(data, 0, 1024).also { byteContent = it } != -1) {
                                fileOS.write(data, 0, byteContent)
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                println("ERROR: Couldn't download picture")
                return
            }
        }
    }

    println("\nDownload finished, starting crop phase.\n")

    mainOutDir.walkTopDown().filter{ !it.isDirectory }.forEach {
        println("CROPPING PICTURE ${it.name}")
        try {
            val img = ImageIO.read(it)
            if (img.width != 437) {
                val newWidth = 437
                val newHeight = 609
                val cropped = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
                var startFromY = 1
                var startFromX = 1
                while (Color(img.getRGB(img.width / 2, startFromY), true).alpha == 0) ++startFromY
                while (Color(img.getRGB(startFromX, img.height / 2), true).alpha == 0) ++startFromX
                for (y in 0 until newHeight) {
                    for (x in 0 until newWidth) {
                        cropped.setRGB(x, y, img.getRGB(startFromX + x, startFromY + y))
                    }
                }
                ImageIO.write(cropped, "png", it)
            }
        } catch (e : IOException) {
            println("ERROR: Couldn't crop picture")
            return
        }
    }

    println("\nCropping finished.\n")
    println("All done, enjoy.")
}