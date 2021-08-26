/**
 * $RCSfileKeyStoreUtil.java,v $
 * version $Revision: 36379 $
 * created 14.06.2017 9:41 by afevma
 * last modified $Date: 2012-05-30 12:19:27 +0400 (Ср, 30 май 2012) $ by $Author: afevma $
 *
 *
 * Copyright 2004-2017 Crypto-Pro. All rights reserved.
 * Программный код, содержащийся в этом файле, предназначен
 * для целей обучения. Может быть скопирован или модифицирован
 * при условии сохранения абзацев с указанием авторства и прав.
 *
 * Данный код не может быть непосредственно использован
 * для защиты информации. Компания Крипто-Про не несет никакой
 * ответственности за функционирование этого кода.
 */
package com.example.signing.util

import android.util.Log
import com.example.signing.util.AlgorithmSelector.DefaultProviderType
import ru.CryptoPro.JCP.JCP
import ru.CryptoPro.JCSP.JCSP
import java.security.KeyStore
import java.security.Provider
import java.security.cert.X509Certificate
import java.util.*

/**
 * Служебный класс KeyStoreUtil для работы с
 * контейнерами.
 *
 * @author Copyright 2004-2017 Crypto-Pro. All rights reserved.
 * @.Version
 */
object KeyStoreUtil {
    /**
     * Java-провайдер Java CSP.
     */
    val DEFAULT_PROVIDER: Provider = JCSP()

    /**
     * Загрузка тех алиасов, которые находятся в хранилище storeType
     * с алгоритмом, сооветствующим типу providerType.
     *
     * @param storeType Тип контейнера.
     * @param providerType Тип провайдера.
     */
    fun aliases(
        storeType: String?,
        providerType: DefaultProviderType?
    ): List<String> {
        val aliasesList: MutableList<String> = ArrayList()
        try {
            val keyStore = KeyStore.getInstance(storeType, JCSP.PROVIDER_NAME)
            keyStore.load(null, null)
            val aliases = keyStore.aliases()

            // Подбор алиасов.
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                val cert = keyStore.getCertificate(alias) as X509Certificate
                if (cert != null) {
                    val keyAlgorithm = cert.publicKey.algorithm
                    if (providerType == DefaultProviderType.pt2001 &&
                        keyAlgorithm.equals(JCP.GOST_EL_DEGREE_NAME, ignoreCase = true)
                    ) {
                        aliasesList.add(alias)
                    } // if
                    else if (providerType == DefaultProviderType.pt2012Short &&
                        keyAlgorithm.equals(JCP.GOST_EL_2012_256_NAME, ignoreCase = true)
                    ) {
                        aliasesList.add(alias)
                    } // else
                    else if (providerType == DefaultProviderType.pt2012Long &&
                        keyAlgorithm.equals(JCP.GOST_EL_2012_512_NAME, ignoreCase = true)
                    ) {
                        aliasesList.add(alias)
                    } // else
                } // if
            } // while
        } catch (e: Exception) {
            Log.e(Constants.APP_LOGGER_TAG, e.message, e)
        }
        return aliasesList
    }
}