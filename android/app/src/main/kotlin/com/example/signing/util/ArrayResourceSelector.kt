/**
 * Copyright 2004-2013 Crypto-Pro. All rights reserved.
 * Программный код, содержащийся в этом файле, предназначен
 * для целей обучения. Может быть скопирован или модифицирован
 * при условии сохранения абзацев с указанием авторства и прав.
 *
 * Данный код не может быть непосредственно использован
 * для защиты информации. Компания Крипто-Про не несет никакой
 * ответственности за функционирование этого кода.
 */
package com.example.signing.util

import android.content.Context
import android.util.Log
import java.io.*
import java.util.*

/**
 * Служебный класс ArrayResourceSelector предназначен
 * для сохранения в файл выбранного из списка значения
 * и доступа к нему.
 *
 * 13/09/2013
 *
 */
open class ArrayResourceSelector {
    /**
     * Файл с настройкой.
     */
    private var resourceFile: File? = null

    /**
     * Содержимое файла с параметрами.
     */
    private var properties: Properties? = null

    /**
     * Список значений ресурса.
     */
    protected var resourceAvailableValues: Array<String>? = null

    /**
     * Конструктор.
     *
     * @param context Контекст приложения.
     * @param name Имя ресурса.
     * @throws IOException
     */
    constructor(context: Context, name: String) {

        // Извлекаем идентификатор ресурса, а не используем
        // его напрямую из xml, т.к. ресурсы могут принадлежать
        // разным приложениям.
        val resources = context.resources
        val resourceId = resources.getIdentifier(
            name,
            "array", context.packageName
        )
        resourceAvailableValues = context.resources
            .getStringArray(resourceId)

        // Файл с активным элементом списка.
        resourceFile = File(context.filesDir, "$name.prop")
        try {
            if (!resourceFile!!.exists() && !resourceFile!!.createNewFile()) {
                throw IOException("Couldn't create file: $name")
            } // if
            val propertiesInput = FileInputStream(resourceFile)
            properties = Properties()
            properties!!.load(propertiesInput)
            propertiesInput.close()
        } catch (e: IOException) {
            Log.e(Constants.APP_LOGGER_TAG, e.message, e)
            throw e
        }
    }

    /**
     * Конструктор.
     * Используется в [KeyStoreType].
     *
     * @param context Контекст приложения.
     * @param name Имя файла для сохранения.
     * @param keyStoreTypeList Список типов контейнеров.
     * @throws IOException
     */
    constructor(
        context: Context, name: String,
        keyStoreTypeList: List<String>
    ) {
        resourceAvailableValues = keyStoreTypeList.toTypedArray()

        // Файл с активным элементом списка.
        resourceFile = File(context.filesDir, "$name.prop")
        try {
            if (!resourceFile!!.exists() && !resourceFile!!.createNewFile()) {
                throw IOException("Couldn't create file: $name")
            } // if
            val propertiesInput = FileInputStream(resourceFile)
            properties = Properties()
            properties!!.load(propertiesInput)
            propertiesInput.close()
        } catch (e: IOException) {
            Log.e(Constants.APP_LOGGER_TAG, e.message, e)
            throw e
        }
    }

    /**
     * Получение текущего активного значения списка или
     * значения по умолчанию.
     *
     * @return активное значение.
     */
    fun currentValue(): String {
        val currentValue = properties!!.getProperty(CURRENT_VALUE_ID)
        return currentValue ?: resourceAvailableValues!![0]
    }

    /**
     * Сохранение выбранного в списке значения в файл.
     *
     * @param value Сохраняемое значение.
     * @return True в случае успешного сохранения.
     */
    fun saveValue(value: String): Boolean {
        try {
            properties!![CURRENT_VALUE_ID] = value
            val resourceOutput: OutputStream = FileOutputStream(resourceFile)
            properties!!.store(resourceOutput, null)
            resourceOutput.close()
            return true
        } catch (e: IOException) {
            Log.e(Constants.APP_LOGGER_TAG, e.message, e)
        }
        return false
    }

    companion object {
        /**
         * Параметр, описывающий выбранный
         * элемент массива.
         */
        private const val CURRENT_VALUE_ID = "CurrentValue"
    }
}