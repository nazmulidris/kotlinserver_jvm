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
        ctx.uploadedFiles("files").forEach {
            val csvString = IOUtils.toString(it.content, "UTF-8")
            ctx.html(process(csvString))
        }
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

            // Match each record to a category
            Category.values().forEach { category ->
                category.descriptionList.forEach { categoryDescription ->
                    if (description.contains(other = categoryDescription, ignoreCase = true)) {
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

        map.keys.forEach {
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
                append("<h1>")
                append(it)
                append(", ")
                append(categoryTotal)
                append("</h1>")
                append(recordBuffer)
            }

        }
        return buffer.toString()
    }

    enum class Category(val descriptionList: List<String>) {
        Restaurants(listOf("doordash", "LYFE KITCHEN", "COUPA", "LISAS TEA TIME LLC", "SQ")),
        Groceries(listOf("wholefds")),
        Chocolate(listOf("WWWVALRHONA")),
        RideShare(listOf("LYFT", "UBER")),
        Transportation(listOf("PORSCHE")),
        Household(listOf("amazon", "jet.com", "walmart", "UPS", "USPS")),
        Beauty(listOf("VIZAVOO")),
        Phone(listOf("VZWRLSS")),
        Movies(listOf("Amazon Video On Demand")),
        Books(listOf("Amazon Services-Kindle")),
        Music(listOf("GOOGLE *Google Music")),
        Health(listOf("GOOGLE *Massage", "GOOGLE WELLNESS CTR")),
        Internet(listOf("COMCAST CALIFORNIA")),
        TechSubscription(listOf("HEROKU", "github")),
        RetirementHome(listOf("TransferwiseCom_USD")),
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