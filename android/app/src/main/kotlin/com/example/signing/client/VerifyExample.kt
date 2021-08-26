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
 * Класс VerifyExample реализует пример проверки подписи
 * сообщения.
 *
 * 27/05/2013
 *
 */
class VerifyExample
/**
 * Конструктор.
 *
 * @param adapter Настройки примера.
 */
    (adapter: ContainerAdapter?) : SignData(adapter!!, false) {
    @Throws(Exception::class)
    override fun getResult(listener: FinalListener?) {
        val thread: VerifyThread = VerifyThread()
        thread.addFinalListener(listener)
        getThreadResult(thread)
    }

    /**
     * Класс VerifyThread реализует проверку подписи
     * в отдельном потоке.
     *
     */
    private inner class VerifyThread : ThreadExecuted() {
        @Throws(Exception::class)
        protected override fun executeOne() {
            Logger.log("Create signature.")

            // Создаем подпись, чтобы потом ее проверить.
            val signData = SignExample(containerAdapter)
            val sign: ByteArray? = signData.sign()
            Logger.log("Load key container to verify signature.")

            // Тип контейнера по умолчанию.
            val keyStoreType: String = KeyStoreType.currentType()
            Logger.log("Default container type: $keyStoreType")

            // Загрузка ключа и сертификата.
            load(
                true, keyStoreType,
                containerAdapter?.clientAlias,
                containerAdapter?.clientPassword
            )
            if (certificate == null) {
                Logger.log("Certificate is null.")
                return
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
            Logger.log(
                "Init verification by certificate: " +
                        certificate!!.getSubjectDN().toString() + ", public key:" +
                        certificate!!.getPublicKey()
            )
            sn.initVerify(certificate)
            Logger.log("Source data: " + Constants.MESSAGE)
            sn.update(Constants.MESSAGE.toByteArray())
            Logger.log("Verify signature:")
            Logger.log(sign!!, true)

            // Проверяем подпись.
            if (sn.verify(sign)) {
                Logger.log("Data has been verified (OK).")
            } // if
            else {
                throw Exception("Invalid signature.")
            } // else
        }
    }
}