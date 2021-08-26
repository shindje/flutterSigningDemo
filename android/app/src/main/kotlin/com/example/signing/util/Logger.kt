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

import android.content.res.Resources
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import com.example.signing.R
import ru.CryptoPro.JCP.tools.Encoder

/**
 * Служебный класс LogCallback предназначен
 * для записи в поле сообщений и установки
 * статуса.
 *
 * 30/05/2013
 *
 */
class Logger private constructor(
    resources: Resources,
    log: EditText, status: TextView
) {
    /**
     * Поле для записи.
     */
    private var logger: EditText? = null

    /**
     * Поле для статуса.
     */
    private var showStatus: TextView? = null

    /**
     * Описание статуса.
     */
    private var statusFieldValue: String? = null
    private var statusUnknown: String? = null
    private var statusOK: String? = null
    private var statusFailed: String? = null

    /**
     * Запись сообщения в поле.
     *
     * @param message Сообщение.
     */
    @Synchronized
    private fun internalLog(message: String) {
        if (logger != null) {
            logger!!.post(Runnable { logger!!.append("\n" + message) })
        } // if
        else {
            Log.i(Constants.APP_LOGGER_TAG, message)
        } // else
    }

    /**
     * Очистка поля.
     */
    @Synchronized
    private fun internalClear() {
        if (logger != null) {
            logger!!.post(Runnable { logger!!.setText("") })
        } // if
        if (showStatus != null) {
            showStatus!!.post(Runnable {
                showStatus!!.setText(
                    statusFieldValue + ": " +
                            statusUnknown
                )
            })
        } // if
    }

    /**
     * Отображение строки статуса.
     *
     * @param status Строка статуса.
     */
    @Synchronized
    private fun setStatus(status: String?) {
        if (showStatus != null) {
            showStatus!!.post(Runnable {
                showStatus!!.setText(
                    (statusFieldValue +
                            ": " + status)
                )
            })
        } // if
    }

    companion object {
        /**
         * Экземпляр логгера.
         */
        private var INSTANCE: Logger? = null

        /**
         * Инициализация логгера.
         *
         * @param resources Ресурсы приложения.
         * @param log Графический объект для вывода лога.
         * @param status  Графический объект для вывода
         * статуса.
         */
        fun init(
            resources: Resources,
            log: EditText, status: TextView
        ) {
            INSTANCE = Logger(resources, log, status)
        }

        /**
         * Запись сообщения в поле.
         *
         * @param message Сообщение.
         */
        fun log(message: String) {
            if (INSTANCE != null) {
                INSTANCE!!.internalLog(message)
            }
        }

        /**
         * Запись сообщения в поле.
         *
         * @param message Сообщение.
         * @param base64 True, если нужно конвертировать
         * в base64.
         */
        fun log(message: ByteArray, base64: Boolean) {
            if (INSTANCE != null) {
                INSTANCE!!.internalLog(if (base64) toBase64(message) else String(message))
            }
        }

        /**
         * Конвертация в base64.
         *
         * @param data Исходные данные.
         * @return конвертированная строка.
         */
        private fun toBase64(data: ByteArray): String {
            val enc = Encoder()
            return enc.encode(data)
        }

        /**
         * Очистка поля.
         */
        fun clear() {
            if (INSTANCE != null) {
                INSTANCE!!.internalClear()
            }
        }

        /**
         * Задание статуса провала.
         *
         */
        fun setStatusFailed() {
            if (INSTANCE != null) {
                INSTANCE!!.setStatus(INSTANCE!!.statusFailed)
            }
        }

        /**
         * Задание статуса успеха.
         *
         */
        fun setStatusOK() {
            if (INSTANCE != null) {
                INSTANCE!!.setStatus(INSTANCE!!.statusOK)
            }
        }
    }

    /**
     * Конструктор.
     *
     * @param resources Ресурсы приложения.
     * @param log Графический объект для вывода лога.
     * @param status  Графический объект для вывода
     * статуса.
     */
    init {
        logger = log
        showStatus = status
        statusFieldValue = resources.getString(R.string.StatusField)
        statusUnknown = resources.getString(R.string.StatusUnknown)
        statusOK = resources.getString(R.string.StatusOK)
        statusFailed = resources.getString(R.string.StatusError)
    }
}