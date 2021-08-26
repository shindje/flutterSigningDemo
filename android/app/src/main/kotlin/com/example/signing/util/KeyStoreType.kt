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
import ru.CryptoPro.JCSP.JCSP
import java.io.IOException
import java.security.Provider
import java.util.*

/**
 * Служебный класс KeyStoreType предназначен
 * для загрузки/сохранения номера типа хранилища
 * в файл. Используется только в demo-приложении.
 *
 * 26/08/2013
 *
 */
class KeyStoreType
/**
 * Конструктор.
 *
 * @param context Контекст приложения.
 * @throws IOException
 */
private constructor(context: Context) :
    ArrayResourceSelector(context, KEY_TYPE_RESOURCE_NAME, keyStoreTypeList) {
    companion object {
        /**
         * Название файла для сохранения ресурсов.
         */
        private const val KEY_TYPE_RESOURCE_NAME = "keyStores"

        /**
         * Объект для управления загрузки/сохранения
         * активного типа контейнера.
         *
         */
        private var keyStoreType_: KeyStoreType? = null

        /**
         * Проверка инициализации.
         */
        private var initiated = false

        /**
         * Инициализация объекта для работы с типами
         * контейнеров.
         *
         * @param context Контекст приложения.
         */
        @Synchronized
        fun init(context: Context) {
            if (!initiated) {
                try {
                    keyStoreType_ = KeyStoreType(context)
                    initiated = true
                } catch (e: IOException) {
                }
            } // if
        }// if
        // for
        // Удалим его, чтобы...
        // поставить на 1 место.
        // А это - не тип контейнера.
        // Пока не поддерживается передача.
// Список типов контейнеров.
        /**
         * Получение списка поддерживаемых типов контейнеров.
         *
         * @return список типов.
         */
        val keyStoreTypeList: List<String>
            get() {
                val keyStoreTypeList: MutableList<String> = LinkedList()
                val services: Set<Provider.Service> = KeyStoreUtil.DEFAULT_PROVIDER.getServices()

                // Список типов контейнеров.
                for (service in services) {
                    if (service.type.equals("KeyStore", ignoreCase = true)) {
                        keyStoreTypeList.add(service.algorithm)
                    } // if
                } // for
                keyStoreTypeList.remove(JCSP.HD_STORE_NAME) // Удалим его, чтобы...
                keyStoreTypeList.add(0, JCSP.HD_STORE_NAME) // поставить на 1 место.
                keyStoreTypeList.remove(JCSP.CERT_STORE_NAME) // А это - не тип контейнера.
                keyStoreTypeList.remove(JCSP.PFX_STORE_NAME) // Пока не поддерживается передача.
                keyStoreTypeList.remove(JCSP.MY_STORE_NAME)
                keyStoreTypeList.remove(JCSP.ROOT_STORE_NAME)
                keyStoreTypeList.remove(JCSP.CA_STORE_NAME)
                keyStoreTypeList.remove(JCSP.ADDRESS_BOOK_STORE_NAME)
                keyStoreTypeList.remove(JCSP.FILE_STORE_NAME)
                keyStoreTypeList.remove(JCSP.SST_STORE_NAME)
                return keyStoreTypeList
            }

        /**
         * Получение активного типа хранилища.
         *
         * @return Тип хранилища.
         */
        fun currentType(): String {
            return if (!initiated) {
                JCSP.HD_STORE_NAME
            } else keyStoreType_!!.currentValue() // if
        }

        /**
         * Сохранение типа хранилища в файл.
         *
         * @param type Тип хранилища.
         * @return True в случае успешного сохранения.
         */
        fun saveCurrentType(type: String?): Boolean {
            return if (initiated) {
                keyStoreType_!!.saveValue(type!!)
            } else false // if
        }
    }
}