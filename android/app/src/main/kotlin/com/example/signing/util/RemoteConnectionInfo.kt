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

import java.net.MalformedURLException
import java.net.URL

/**
 * Класс шаблона для группировки свойств удаленного хоста.
 *
 * @author 2014/01/24
 */
class RemoteConnectionInfo(
    ha: String?, hp: Int,
    upg: String?, ca: Boolean
) {
    /**
     * Получение адреса хоста.
     *
     * @return адрес хоста.
     */
    /**
     * Хост.
     */
    var hostAddress: String? = null
    /**
     * Получение порта.
     *
     * @return порт.
     */
    /**
     * Порт.
     */
    var hostPort = 0
    /**
     * Получение страницы.
     *
     * @return страница.
     */
    /**
     * Страница.
     */
    var hostPage: String? = null
    /**
     * Проверка использования client auth.
     *
     * @return true, если используем.
     */
    /**
     * Использование client auth.
     */
    var isUseClientAuth = false

    /**
     * Получение полного URL.
     *
     * @return url ресурса.
     */
    fun toUrl(): String? {
        var url: URL? = null
        try {
            url = URL(
                "https", hostAddress,
                hostPort, "/$hostPage"
            )
        } catch (e: MalformedURLException) {
            // ignore
        }
        return url?.toString()
    }

    /**
     * Вывод инофрмации о хосте.
     *
     */
    fun print() {
        Logger.log(
            """
                Remote host: $hostAddress:$hostPort
                Page: $hostPage
                [client auth: $isUseClientAuth]
                """.trimIndent()
        )
    }

    companion object {
        /**
         * Https-порт по умолчанию.
         */
        private const val DEFAULT_PORT = 443

        /**
         * Удаленный хост, не поддерживающий новую cipher suite
         * (только ГОСТ 2001), без клиентской аутентификации.
         */
        val host2001NoAuth = RemoteConnectionInfo(
            "tlsgost-2001.cryptopro.ru",
            DEFAULT_PORT, "index.html", false
        )

        /**
         * Удаленный хост, не поддерживающий новую cipher suite
         * (только ГОСТ 2001), с клиентской аутентификацией.
         */
        val host2001ClientAuth = RemoteConnectionInfo(
            "tlsgost-2001auth.cryptopro.ru",
            DEFAULT_PORT, "index.html", true
        )

        /**
         * Удаленный хост, поддерживающий новую cipher suite
         * (ГОСТ 2001, ГОСТ 2012), короткий хеш ГОСТ 2012, без
         * клиентской аутентификации.
         */
        val host2012256NoAuth = RemoteConnectionInfo(
            "tlsgost-256.cryptopro.ru",
            DEFAULT_PORT, "index.html", false
        )

        /**
         * Удаленный хост, поддерживающий новую cipher suite
         * (ГОСТ 2001, ГОСТ 2012), короткий хеш ГОСТ 2012, с
         * клиентской аутентификацией.
         */
        val host2012256ClientAuth = RemoteConnectionInfo(
            "tlsgost-256auth.cryptopro.ru",
            DEFAULT_PORT, "index.html", true
        )

        /**
         * Удаленный хост, поддерживающий новую cipher suite
         * (ГОСТ 2001, ГОСТ 2012), длинный хеш ГОСТ 2012, без
         * клиентской аутентификации.
         */
        val host2012512NoAuth = RemoteConnectionInfo(
            "tlsgost-512.cryptopro.ru",
            DEFAULT_PORT, "index.html", false
        )

        /**
         * Удаленный хост, поддерживающий новую cipher suite
         * (ГОСТ 2001, ГОСТ 2012), длинный хеш ГОСТ 2012, с
         * клиентской аутентификацией.
         */
        val host2012512ClientAuth = RemoteConnectionInfo(
            "tlsgost-512auth.cryptopro.ru",
            DEFAULT_PORT, "index.html", true
        )
    }

    /**
     * Конструктор.
     *
     * @param ha Удаленный сервер.
     * @param hp Порт.
     * @param upg Страница.
     * @param ca True, если используется client auth.
     */
    init {
        hostAddress = ha
        hostPort = hp
        hostPage = upg
        isUseClientAuth = ca
    }
}