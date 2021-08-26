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

import com.objsys.asn1j.runtime.Asn1BerDecodeBuffer
import com.objsys.asn1j.runtime.Asn1Exception
import com.example.signing.util.AlgorithmSelector
import com.example.signing.util.ContainerAdapter
import ru.CryptoPro.JCP.ASN.PKIX1Explicit88.SubjectPublicKeyInfo
import ru.CryptoPro.JCP.params.AlgIdInterface
import ru.CryptoPro.JCP.params.AlgIdSpec
import java.io.IOException
import java.security.cert.Certificate
import java.security.cert.X509Certificate

/**
 * Служебный класс EncryptDecryptData предназначен для
 * реализации примеров шифрования.
 *
 * 27/05/2013
 *
 */
abstract class EncryptDecryptData protected constructor(adapter: ContainerAdapter) :
    SignData(adapter, false) {
    /**
     * Алгоритмы провайдера. Используются на стороне клиента.
     */
    protected var clientAlgSelector: AlgorithmSelector? = null

    /**
     * Алгоритмы провайдера. Используются на стороне сервера.
     */
    protected var serverAlgSelector: AlgorithmSelector? = null

    /**
     * Получение параметров сертификата.
     *
     * @param cert Сертификат.
     * @return Параметры сертификата.
     * @throws IOException
     */
    @Throws(IOException::class)
    protected fun getKeyParams(cert: Certificate): AlgIdInterface {
        if (cert !is X509Certificate) {
            throw IOException("Certificate format is not X509")
        } // if
        val encoded = cert.getPublicKey().encoded
        val buf = Asn1BerDecodeBuffer(encoded)
        val keyInfo = SubjectPublicKeyInfo()
        try {
            keyInfo.decode(buf)
        } catch (e: Asn1Exception) {
            val ex = IOException("Not GOST DH public key")
            ex.initCause(e)
            throw ex
        }
        buf.reset()
        return AlgIdSpec(keyInfo.algorithm)
    }

    /**
     * Конструктор.
     *
     * @param adapter Настройки примера.
     */
    init {
        clientAlgSelector = AlgorithmSelector.getInstance(adapter.providerType)
        serverAlgSelector = AlgorithmSelector.getInstance(adapter.providerType)
    }
}