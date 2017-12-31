
import io.javalin.Context
import io.javalin.Handler
import org.apache.commons.io.IOUtils

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

object path2 : Handler {
    fun name(): String = javaClass.name
    override fun handle(ctx: Context?) {
        ctx?.result(name())
    }
}

fun doPath(ctx: Context) {
    ctx.result(::doPath.name)
}

object fileupload {
    fun name() = javaClass.name
    fun run(ctx: Context) {
        ctx.uploadedFiles("files").forEach {
            val content = IOUtils.toString(it.content, "UTF-8")
            ctx.html(content)
        }
    }
}