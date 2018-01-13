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
import org.apache.commons.io.IOUtils
import java.io.StringReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
                    h1 { font-family: 'Google Sans', Arial, sans-serif; }
                    h2 { font-family: 'Google Sans', Arial, sans-serif; }
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
        // Process each line in the reader
        for (line in lines) {
            with(line) {
                recordList.add(
                        Record(
                                type = get(Headers.TYPE.id),
                                transDate = get(Headers.XACT.id).parseDate(),
                                postDate = get(Headers.POST.id).parseDate(),
                                description = get(Headers.DESC.id),
                                amount = get(Headers.AMT.id).parseFloat()
                        )
                )
            }
        }
        return recordList
    }

    fun transform(recordList: MutableList<Record>): MutableMap<Category, MutableList<Record>> {
        val map = mutableMapOf<Category, MutableList<Record>>()
        for (record in recordList) {
            val (type, transDate, postDate, description, amount) = record

            // Check to see if the record matches any of the Categories
            Category.values().forEach { category ->
                category.descriptionList.forEach { categoryDescription ->
                    if (description.contains(categoryDescription, true)) {
                        map.getOrPut(category) { mutableListOf() }.add(record)
                    }
                }
            }

            // If a record didn't match any of the categories, then add it to Unknown
            if (!map.any { it.value.any { it == record } })
                map.getOrPut(Category.Unknown) { mutableListOf() }.add(record)
        }
        return map
    }

    fun prettyPrint(map: MutableMap<Category, MutableList<Record>>): String {
        val buffer = StringBuilder()
        val totals = mutableMapOf<Category, Float>()

        map.keys.sorted().forEach {
            // every category

            var categoryTotal = 0f

            val recordBuffer = StringBuilder()
            map[it]?.forEach {
                // every record in a category
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
                append(categoryTotal)
                append("</h2>")
                append(recordBuffer)
            }

        }
        return buffer.toString()
    }

    enum class Category(val descriptionList: List<String>) {
        // Transportation
        Cars(listOf("PORSCHE")),
        RideShare(listOf("LYFT", "UBER")),

        // Household
        Household(
                listOf("Amazon.com", "AMAZON MKTPLACE PMTS", "jet.com", "walmart",
                        "UPS", "USPS", "CRATE &amp; BARREL")),

        // Services
        Phone(listOf("VZWRLSS")),
        Internet(listOf("COMCAST CALIFORNIA")),
        Utilities(listOf("CITY OF PALO ALTO UT")),

        // Food
        Groceries(listOf("wholefds", "WHOLEFOODS.COM")),
        Restaurants(
                listOf("doordash", "LYFE KITCHEN", "COUPA", "LISAS TEA TIME LLC",
                        "SQ", "YLP* SHOP@YELP.COM", "DARBAR INDIAN CUISINE")),
        Chocolate(listOf("WWWVALRHONA")),

        // Health
        Health(listOf("GOOGLE *Massage", "GOOGLE WELLNESS CTR")),

        // Education
        Books(listOf("Amazon Services-Kindle")),

        // Entertainment
        Music(listOf("GOOGLE *Google Music")),
        Movies(listOf("Amazon Video On Demand")),

        // I
        TechSubscription(listOf("HEROKU", "github", "ADOBE *PHOTOGPHY PLAN")),

        // Grooming
        Beauty(
                listOf("NORDSTROM DIRECT #0808", "MACYS", "CVS/PHARMACY #09915",
                        "MADISON REED", "VIZAVOO", "ETSY.COM")),

        // Other
        RetirementHome(listOf("TransferwiseCom_USD")),

        // Unknown
        Unknown(listOf())
    }

    enum class Headers(val id: String) {
        TYPE("Type"),
        XACT("Trans Date"),
        POST("Post Date"),
        DESC("Description"),
        AMT("Amount")
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