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
import com.example.signing.util.AlgorithmSelector.DefaultProviderType
import java.io.IOException

/**
 * Служебный класс ProviderType предназначен
 * для загрузки/сохранения номера типа провайдера
 * в файл. Используется только в demo-приложении.
 *
 * 09/12/2013
 *
 */
class ProviderType
/**
 * Конструктор.
 *
 * @param context Контекст приложения.
 * @throws IOException
 */
private constructor(context: Context) :
    ArrayResourceSelector(context, PROVIDER_TYPE_RESOURCE_NAME) {
    companion object {
        /**
         * Название ресурса с типами провайдеров.
         */
        private const val PROVIDER_TYPE_RESOURCE_NAME = "providerTypes"

        /**
         * Объект для управления загрузки/сохранения
         * активного типа провайдера.
         *
         */
        private var providerType_: ProviderType? = null

        /**
         * Проверка инициализации.
         */
        private var initiated = false

        /**
         * Инициализация объекта для работы с типами
         * провайдеров.
         *
         * @param context Контекст приложения.
         */
        fun init(context: Context) {
            if (!initiated) {
                try {
                    providerType_ = ProviderType(context)
                    initiated = true
                } catch (e: IOException) {
                }
            } // if
        }

        /**
         * Получение активного типа провайдера.
         *
         * @return Тип провайдера.
         */
        fun currentProviderType(): DefaultProviderType? {
            if (!initiated) {
                return DefaultProviderType.pt2001
            } // if
            val `val` = currentType()
            val providerTypesList = providerType_?.resourceAvailableValues?.toList()
            val position = providerTypesList?.indexOf(`val`)
            return position?.let { AlgorithmSelector.find(it) }
        }

        /**
         * Получение активного типа провайдера.
         *
         * @return Тип провайдера.
         */
        fun currentType(): String {
            return if (!initiated) {
                "" // context == null
            } else providerType_!!.currentValue() // if
        }

        /**
         * Сохранение типа провайдера в файл.
         *
         * @param type Тип провайдера.
         * @return True в случае успешного сохранения.
         */
        fun saveCurrentType(type: String?): Boolean {
            return if (initiated) {
                providerType_!!.saveValue(type!!)
            } else false // if
        }
    }
}