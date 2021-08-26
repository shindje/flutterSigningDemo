/**
 * $RCSfileContainerAdapter.java,v $
 * version $Revision: 36379 $
 * created 01.12.2014 18:53 by Yevgeniy
 * last modified $Date: 2012-05-30 12:19:27 +0400 (Ср, 30 май 2012) $ by $Author: afevma $
 *
 * Copyright 2004-2014 Crypto-Pro. All rights reserved.
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
import android.content.res.Resources
import com.example.signing.util.AlgorithmSelector.DefaultProviderType
import ru.CryptoPro.JCSP.JCSP
import java.io.InputStream

/**
 * Класс ContainerAdapter предназначен для хранения всех
 * настроек различных примеров пакета example.
 *
 * @author Copyright 2004-2014 Crypto-Pro. All rights reserved.
 * @.Version
 */
class ContainerAdapter(
    /**
     * Контекст приложения.
     */
    val context: Context?, cAlias: String?,
    cPassword: CharArray?, sAlias: String?, sPassword: CharArray?
) {
    /**
     * Определение алиаса контейнера клиента.
     *
     * @return алиас контейнера клиента.
     */
    /**
     * Алиасы клиента и получателя.
     */
    var clientAlias: String? = null

    /**
     * Определение алиаса к контейнеру получателя.
     *
     * @return алиас к контейнеру получателя.
     */
    var serverAlias: String? = null
    /**
     * Определение пароля к контейнеру клиента.
     *
     * @return пароль к контейнеру клиента.
     */
    /**
     * Пароли клиента и получателя.
     */
    var clientPassword: CharArray? = null

    /**
     * Определение пароля к контейнеру получателя.
     *
     * @return пароль к контейнеру получателя.
     */
    var serverPassword: CharArray? = null
    /**
     * Определение провайдера хранилища доверенных
     * сертификатов.
     *
     * @return провайдер.
     */
    /**
     * Задание провайдера хранилища доверенных
     * сертификатов.
     *
     * @param provider Провайдер.
     */
    /**
     * Провайдер хранилища сертификатов.
     */
    var trustStoreProvider = JCSP.PROVIDER_NAME
    /**
     * Определение типа хранилища доверенных
     * сертификатов.
     *
     * @return тип.
     */
    /**
     * Задание типа хранилища доверенных
     * сертификатов.
     *
     * @param type Тип.
     */
    /**
     * Тип хранилища сертификатов.
     */
    var trustStoreType = JCSP.CERT_STORE_NAME
    /**
     * Определение пароля к хранилищу доверенных
     * сертификатов.
     *
     * @return пароль.
     */
    /**
     * Задание пароля к хранилищу доверенных
     * сертификатов.
     *
     * @param password Пароль.
     */
    /**
     * Пароль к хранилищу сертификатов.
     */
    var trustStorePassword: CharArray? = null
    /**
     * Определение потока из файла хранилища доверенных
     * сертификатов.
     *
     * @return поток.
     */
    /**
     * Задание потока из файла хранилища доверенных
     * сертификатов.
     *
     * @param stream поток.
     */
    /**
     * Поток из файлов хранилища сертификатов.
     */
    var trustStoreStream: InputStream? = null
    /**
     * Определение типа провайдера.
     *
     * @return тип провайдера.
     */
    /**
     * Задание типа провайдера.
     *
     * @param pt Тип провайдера.
     */
    /**
     * Тип провайдера для выбора алгоритмов.
     */
    var providerType: DefaultProviderType? = null
    /**
     * Определение ресурсов приложения.
     *
     * @return ресурсы приложения.
     */
    /**
     * Задание ресурсов приложения.
     *
     * @param r Ресурсы приложения.
     */
    /**
     * Ресурсы приложения.
     */
    var resources: Resources? = null

    /**
     * Настройки для удаленного подключения (TLS).
     */
    private var connectionInfo: RemoteConnectionInfo? = null
    /**
     * Определение, является ли ключ ключом обмена.
     *
     * @return true, если ключ обмена.
     */
    /**
     * Флаг типа ключа. True, если ключ обмена.
     */
    var isExchangeKey = false
        private set
    /**
     * Получения контекста приложения.
     *
     * @return контекст приложения.
     */

    /**
     * Конструктор. Используется при генерации и
     * удалении контейнеров.
     *
     * @param context Контекст приложения.
     * @param alias Алиас контейнера клиента.
     * @param exchange Тип ключа. True, если ключ обмена.
     */
    constructor(
        context: Context?, alias: String?,
        exchange: Boolean
    ) : this(context, alias, null, null, null) {
        isExchangeKey = exchange
    }

    /**
     * Определение, требуется ли аутентификация клиента.
     * Зависит от настроек удаленного хоста.
     *
     * @return true, если требуется.
     */
    val isUseClientAuth: Boolean
        get() = connectionInfo != null && connectionInfo!!.isUseClientAuth

    /**
     * Задание настроек удаленного хоста.
     *
     * @param info Настройки хоста.
     */
    fun setConnectionInfo(info: RemoteConnectionInfo?) {
        connectionInfo = info
    }

    /**
     * Определение настроек удаленного хоста.
     *
     * @return настройки хоста.
     */
    fun getConnectionInfo(): RemoteConnectionInfo? {
        return connectionInfo
    }

    /**
     * Вывод в лог информации о подключении.
     *
     */
    fun printConnectionInfo() {
        if (connectionInfo == null) {
            return
        } // if
        Logger.log("------ Remote host settings ------")
        Logger.log("Host: " + connectionInfo!!.hostAddress)
        Logger.log("Port: " + connectionInfo!!.hostPort)
        Logger.log("Page: " + connectionInfo!!.hostPage)
        Logger.log("Require client auth: " + connectionInfo!!.isUseClientAuth)
        Logger.log("[Url: " + connectionInfo!!.toUrl().toString() + "]")
        Logger.log("----------------------------------")
    }

    /**
     * Конструктор. Используется для выполнения примеров.
     *
     * @param context Контекст приложения.
     * @param cAlias Алиас контейнера клиента.
     * @param cPassword Пароль к контейнеру клиента.
     * @param sAlias Алиас контейнера получателя.
     * @param sPassword Пароль к контейнеру получателя.
     */
    init {
        clientAlias = cAlias
        clientPassword = cPassword
        serverAlias = sAlias
        serverPassword = sPassword
    }
}