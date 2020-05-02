The Binding of Isaac: Four Souls Card Downloader

Created in Kotlin with IntelliJ IDEA.
Dependencies: Google GSON, JSoup.

This simple tool allows you to automatically download card pictures from foursoulsspoiler.com, neatly organized based on the categories of the aforementioned website and cropping them down to the size 437x609, i.e. removing the surronding transparent border.

USAGE:
java -jar FourSoulsDownloader.jar STARTID ENDID

Where STARTID and ENDID are the extremes of the range of card IDs you want to check on the website. If you fail to provide one or both, the tool will automatically use the currently known extremes of the range of all existing cards at the time I'm writing this. These extremes are 70 and 790.

The tool operates by first fetching all card names, categories and picture urls from the website, saving each of them in cards.json, then downloads each picture to FOURSOULS_DOWNLOADS/NAME OF CATEGORY, then crops all the pictures.

If the tool is run a second time, it will not check for the ID again if it's able to find the card in cards.json. It will also not download pictures already present in the downloads folder, nor will it try to crop pictures if it figures they're already cropped. This means the tool should be able to resume from where it stopped, if it gets halted. However, the IDs where no cards where found on the first run will still be checked.

The range provided with the parameters is only relevant for the fetching phase, so if you happen to have an up-to-date cards.json ready, you can pass 0 0 as the range of cards to check and it will go straight to the download and cropping phases, saving about 5 minutes.

You can modify, redistribute and/or use this tool and/or its respective source code in your projects as long as you give credit. Do not redisitribute the pictures you download with this tool without explicit authorization from whomever holds their copyright.
