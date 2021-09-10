package com.example.signing.base

import com.example.signing.util.Constants
import com.example.signing.util.ContainerAdapter
import com.example.signing.util.KeyStoreType
import com.example.signing.util.Logger
import ru.CryptoPro.JCSP.JCSP
import ru.CryptoPro.ssl.Provider
import ru.CryptoPro.ssl.util.TLSContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

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


/**
 * Служебный класс TLSData предназначен для
 * реализации примеров соединения по TLS.
 *
 * 30/05/2013
 *
 */
abstract class TLSData(adapter: ContainerAdapter) : EncryptDecryptData(adapter) {
    /*
    * Создание SSL контекста.
    *
    * @param trustManagers Менеджеры хранилища доверенных
    * сертификатов. Для получения одного менеджера должен
    * быть передан массив по крайней мере из одного элемента,
    * в него будет помещен выбранный менеджер сертификатов.
    * Может быть null.
    * @return готовый SSL контекст.
    * @throws Exception.
    */
    /*
    * Создание SSL контекста.
    *
    * @return готовый SSL контекст.
    * @throws Exception.
    */
    @Throws(Exception::class)
    protected fun createSSLContext(trustManagers: Array<TrustManager?>? = null): SSLContext {
        containerAdapter!!.printConnectionInfo()
        Logger.log("Creating SSL context...")

        //
        // Для чтения(!) доверенного хранилища доступна
        // реализация CertStore из Java CSP. В ее случае
        // можно не использовать пароль.
        //
        val keyStoreType: String = KeyStoreType.currentType()
        Logger.log(
            "Initiating key store. Loading containers. " +
                    "Default container type: " + keyStoreType
        )

        //
        // @see {@link ru.CryptoPro.ACSPClientApp.client.example.base#TLSData}
        // @see {@link ru.CryptoPro.ACSPInClientApp.examples#HttpsExample}
        // @see {@link ru.CryptoPro.ssl.util#TLSContext}
        //
        // В данном примере используется только функции из "белого" списка
        // из класса TLSContext и в случае применения двухсторонней
        // аутентификации (2СА) при инициализации TLS задается явный алиаса
        // ключа keyAlias!
        //
        // В случае 2СА оптимальный вариант - всегда задавать алиас ключа,
        // т.к. это позволяет указать точно, какой контейнер использовать,
        // избегнув, возможно, долгого перечисления контейнеров.
        //
        // Есть 2 способа работы:
        // 1. "белый список" с функциями из класса TLSContext - для случаев
        // 1СА и 2СА. Использует динамически создаваемый SSL контекст классов
        // javax.net.ssl.*;
        // 2. стандартный (прежний) Java SSE, использующий классы javax.net.ssl.*
        // для динамического создания контекста или System.setProperty с передачей
        // параметров аутентификации с помощью свойств javax.net.ssl.* и формированием
        // дефолтного (static) контекста, или без передачи параметров одновременно
        // с созданием SSL контекста.
        //
        // Важно!
        // Рекомендуется использовать:
        // вариант 1 - функции "белого" списка, где создается динамический контекст;
        // или вариант 2 с динамическим созданием контекста;
        // избегать HttpsURLConnection.
        //
        // Важно!
        // Смешанное использование 1 и 2 вариантов крайне не рекомендуется: нужно
        // применять либо только первый подход, либо только второй. Также не
        // рекомендуется смешивать использование дефолтного (static) контекста и
        // динамического. Смешанное использование происходит, например, когда
        // применяется SSlContext к HttpsURLConnection, т.к. последний внутри
        // создает дефолтныый (static) конекст.
        //
        // В первом случае SSLContext будет получен из функций "белого" списка. В
        // случае 2СА пароль к контейнеру в сами функции "белого" списка не передается,
        // т.к. он будет запрошен в ходе подбора контейнеров в специальном окне CSP.
        // Алиас нужно передавать всегда, иначе пароль будет запрошен для всех найденных
        // контейнеров.
        //
        // Во втором случае (стандартный Java SSE) при 2СА поведение осталось тем
        // же, что и раньше, то есть можно и дальше передать пароль в функцию init:
        // <KeyManagerFactory>.init(KeyStore, password);
        // в виде password, или
        // System.setProperty("javax.net.ssl.keyStorePassword", keyPassword);
        // в случае HttpsURLConnection, но появилась особенность из-за п.1: если
        // пароль никак не передали (null), то будет отображено окно ввода пароля CSP
        // для каждого(!) контейнера, который может быть получен с помощью KeyStore,
        // переданного в KeyManagerFactory. Таким образом, в случае передачи пустого
        // пароля (null) также настоятельно рекомендуется передавать в KeyStore хотя
        // бы алиас с помощью класса StoreInputStream в случае 2СА в конструкции вида:
        // keyStore.load(new StoreInputStream(keyAlias), null); // задаем keyAlias
        // <KeyManagerFactory>.init(keyStore, password);
        //
        // Если же используется вариант с передачей параметров javax.net.ssl.* через
        // System.setProperty, то в случае 2СА рекомендуется передавать алиас с помощью:
        // System.setProperty("javax.net.ssl.keyStore", keyAlias);
        // и обязательно пароль:
        // System.setProperty("javax.net.ssl.keyStorePassword", keyPassword);
        //
        val sslCtx: SSLContext

        // Вариант №1, рекомендуемый.
        //
        // В данном случае, при клиентской аутентификации, пароль не
        // передается, он будет запрошен в окне ввода пароля для переданного
        // алиаса ключа.
        if (containerAdapter!!.isUseClientAuth) {
            sslCtx = TLSContext.initAuthClientSSL(
                Provider.PROVIDER_NAME,  // провайдер, по умолчанию - JTLS
                "TLSv1.2",  // протокол, по умолчанию - GostTLS
                JCSP.PROVIDER_NAME,
                keyStoreType,
                containerAdapter?.clientAlias,  // точный алиас ключа
                containerAdapter?.trustStoreProvider,
                containerAdapter?.trustStoreType,
                containerAdapter?.trustStoreStream,
                java.lang.String.valueOf(containerAdapter?.trustStorePassword),
                trustManagers // для Ok Http
            )
        } // if
        else {
            sslCtx = TLSContext.initClientSSL(
                Provider.PROVIDER_NAME,  // провайдер, по умолчанию - JTLS
                "TLSv1.2",  // протокол, по умолчанию - GostTLS
                containerAdapter?.trustStoreProvider,
                containerAdapter?.trustStoreType,
                containerAdapter?.trustStoreStream,
                java.lang.String.valueOf(containerAdapter?.trustStorePassword),
                trustManagers // для Ok Http
            )
        } // else

        Logger.log("SSL context completed.")
        return sslCtx
    }

    companion object {
        /**
         * Вывод полученных данных.
         *
         * @param inputStream Входящий поток.
         * @throws Exception
         */
        @Throws(Exception::class)
        fun logData(inputStream: InputStream?): ByteArray? {
            var br: BufferedReader? = null
            val sb = StringBuilder()
            if (inputStream != null) {
                try {
                    br = BufferedReader(
                        InputStreamReader(
                            inputStream, Constants.DEFAULT_ENCODING
                        )
                    )
                    var input: String?
                    Logger.log("*** Content begin ***")
                    while ((br.readLine().also { input = it }) != null) {
                        input?.let {
                            Logger.log(it)
                            sb.append(it)
                        }
                    } // while
                    Logger.log("*** Content end ***")
                } finally {
                    if (br != null) {
                        try {
                            br.close()
                        } catch (e: IOException) {
                            // ignore
                        }
                    }
                }
            }
            if (sb.length > 0)
                return sb.toString().toByteArray()
            else
                return null
        }
    }
}