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
package com.example.signing.base

import android.content.Context
import android.content.DialogInterface
import android.util.Log
import com.example.signing.interfaces.HashData
import com.example.signing.interfaces.ThreadExecuted
import com.example.signing.util.*
import ru.CryptoPro.JCP.KeyStore.JCPPrivateKeyEntry
import ru.CryptoPro.JCP.params.JCPProtectionParameter
import ru.CryptoPro.JCSP.JCSP
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Служебный класс ISignData предназначен для
 * релизации примеров работы с подписью.
 *
 * 27/05/2013
 *
 */
abstract class SignData protected constructor(adapter: ContainerAdapter, signAttributes: Boolean) :
    HashData, Containers {
    /**
     * Флаг ввода пин-кода в окне CSP, а не программно.
     */
    protected var askPinInDialog = true
    /**
     * Получение закрытого ключа.
     *
     * @return закрытый ключ.
     */
    /**
     * Загруженный закрытый ключ для подписи.
     */
    var privateKey: PrivateKey? = null
        private set
    /**
     * Получение сертификата ключа.
     *
     * @return сертификат ключа.
     */
    /**
     * Загруженный сертификат ключа подписи для
     * проверки подписи.
     */
    var certificate: X509Certificate? = null
        private set

    /**
     * Алгоритмы провайдера. Используются при подписи.
     */
    protected var algorithmSelector: AlgorithmSelector? = null

    /**
     * Флаг необходимости создания подписи на
     * подписываемые аттрибуты.
     */
    protected var needSignAttributes = false

    /**
     * Настройки примера.
     */
    protected var containerAdapter: ContainerAdapter? = null

    companion object {
        /**
         * Фабрика сертификатов.
         */
        protected var CERT_FACTORY: CertificateFactory? = null

        /**
         * Работа примера в потоке. Запускается выполнение
         * задачи в отдельном потоке (обычно при подключении
         * к интернету).
         *
         * @param context Контекст приложения.
         * @param task Выполняемая задача.
         * @throws Exception
         */
        @Throws(Exception::class)
        fun getThreadResult(
            context: Context?,
            task: ThreadExecuted?
        ) {

            // Формирование окна ожидания.
            Logger.log("Prepare progress dialog.")
            val progressDialog = context?.let { ProgressDialogHolder(it, false) }
            Logger.log("Prepare client thread.")

            // Запуск потока с задачей, который можно
            // прервать. Окно будет закрыто в нем.
            val clientThread = ClientThread(task, progressDialog)
            clientThread.setPriority(Thread.NORM_PRIORITY)
            class CancelListener() : DialogInterface.OnCancelListener {
                override fun onCancel(dialog: DialogInterface) {
                    if (clientThread.isAlive()) {

                        // Прерывание не реализовано.
                        Logger.log("Client thread interrupted!")
                        clientThread.interrupt()
                    } // if
                }
            }

            // Обработка отмены.
            progressDialog?.setOnCancelListener(CancelListener())
            Logger.log("Show progress dialog.")
            progressDialog?.show()
            Logger.log("Start client thread.")
            clientThread.start()
        }

        init {
            try {
                CERT_FACTORY = CertificateFactory.getInstance("X.509")
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    /**
     * Загрузка ключа и сертификата из контейнера. Если параметр
     * askPinInWindow равен true, то переданный сюда пароль не
     * имеет значения, он будет запрошен в окне CSP только при
     * непосредственной работе с ключом. Если же параметр равен
     * false, то этот пароль будет задан однажды и, если он
     * правильный, больше не понадобится вводить его в окне CSP.
     *
     * @param askPinInWindow True, если будем вводить пин-код в
     * окне.
     * @param storeType Тип ключевого контейнера.
     * @param alias Алиас ключа.
     * @param password Пароль к ключу.
     */
    @Throws(Exception::class)
    fun load(
        askPinInWindow: Boolean, storeType: String?,
        alias: String?, password: CharArray?
    ) {
        if (privateKey != null && certificate != null) {
            return
        } // if

        // Загрузка контейнеров.
        val keyStore = KeyStore.getInstance(storeType, JCSP.PROVIDER_NAME)
        keyStore.load(null, null)
        if (askPinInWindow) {
            val getKey = keyStore.getKey(alias, password)
            if (getKey != null)
                privateKey = getKey as PrivateKey
            else
                privateKey = null

            val getCertificate = keyStore.getCertificate(alias)
            if (getCertificate != null)
                certificate = getCertificate as X509Certificate
            else
                certificate = null
        } // if
        else {
            val protectedParam = JCPProtectionParameter(
                password,
                true, true
            ) // допускаем, что сертификата может не быть
            val entry = keyStore.getEntry(alias, protectedParam) as JCPPrivateKeyEntry
            privateKey = entry.privateKey
            certificate = entry.certificate as X509Certificate
        } // else

        // Отображение информации о ключе.
        if (privateKey == null || certificate == null) {
            throw Exception("Private key or/and certificate is null.")
        } // if
        else {
            Log.i(
                Constants.APP_LOGGER_TAG, "Certificate: " +
                        certificate!!.subjectDN
            )
        } // else
        Logger.log("Read private key:$privateKey")
        Logger.log(
            "Read certificate:" + certificate!!.subjectDN +
                    ", public key: " + certificate!!.publicKey
        )
    }

    /**
     * Работа примера в потоке. Запускается выполнение
     * задачи в отдельном потоке (обычно при подключении
     * к интернету).
     *
     * @param task Выполняемая задача.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun getThreadResult(task: ThreadExecuted?) {
        getThreadResult(containerAdapter?.context, task)
    }

    /**
     * Конструктор.
     *
     * @param adapter Настройки примера.
     * @param signAttributes True, если требуется создать
     * подпись по атрибутам.
     */
    init {
        algorithmSelector = AlgorithmSelector.getInstance(adapter.providerType)
        needSignAttributes = signAttributes
        containerAdapter = adapter
    }
}