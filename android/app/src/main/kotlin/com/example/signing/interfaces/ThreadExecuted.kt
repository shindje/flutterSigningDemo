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
package com.example.signing.interfaces

import android.util.Log
import com.example.signing.base.FinalListener
import com.example.signing.util.Constants
import com.example.signing.util.Logger

/**
 * Служебный интерфейс ThreadExecuted предназначен
 * для выполнения задачи внутри потока. Обычно это
 * задача передачи или получения данных по сети
 * интернет.
 *
 * 29/05/2013
 *
 */
abstract class ThreadExecuted {
    /**
     * Обработчик события.
     */
    protected var finalListener: FinalListener? = null

    /**
     * Добавление обработчика события.
     *
     * @param listener Обработчик события.
     */
    fun addFinalListener(listener: FinalListener?) {
        finalListener = listener
    }

    /**
     * Метод для выполнения задачи в потоке.
     * Задача записывается внутри метода.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    protected abstract fun executeOne(data: ByteArray?): Any?

    /**
     * Метод для выполнения задачи в потоке.
     * Задача записана внутри метода.
     *
     */
    fun execute(data: ByteArray?) {
        try {
            val result = executeOne(data)
            Logger.setStatusOK()
            if (finalListener != null) {
                finalListener!!.onComplete(result)
            } // if
        } catch (e: Exception) {
            e.message?.let { Logger.log(it) }
            Logger.setStatusFailed()
            Log.e(Constants.APP_LOGGER_TAG, "Operation exception", e)
        }
    }
}