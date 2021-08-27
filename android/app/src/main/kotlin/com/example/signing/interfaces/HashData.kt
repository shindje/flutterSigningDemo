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

import com.example.signing.base.FinalListener

/**
 * Служебный интерфейс HashData предназначен для
 * релизации примеров работы с хешем.
 *
 * 27/05/2013
 *
 */
interface HashData {
    /**
     * Работа примера.
     *
     * @param listener Обработчик события завершения задачи.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun getResult(data: ByteArray?, listener: FinalListener?)

    companion object {
        /**
         * Максимальный таймаут ожидания чтения/записи клиентом
         * (мсек).
         */
        const val MAX_CLIENT_TIMEOUT = 60 * 60 * 1000

        /**
         * Максимальный таймаут ожидания завершения потока с примером
         * в случае использования интернета (мсек).
         */
        const val MAX_THREAD_TIMEOUT = 100 * 60 * 1000
    }
}