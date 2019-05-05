/*
 * Copyright 2017 Nazmul Idris All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.javalin.Context
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.apache.commons.io.IOUtils
import java.io.StringReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

object fileupload {
    fun name() = javaClass.name
    fun run(ctx: Context) {
        val sb = StringBuilder()
        sb.append("""
            <html>
            <head>
                <title>Spend Analysis</title>
                <link href="https://fonts.googleapis.com/css?family=Google+Sans" rel="stylesheet">
                <style>
                    body { font-family: 'Google Sans', Arial, sans-serif; }
                </style>
            <head/>
            <body>
            """)
        for ((idx, file) in ctx.uploadedFiles("files").withIndex()) {
            val csvString = IOUtils.toString(file.content, "UTF-8")
            sb.append("<h1>File #$idx : ${file.name}</h1>")
            sb.append(process(csvString))
        }
        sb.append("</body></html>")
        ctx.html(sb.toString())
    }

    fun process(csvString: String): String {
        return prettyPrint(transform(parse(csvString)))
    }

    fun parse(csvString: String): MutableList<Record> {
        val reader = StringReader(csvString)
        val lines = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(reader)
        val recordList = mutableListOf<Record>()
        // Process each line in the reader.
        for (line in lines) {
            with(line) {
                val record = Record(
                        type = get(Headers.TYPE.id),
                        transDate = get(getDateFieldName(line)).parseDate(),
                        postDate = get(Headers.POST.id).parseDate(),
                        description = get(Headers.DESC.id),
                        amount = get(Headers.AMT.id).parseFloat()
                )
                // Don't include "Payment" record types into the analysis, ignore them.
                if (record.type != "Payment") recordList.add(record)
            }
        }
        return recordList
    }

    fun transform(recordList: MutableList<Record>): MutableMap<Category, MutableList<Record>> {
        val map = mutableMapOf<Category, MutableList<Record>>()
        for (record in recordList) {
            val (type, transDate, postDate, description, amount) = record

            // Check to see if the record matches any of the Categories.
            Category.values().forEach { category ->
                category.descriptionList.forEach { categoryDescription ->
                    if (description.contains(categoryDescription, true)) {
                        map.getOrPut(category) { mutableListOf() }.add(record)
                    }
                }
            }

            // If a record didn't match any of the categories, then add it to Uncategorised.
            if (!map.any { it.value.any { it == record } })
                map.getOrPut(Category.Uncategorised) { mutableListOf() }.add(
                        record)
        }
        return map
    }

    fun prettyPrint(map: MutableMap<Category, MutableList<Record>>): String {
        val buffer = StringBuilder()
        val totals = mutableMapOf<Category, Float>()

        map.keys.sorted().forEach {
            // Every category.

            var categoryTotal = 0f

            val recordBuffer = StringBuilder()
            map[it]?.forEach {
                // Every record in a category.
                categoryTotal += it.amount
                with(recordBuffer) {
                    val highlightColor = when (it.type) {
                        "Sale" -> "#ff8c00"
                        "Payment" -> "#006994"
                        else -> "#3cb371"
                    }
                    append("""<span style="color:$highlightColor">${it.type}</span>""")
                    append(", ${it.transDate}")
                    append(", ${it.amount}")
                    append(", ${it.description}")
                    append("<br/>")
                }
            }

            totals.getOrPut(it) { categoryTotal }

            with(buffer) {
                append("<h2>")
                append(it)
                append(", ")
                append(categoryTotal.roundToInt() * -1)
                append("</h2>")
                append(recordBuffer)
            }

        }
        return buffer.toString()
    }

    enum class Category(val descriptionList: List<String>) {

        Cars(listOf("DETAIL PLUS",
                    "PORSCHE",
                    "HEYER PERFORMANCE",
                    "THE TOLL ROADS",
                    "GEICO",
                    "CARMAX",
                    "SFMTA 5TH &amp; MISSION GARA",
                    "CUSTOM ALIGNMENT",
                    "THE TIRE RACK",
                    "OG RACING AAR RACING GE",
                    "IN *ALEKSHOP",
                    "MODDERMAN SERVICE INC",
                    "STATE OF CALIF DMV INT")),
        Gas(listOf("THUNDERHILL PARK",
                   "MENLO PARK BEACON",
                   "SHELL OIL",
                   "CHEVRON",
                   "GAS",
                   "ABM ONSITE MARSHALL",
                   "LAKEWOOD VALERO",
                   "SALAH SINCLAIR",
                   "76 - PATRICK POUNDERS",
                   "7-ELEVEN 33011",
                   "76 - DBA GREG GALATOLO",
                    "EXXONMOBIL")),
        RideShare(listOf("LYFT",
                         "UBER",
                         "BART-DALY CITY QPS",
                         "ENTERPRISE RENT-A-CAR",
                         "CALTRAIN 1010 HILLSDALE",
                         "CSJ MKT &amp; S PEDRO GARAGE",
                         "BART-DALY CITY     QPS",
                         "ALASKA AIR",
                         "JETBLUE",
                         "SFO PARKINGCENTRA",
                         "HERTZ RENT-A-CAR")),

        Household(
                listOf("Amazon.com",
                       "AMAZON MKTPLACE PMTS",
                       "AMZN Mktp",
                       "jet.com",
                       "walmart",
                       "UPS",
                       "USPS",
                       "BedBathBeyond",
                       "WAL-MART",
                       "CVS/PHARMACY",
                       "TARGET",
                       "STAPLES",
                       "IKEA.COM",
                       "WWW.KOHLS.COM",
                       "JOANN STORES",
                       "THE HOME DEPOT",
                       "IKEA EAST PALO ALTO",
                       "Amazon Prime",
                       "TASKRABBIT",
                       "ROOTCANDLES.COM",
                       "WEST ELM E-COMMERCE",
                       "MACYS",
                       "ART.COM/ALLPOSTERS.COM",
                       "POTTERY BARN",
                       "CRATE&amp;BARREL AND CB2",
                       "CRATE &amp; BARREL",
                       "WALGREENS",
                       "MOO.COM",
                       "BLOOMINGDALES  STANFORD",
                       "YELPINC*DEALCALIFORNIA",
                       "APPFEE*PARK SQUARE APA",
                       "AMZ*UCA7-SanJose-Prime",
                       "APPFEE*THE SHADOWS",
                       "CALIFORNIA LOYAL MOVER",
                       "BED BATH &amp; BEYOND",
                        "T J MAXX",
                        "KeepCup",
                        "EZ PRINTS HOLDINGS INC",
                        "W.S. BADGER CO")),

        Utilities(listOf("CITY OF PALO ALTO UT",
                         "VZWRLSS",
                         "COMCAST CALIFORNIA")),

        Groceries(listOf("LE BELGE CHOCOLATIER",
                         "wholefds",
                         "WHOLEFOODS.COM",
                         "AMZ*WholeFoodsSTC10267",
                         "TRADER JOE",
                         "Amazon Prime Now",
                         "PrimeNowMktp",
                         "Prime Now",
                         "Amazon Prime Now Tips",
                         "SAFEWAY",
                         "NIJIYA MARKET",
                         "PrimeNowTips",
                         "DITTMERS",
                         "INSTACART",
                         "WWWVALRHONA",
                         "PALM SPRINGS 0445A",
                         "NEWSEXPRESSST2619",
                         "EZ PRINTS HOLDINGS")),
        Restaurants(
                listOf("HANAHAUS RESERVATION",
                       "OKASHI FUSION",
                       "SQ *CAVIAR",
                       "BLUE BOTTLE COFFEE",
                       "doordash",
                       "LYFE KITCHEN",
                       "COUPA",
                       "LISAS TEA TIME LLC",
                       "YLP* SHOP@YELP.COM",
                       "DARBAR INDIAN CUISINE",
                       "ROAM SAN MATEO",
                       "FUKI SUSHI",
                       "STARBUCKS",
                       "GRUBHUB",
                       "AD HOC",
                       "CHAAT BHAVAN",
                       "CAFE VENETIA",
                       "CHROMATIC COFFEE",
                       "CAFE SPROUT",
                       "RANGOON RUBY",
                       "LOCAL UNION 271",
                       "ORENS HUMMUS",
                       "TEASPOON",
                       "CASCAL",
                       "SCOUT COFFEE",
                       "GOSHI JAPANESE RESTAURANT",
                       "BIG SKY CAFE",
                       "COCOHODO SUNNYVALE",
                       "WWW.TOMMY-THAI.COM",
                       "SOGONG DONG TOFU HOUSE",
                       "YAKKO JAPANESE RESTAURANT",
                       "SQ *SCOOP MICROCREAMERY",
                       "ALEXANDER'S PATISSERIE",
                       "SQ *FALAFEL STOP",
                       "GOCHI JAPANESE TAPAS",
                       "SQ *SULTANA MEDITERRANEAN",
                       "THE COFFEE BEAN &amp; TEA LEA",
                       "SQ *GELATAIO",
                       "STEAM RESTAURANT",
                       "INDIAN STREET CAFE",
                       "SLIDERBAR CAFE",
                       "NAPA VALLEY COFFEE ROAST",
                       "SQ *RITUAL COFFEE ROASTER",
                       "NAPA NOODLES",
                       "THE COFFEE BEAN & TEA LEA",
                       "SQ *GO FISH POKE BAR - WE",
                       "EAT24 *SULTANA MEDITER",
                       "TRUE FOOD KITCHEN",
                       "Equator Coffees",
                       "PERRY'S ON MAGNOLIA",
                       "STATION HOUSE CAFE",
                       "REVERE COFFEE &amp; TEA",
                       "1 OZ COFFEE")),

        Recreation(listOf("HILTON GARDEN INN",
                          "TICKLE PINK INN",
                          "NAZARETH ICE OASIS",
                          "THE FIGURE SKATING PRO SH",
                          "SAN LUIS CREEK LODGE",
                          "THE ESTATE YOUNTVILLE",
                          "SIMRACEWAY PERFORMANCE",
                          "BAY AREA GUN VAULT",
                          "WATERCOURSE WAY",
                          "BAYAREADRIVINGACADEMY",
                          "HOLIDAY INN EXPRESS &amp; SU",
                          "SHARKS ICE AT SAN JOSE",
                          "MOTORSPORTREG.COM",
                          "HOOKED ON DRIVING",
                          "BMW PERFORMANCE CENTE",
                          "BEST WESTERN WILLOWS INN",
                          "EMBASSY SUITES",
                          "MVLA Union HS")),

        TechSubscription(listOf("HEROKU",
                                "github",
                                "ADOBE",
                                "JetBrains",
                                "MEETUP",
                                "Google Storage",
                                "GOOGLE *Dark Sky",
                                "INVISIONAPP",
                                "LUCID SOFTWARE INC",
                                "FS *Sketch",
                                "STUDIO MDS",
                                "CREATIVEMARKET.COM",
                                "FRAMER.COM",
                                "ESET WWW.ESET.COM",
                                "LINKEDIN",
                                "GOOGLE *YoWindow",
                                "SUBLIME HQ PTY LTD",
                                "GSUITE_fasterl",
                                "GSUITE R3BL.ORG",
                                "APL* ITUNES.COM",
                                "LASTPASS.COM",
                                "WORDPRESS",
                                "GOOGLE *VOICE",
                                "GOOGLE *Cgollner",
                                "GOOGLE *Domains",
                                "GOOGLE *WeatherBug",
                                "GOOGLE *Chrome Web Str")),

        IT(listOf("PADDLE.NET* ADGUARD",
                  "KINESIS CORPORATION",
                  "AMZ*Lenovo_USA",
                  "APL*APPLE ONLINE STORE",
                  "LENOVO GROUP")),

        Entertainment(listOf("Amazon Video On Demand",
                             "CINEMARK",
                             "GOOGLE *Google Play",
                             "HBO",
                             "GOOGLE*GOOGLE PLAY",
                             "AMC ONLINE",
                             "Amazon Digital Svcs",
                             "GOOGLE *Google Music",
                             "Prime Video")),

        Education(listOf("UDACITY",
                         "EB INTERSECT 2018",
                         "JOYCE THOM",
                         "HACKBRIGHT ACADEMY",
                         "UdemyUS",
                         "ACEABLE INC. ACEABLE C",
                         "Amazon Services-Kindle",
                         "Kindle Svcs",
                         "SAN FRANCISCO SCHOOL OF",
                         "BOSTYPE / WES BOS")),

        Health(listOf("GOOGLE *GOOGLE SERVICES",
                      "GOOGLE *Massage",
                      "GOOGLE WELLNESS CTR",
                      "*OSMENA PEARL",
                      "GOOGLE *SERVICES")),

        Beauty(
                listOf("NORDSTROM", "MADISON REED", "VIZAVOO", "ETSY.COM",
                       "UMBRELLA SALON", "EVANHEALY", "SQ *JENNY BARRY HAIR")),
        Clothing(listOf("Karen Millen",
                        "Fabric.com",
                        "7 FOR ALL MANKIND",
                        "BLUE NILE LLC",
                        "VIBRAM COMMERCE",
                        "SUNGLASS HUT",
                        "RENT THE RUNWAY",
                        "LUCKYBRAND.COM",
                        "LULULEMON",
                        "LULULEMONCOM",
                        "AMZ*Zappos.com",
                        "ZAP*ZAPPOS.COM",
                        "MAUI JIM/ZEAL OPTICS",
                        "LEVI'S STORE",
                        "LEVI'S",
                        "AMZ*RedBubble Inc",
                        "PANERAI BOUTIQUE LAJOLLA",
                        "WatchStyle",
                        "WWW.MOODFABRICS.COM",
                        "The Frye Company",
                        "ORIS")),

        Fees(listOf("PURCHASE INTEREST CHARGE")),
        Tax(listOf("TAX", "INCORPORATE.COM", "DANIEL P KENNEDY")),
        Legal(listOf("WWW.ITITRANSLATES.COM", "ALCORN IMMIGRATION LAW PC",
                     "ALCORN IMMIGRATIONLAW")),

        Uncategorised(listOf()),

        Donations(listOf("ARCHIVE.ORG",
                         "Wikimedia",
                         "GOOGLE *Donations CDP",
                         "PATREON* MEMBERSHIP",
                         "PATREON*PLEDGE",
                         "CKO*Patreon* Membership",
                         "TransferwiseCom_USD",
                         "COMMUNITY FOUNDATION OF N")),

    }

    enum class Headers(val id: String) {
        TYPE("Type"),
        DATE_NAME1("Trans Date"),
        DATE_NAME2("Transaction Date"),
        POST("Post Date"),
        DESC("Description"),
        AMT("Amount");
    }

    /**
     * Chase bank changed the transaction date field around Jan 2019. This allows CSV files
     * that were generated before Jan 2019 and after to be parsed successfully.
     */
    fun getDateFieldName(line: CSVRecord): String {
        return if (line.isMapped(Headers.DATE_NAME1.id)) Headers.DATE_NAME1.id
        else Headers.DATE_NAME2.id
    }

    data class Record(val type: String,
                      val transDate: LocalDate,
                      val postDate: LocalDate,
                      val description: String,
                      val amount: Float)

    fun String.parseDate(): LocalDate =
            LocalDate.parse(this, DateTimeFormatter.ofPattern("MM/dd/yyyy"))

    fun String.parseFloat(): Float = this.toFloat()
}