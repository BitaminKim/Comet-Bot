/*
 * Copyright (c) 2022- Sorapointa
 *
 * 此源代码的使用受 Apache-2.0 许可证约束, 欲阅读此许可证, 可在以下链接查看.
 * Use of this source code is governed by the Apache-2.0 License which can be found through the following link.
 *
 * https://github.com/Sorapointa/Sorapointa/blob/master/LICENSE
 */

package ren.natsuyuk1.comet.api.config.provider

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import ren.natsuyuk1.comet.utils.coroutine.ModuleScope
import ren.natsuyuk1.comet.utils.file.readTextBuffered
import ren.natsuyuk1.comet.utils.file.touch
import ren.natsuyuk1.comet.utils.file.writeTextBuffered
import java.io.File
import kotlin.reflect.full.createType

private val logger = mu.KotlinLogging.logger { }

open class PersistDataFile<T : Any>(
    final override val file: File,
    defaultValue: T,
    override val format: StringFormat = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    },
    final override val scope: CoroutineScope =
        ModuleScope("DataFilePersist", dispatcher = Dispatchers.IO)
) : PersistFile<T> {
    private val clazz = defaultValue::class

    /**
     * The serializer of data
     */
    private val serializer: KSerializer<Any?> = serializer(clazz.createType())

    private val mutex = Mutex()

    @Suppress("PropertyName")
    val _data = atomic(defaultValue)

    final override val data: T by _data

    init {
        clazz.requireSerializable()
    }

    /**
     * Modify current [PersistDataFile] data
     */
    inline fun updateData(update: (T) -> T) = _data.update(update)

    override suspend fun init(): Unit =
        withContext(scope.coroutineContext) {
            load()
        }

    suspend fun initAndLoad(): T =
        withContext(scope.coroutineContext) {
            load()
        }

    /**
     * Save [PersistDataFile]
     *
     * @param saveData saved data
     */
    override suspend fun save(saveData: T) = mutex.withLock {
        withContext(scope.coroutineContext) {
            logger.debug { "Saving data $saveData" }
            file.touch()
            file.writeTextBuffered(format.encodeToString(serializer, saveData))
        }
    }

    /**
     * Load [PersistDataFile]
     */
    override suspend fun load(): T {
        return withContext(scope.coroutineContext) {
            if (!file.exists()) {
                logger.debug { " ${file.absolutePath} does not exist, creating new default config..." }
                save(data)
            }
            mutex.withLock {
                val json = file.readTextBuffered()
                val t = (
                    format.decodeFromString(serializer, json) as? T
                        ?: error("Failed to cast Any? to ${clazz.simpleName}")
                    )
                updateData { t }
                t.also {
                    logger.debug { "Loaded data: ${it::class.simpleName}" }
                    logger.trace { "Data content $it" }
                }
            }
        }
    }
}