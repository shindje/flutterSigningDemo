/**
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

import ru.CryptoPro.JCP.JCP

/**
 * Служебный класс AlgorithmSelector предназначен
 * для получения алгоритмов и свойств, соответствующих
 * заданному провайдеру.
 *
 * 27/01/2014
 *
 */
open class AlgorithmSelector protected constructor(
    /**
     * Тип провайдера.
     */
    val providerType: DefaultProviderType,
    signAlgName: String?, digestAlgName: String?, digestAlgOid: String?
) {
    /**
     * Возможные типы провайдеров.
     */
    enum class DefaultProviderType {
        ptUnknown, pt2001, pt2012Short, pt2012Long
    }
    /**
     * Получение типа провайдера.
     *
     * @return тип провайдера.
     */
    /**
     * Получение алгоритма подписи.
     *
     * @return алгоритм подписи.
     */
    /**
     * Алгоритм подписи.
     */
    var signatureAlgorithmName: String? = null
    /**
     * Получение алгоритма хеширования.
     *
     * @return алгоритм хеширования.
     */
    /**
     * Алгоритм хеширования.
     */
    var digestAlgorithmName: String? = null
    /**
     * Получение OID'а алгоритма хеширования.
     *
     * @return OID алгоритма.
     */
    /**
     * OID алгоритма хеширования.
     */
    var digestAlgorithmOid: String? = null
    //------------------------------------------------------------------------------------------------------------------
    /**
     * Класс с алгоритмами ГОСТ 2001.
     *
     */
    private class AlgorithmSelector_2011
    /**
     * Конструктор.
     *
     */
        : AlgorithmSelector(
        DefaultProviderType.pt2001, JCP.GOST_EL_SIGN_NAME,
        JCP.GOST_DIGEST_NAME, JCP.GOST_DIGEST_OID
    )

    /**
     * Класс с алгоритмами ГОСТ 2012 (256).
     *
     */
    private class AlgorithmSelector_2012_256
    /**
     * Конструктор.
     *
     */
        : AlgorithmSelector(
        DefaultProviderType.pt2012Short, JCP.GOST_SIGN_2012_256_NAME,
        JCP.GOST_DIGEST_2012_256_NAME, JCP.GOST_DIGEST_2012_256_OID
    )

    /**
     * Класс с алгоритмами ГОСТ 2012 (512).
     *
     */
    private class AlgorithmSelector_2012_512
    /**
     * Конструктор.
     *
     */
        : AlgorithmSelector(
        DefaultProviderType.pt2012Long, JCP.GOST_SIGN_2012_512_NAME,
        JCP.GOST_DIGEST_2012_512_NAME, JCP.GOST_DIGEST_2012_512_OID
    )

    companion object {
        /**
         * Получение списка алгоритмов для данного провайдера.
         *
         * @param pt Тип провайдера.
         * @return настройки провайдера.
         */
        fun getInstance(pt: DefaultProviderType?): AlgorithmSelector {
            when (pt) {
                DefaultProviderType.pt2001 -> return AlgorithmSelector_2011()
                DefaultProviderType.pt2012Short -> return AlgorithmSelector_2012_256()
                DefaultProviderType.pt2012Long -> return AlgorithmSelector_2012_512()
            }
            throw IllegalArgumentException()
        }

        /**
         * Получение типа провайдера по его строковому представлению.
         *
         * @param val Тип в виде числа.
         * @return тип в виде значения из перечисления.
         */
        fun find(`val`: Int): DefaultProviderType {
            when (`val`) {
                0 -> return DefaultProviderType.pt2001
                1 -> return DefaultProviderType.pt2012Short
                2 -> return DefaultProviderType.pt2012Long
            }
            throw IllegalArgumentException()
        }
    }

    /**
     * Конструктор.
     *
     * @param type Тип провайдера.
     * @param signAlgName Алгоритм подписи.
     * @param digestAlgName Алгоритм хеширования.
     * @param digestAlgOid OID алгоритма хеширования.
     */
    init {
        signatureAlgorithmName = signAlgName
        digestAlgorithmName = digestAlgName
        digestAlgorithmOid = digestAlgOid
    }
}