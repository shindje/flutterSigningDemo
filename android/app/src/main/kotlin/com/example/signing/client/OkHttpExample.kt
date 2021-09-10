package com.example.signing.client

import com.example.signing.base.FinalListener
import com.example.signing.base.TLSData
import com.example.signing.interfaces.ThreadExecuted
import com.example.signing.util.ContainerAdapter
import com.example.signing.util.Logger
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


/**
 * $RCSfileOkHttpExample.java,v $
 * version $Revision: 36379 $
 * created 17.02.2020 15:05 by afevma
 * last modified $Date: 2012-05-30 12:19:27 +0400 (Ср, 30 май 2012) $ by $Author: afevma $
 * (C) ООО Крипто-Про 2004-2020.
 *
 *
 * Программный код, содержащийся в этом файле, предназначен
 * для целей обучения. Может быть скопирован или модифицирован
 * при условии сохранения абзацев с указанием авторства и прав.
 *
 *
 * Данный код не может быть непосредственно использован
 * для защиты информации. Компания Крипто-Про не несет никакой
 * ответственности за функционирование этого кода.
 */

/**
 * Класс OkHttpExample реализует пример обмена
 * по TLS 1.0 с помощью Ok Http v3.
 *
 * @author Copyright 2004-2020 Crypto-Pro. All rights reserved.
 * @.Version
 */
open class OkHttpExample(adapter: ContainerAdapter) : TLSData(adapter) {
    @Throws(Exception::class)
    override fun getResult(data: ByteArray?, listener: FinalListener?) {
        val thread: OkHttpThread = OkHttpThread()
        thread.addFinalListener(listener)
        getThreadResult(thread, data)
    }

    /**
     * Класс SimpleTLSThread реализует подключение
     * самописного клиента по TLS в отдельном потоке.
     *
     */
    inner class OkHttpThread : ThreadExecuted() {
        @Throws(Exception::class)
        override fun executeOne(data: ByteArray?) : Any? {
            Logger.log("Init OkHttp Sample Example.")
            val trustManagers = arrayOfNulls<TrustManager>(1)
            Logger.log("Create SSL context.")
            val sslContext: SSLContext = createSSLContext(trustManagers)
            Logger.log("Create SSL socket factory.")
            val sslSocketFactory = sslContext.socketFactory
            val trustManager = trustManagers[0] as X509TrustManager

            // Установка нужного SSLSocketFactory.
            Logger.log("Create Ok Http client.")
            val builder = OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, trustManager)

            // Задание необходимых параметров (сюиты, протокол).
            val spec: ConnectionSpec = ConnectionSpec.Builder(
                ConnectionSpec.MODERN_TLS
            )
                .tlsVersions("TLSv1.2")
                .cipherSuites(
                    "TLS_CIPHER_2012",
                    "TLS_CIPHER_2001"
                )
                .build()
            builder.connectionSpecs(listOf(spec))
            val client: OkHttpClient = builder.build()

            // Создание запроса к нужному адресу.
            Logger.log("Prepare request.")
            val uri: String = containerAdapter!!.getConnectionInfo()!!.toUrl()!!
            val request: Request = Request.Builder().url(uri).build()

            // Обращение к серверу и вывод полученного ответа.
            Logger.log("Send request.")
            val response = client.newCall(request).execute()
            Logger.log("Successful: " + response.isSuccessful)
            return logData(response.body!!.byteStream())
            //Connection has been established (OK)
        }
    }
}