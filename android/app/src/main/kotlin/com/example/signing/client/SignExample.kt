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
package com.example.signing.client

import com.example.signing.base.FinalListener
import com.example.signing.base.SignData
import com.example.signing.interfaces.ThreadExecuted
import com.example.signing.util.Constants
import com.example.signing.util.ContainerAdapter
import com.example.signing.util.KeyStoreType
import com.example.signing.util.Logger
import ru.CryptoPro.JCSP.JCSP
import java.security.Signature

/**
 * Класс SignExample реализует пример подписи
 * сообщения.
 *
 * 27/05/2013
 *
 */
class SignExample(adapter: ContainerAdapter?) : SignData(adapter!!, false) {

    @Throws(Exception::class)
    override fun getResult(data: ByteArray?, listener: FinalListener?) {
        val thread = SignThread()
        thread.addFinalListener(listener)
        getThreadResult(thread, data)
    }

    /**
     * Класс SignThread реализует формирование подписи
     * в отдельном потоке.
     *
     */
    private inner class SignThread() : ThreadExecuted() {
        @Throws(Exception::class)
        override fun executeOne(data: ByteArray?): Any? {
            return sign(data)
        }
    }

    /**
     * Формирование подписи.
     *
     * @return подпись или null.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sign(data: ByteArray?): ByteArray? {
        Logger.log("Load key container to sign data.")

        // Тип контейнера по умолчанию.
        val keyStoreType: String = KeyStoreType.currentType()
        Logger.log("Default container type: $keyStoreType")

        // Загрузка ключа и сертификата.
        load(
            askPinInDialog, keyStoreType,
            containerAdapter?.clientAlias,
            containerAdapter?.clientPassword
        )
        if (privateKey == null) {
            Logger.log("Private key is null.")
            return null
        } // if
        Logger.log(
            "Init Signature: " +
                    algorithmSelector?.signatureAlgorithmName
        )

        // Инициализация подписи.
        val sn = Signature.getInstance(
            algorithmSelector?.signatureAlgorithmName,
            JCSP.PROVIDER_NAME
        )
        Logger.log("Init signature by private key: " + privateKey)
        sn.initSign(privateKey)
        sn.update(data)

        // Формируем подпись.
        Logger.log(
            "Compute signature for message '" +
                    data?.size + "' :"
        )
        val sign = sn.sign()
        Logger.log(sign, true)
        Logger.log("Data has been signed (OK).")
        return sign
    }
}