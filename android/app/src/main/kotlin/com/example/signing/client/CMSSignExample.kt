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
import com.objsys.asn1j.runtime.*
import ru.CryptoPro.JCP.ASN.CertificateExtensions.GeneralName
import ru.CryptoPro.JCP.ASN.CertificateExtensions.GeneralNames
import ru.CryptoPro.JCP.ASN.CryptographicMessageSyntax.*
import ru.CryptoPro.JCP.ASN.PKIX1Explicit88.*
import ru.CryptoPro.JCP.params.OID
import ru.CryptoPro.JCP.tools.AlgorithmUtility
import ru.CryptoPro.JCSP.JCSP
import java.io.ByteArrayInputStream
import java.lang.Exception
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*

/**
 * Класс CMSSignatureExample реализует пример
 * создания CMS подписи.
 *
 * 26/09/2013
 *
 */
class CMSSignExample (signAttributes: Boolean, adapter: ContainerAdapter?) :
    SignData(adapter!!, signAttributes) {
    /**
     * Буферы для записи сообщений о проверке подписей.
     */
    var validationResultOk: StringBuffer? = null
    var validationResultError: StringBuffer? = null

    /**
     * Количество проверенных подписей.
     */
    private var validSignatureCount = 0
    @Throws(Exception::class)
    override fun getResult(data: ByteArray?, listener: FinalListener?) {
        val thread = CMSSignThread()
        thread.addFinalListener(listener)
        getThreadResult(thread, data)
    }

    /**
     * Класс CMSSignThread реализует подпись и
     * проверку CMS в отдельном потоке.
     *
     */
    private inner class CMSSignThread() : ThreadExecuted() {
        @Throws(Exception::class)
        override fun executeOne(data: ByteArray?) : Any? {
            Logger.log("Load key container to sign data.")

            // Тип контейнера по умолчанию.
            val keyStoreType: String = KeyStoreType.currentType()
            Logger.log("Default container type: $keyStoreType")

            // Загрузка ключа и сертификата.
            load(
                askPinInDialog, keyStoreType,
                containerAdapter!!.clientAlias,
                containerAdapter!!.clientPassword
            )
            if (privateKey == null) {
                Logger.log("Private key is null.")
                return null
            } // if
            Logger.log(
                "Compute attached signature for message '" +
                        data?.size + "' :"
            )

            // Формируем совмещенную подпись.
            val signature = create(
                data!!,
                false,
                arrayOf(privateKey!!),
                arrayOf(certificate!!),
                false,
                false
            )
            Logger.log("--- SIGNATURE BEGIN ---")
            Logger.log(signature, true)
            Logger.log("--- SIGNATURE END ---")

            // Проверяем подпись.
            if (!verify(signature, arrayOf(certificate!!), null)) {
                throw Exception("Invalid signature")
            } // if
            else {
                Logger.log("Signature has been created and verified (OK).")
                return signature
            } // else
        }
    }

    /**
     * Создание CMS подписи на хэш данных.
     *
     * @param data Подписываемые данные.
     * @param isExternalDigest True, если вместо данных
     * передается хэш данных (например, при создании подписи
     * формата CAdES-BES).
     * @param certs Список сертификатов подписи подписантов.
     * @param keys Список ключей подписантов.
     * @param detached True, если подпись отсоединенная.
     * @return ЭЦП CMS.
     * @param addSignCertV2 Добавление аттрибута signingCertificateV2
     * для получения подписи формата CAdES-BES.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun create(
        data: ByteArray,
        isExternalDigest: Boolean,
        keys: Array<PrivateKey>,
        certs: Array<Certificate>,
        detached: Boolean,
        addSignCertV2: Boolean
    ): ByteArray {
        Logger.log(
            ("*** Create CMS signature" +
                    (if (needSignAttributes) " on signed attributes" else "") +
                    " ***")
        )
        val all = ContentInfo()
        all.contentType = Asn1ObjectIdentifier(
            OID(STR_CMS_OID_SIGNED).value
        )
        val cms = SignedData()
        all.content = cms
        cms.version = CMSVersion(1)
        cms.digestAlgorithms = DigestAlgorithmIdentifiers(1)
        val a = DigestAlgorithmIdentifier(
            OID(algorithmSelector?.digestAlgorithmOid).value
        )
        a.parameters = Asn1Null()
        cms.digestAlgorithms.elements[0] = a
        Logger.log("Prepare encapsulated content information.")

        // Нельзя сделать подпись совмещенной, если нет данных, а
        // есть только хэш с них.
        if (isExternalDigest && !detached) {
            throw Exception(
                "Signature is attached but external " +
                        "digest is available only (not data)"
            )
        } // if
        if (detached) {
            cms.encapContentInfo = EncapsulatedContentInfo(
                Asn1ObjectIdentifier(
                    OID(STR_CMS_OID_DATA).value
                ), null
            )
        } // if
        else {
            cms.encapContentInfo = EncapsulatedContentInfo(
                Asn1ObjectIdentifier(OID(STR_CMS_OID_DATA).value),
                Asn1OctetString(data)
            )
        } // else

        // Сертификаты.
        Logger.log("Enumerate certificates.")
        val nCerts = certs.size
        cms.certificates = CertificateSet(nCerts)
        cms.certificates.elements = arrayOfNulls(nCerts)
        for (i in cms.certificates.elements.indices) {
            val certificate = Certificate()
            val decodeBuffer = Asn1BerDecodeBuffer(certs[i].encoded)
            certificate.decode(decodeBuffer)
            cms.certificates.elements[i] = CertificateChoices()
            cms.certificates.elements[i].set_certificate(certificate)
        } // for
        val signature = Signature.getInstance(
            algorithmSelector?.signatureAlgorithmName
        )
        var sign: ByteArray?

        // Подписанты (signerInfos).
        Logger.log("Prepare signature infos.")
        val nSigners = keys.size
        cms.signerInfos = SignerInfos(nSigners)
        for (i in cms.signerInfos.elements.indices) {
            Logger.log("** Create signer info $ $i **")
            cms.signerInfos.elements[i] = SignerInfo()
            cms.signerInfos.elements[i].version = CMSVersion(1)
            cms.signerInfos.elements[i].sid = SignerIdentifier()
            Logger.log("Add certificate info.")
            val encodedName = (certs[i] as X509Certificate)
                .issuerX500Principal.encoded
            val nameBuf = Asn1BerDecodeBuffer(encodedName)
            val name = Name()
            name.decode(nameBuf)
            val num = CertificateSerialNumber(
                (certs[i] as X509Certificate).serialNumber
            )
            cms.signerInfos.elements[i].sid.set_issuerAndSerialNumber(
                IssuerAndSerialNumber(name, num)
            )
            cms.signerInfos.elements[i].digestAlgorithm = DigestAlgorithmIdentifier(
                OID(algorithmSelector?.digestAlgorithmOid).value
            )
            cms.signerInfos.elements[i].digestAlgorithm.parameters = Asn1Null()
            val keyAlgOid = AlgorithmUtility.keyAlgToKeyAlgorithmOid(
                privateKey?.getAlgorithm()
            ) // алгоритм ключа подписи
            cms.signerInfos.elements[i].signatureAlgorithm =
                SignatureAlgorithmIdentifier(OID(keyAlgOid).value)
            cms.signerInfos.elements[i].signatureAlgorithm.parameters = Asn1Null()
            var data2hash: ByteArray
            if (needSignAttributes) {
                Logger.log("Need to calculate digest on signed attributes.")
                val kMax = if (addSignCertV2) 4 else 3
                cms.signerInfos.elements[i].signedAttrs = SignedAttributes(kMax)
                Logger.log("Count of signed attributes: $kMax")

                // content-type
                Logger.log("Add content-type.")
                var k = 0
                cms.signerInfos.elements[i].signedAttrs.elements[k] = Attribute(
                    OID(
                        STR_CMS_OID_CONT_TYP_ATTR
                    ).value,
                    Attribute_values(1)
                )
                val cont_type: Asn1Type = Asn1ObjectIdentifier(
                    OID(STR_CMS_OID_DATA).value
                )
                cms.signerInfos.elements[i].signedAttrs.elements[k].values.elements[0] = cont_type

                // signing-time
                Logger.log("Add signing-time.")
                k += 1
                cms.signerInfos.elements[i].signedAttrs.elements[k] = Attribute(
                    OID(
                        STR_CMS_OID_SIGN_TYM_ATTR
                    ).value,
                    Attribute_values(1)
                )
                val time = Time()
                val UTCTime = Asn1UTCTime()

                // Текущая дата календаря.
                UTCTime.time = Calendar.getInstance()
                time.set_utcTime(UTCTime)
                cms.signerInfos.elements[i].signedAttrs.elements[k].values.elements[0] =
                    time.element

                // message-digest
                Logger.log("Add message-digest.")
                k += 1
                cms.signerInfos.elements[i].signedAttrs.elements[k] = Attribute(
                    OID(
                        STR_CMS_OID_DIGEST_ATTR
                    ).value,
                    Attribute_values(1)
                )
                val message_digest_b: ByteArray
                Logger.log("Signing data is digest: $isExternalDigest")

                // Если вместо данных у нас хеш, то сразу его передаем,
                // ничего не вычисляем.
                if (isExternalDigest) {
                    message_digest_b = data
                } // if
                else {
                    if (detached) {
                        message_digest_b = digest(
                            data,
                            algorithmSelector!!.digestAlgorithmName!!
                        )
                    } // if
                    else {
                        message_digest_b = digest(
                            cms.encapContentInfo.eContent.value,
                            algorithmSelector!!.digestAlgorithmName!!
                        )
                    } // else
                } // else
                val message_digest: Asn1Type = Asn1OctetString(message_digest_b)
                cms.signerInfos.elements[i].signedAttrs.elements[k].values.elements[0] =
                    message_digest

                // Добавление signingCertificateV2 в подписанные аттрибуты,
                // чтобы подпись стала похожа на CAdES-BES.
                if (addSignCertV2) {
                    Logger.log("Add signing-certificateV2.")

                    // Аттрибут с OID'ом id_aa_signingCertificateV2.
                    k += 1
                    cms.signerInfos.elements[i].signedAttrs.elements[k] = Attribute(
                        OID(ALL_PKIX1Explicit88Values.id_aa_signingCertificateV2).value,
                        Attribute_values(1)
                    )

                    // Идентификатор алгоритма, который использовался
                    // для хеширования контекста сертификата ключа подписи.
                    val digestAlgorithmIdentifier = DigestAlgorithmIdentifier(
                        OID(algorithmSelector?.digestAlgorithmOid).value
                    )

                    // Хеш сертификата ключа подписи.
                    val certHash = CertHash(
                        digest(
                            certs[i].encoded,
                            algorithmSelector!!.digestAlgorithmName!!
                        )
                    )

                    // Issuer name из сертификата ключа подписи.
                    val generalName = GeneralName()
                    generalName.set_directoryName(name)
                    val generalNames = GeneralNames()
                    generalNames.elements = arrayOfNulls(1)
                    generalNames.elements[0] = generalName

                    // Комбинируем издателя и серийный номер.
                    val issuerSerial = IssuerSerial(generalNames, num)
                    val essCertIDv2 = ESSCertIDv2(
                        digestAlgorithmIdentifier,
                        certHash, issuerSerial
                    )
                    val essCertIDv2s = _SeqOfESSCertIDv2(1)
                    essCertIDv2s.elements = arrayOfNulls(1)
                    essCertIDv2s.elements[0] = essCertIDv2

                    // Добавляем сам аттрибут.
                    val signingCertificateV2 = SigningCertificateV2(essCertIDv2s)
                    cms.signerInfos.elements[i].signedAttrs.elements[k].values.elements[0] =
                        signingCertificateV2
                } // if

                // Данные для хэширования.
                val encBufSignedAttr = Asn1BerEncodeBuffer()
                cms.signerInfos.elements[i].signedAttrs.encode(encBufSignedAttr)
                data2hash = encBufSignedAttr.msgCopy
            } // if
            else {
                data2hash = data
            } // else
            signature.initSign(keys[i])
            signature.update(data2hash)
            sign = signature.sign()
            cms.signerInfos.elements[i].signature = SignatureValue(sign)
        } // for

        // CMS подпись.
        Logger.log("Produce CMS signature.")
        val asnBuf = Asn1BerEncodeBuffer()
        all.encode(asnBuf, true)
        return asnBuf.msgCopy
    }

    /**
     * Проверка CMS подписи.
     *
     * @param buffer CMS подпись.
     * @param certs Сертификаты для проверки ЭЦП. Может быть null.
     * @param data Подписанные данные. Может быть null.
     * @return результат проверки.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun verify(buffer: ByteArray?, certs: Array<Certificate>?, data: ByteArray?): Boolean {
        Logger.log("*** Verify CMS signature ***")
        validationResultOk = StringBuffer("")
        validationResultError = StringBuffer("")
        Logger.log("Decode CMS signature.")
        val asnBuf = Asn1BerDecodeBuffer(buffer)
        val all = ContentInfo()
        all.decode(asnBuf)
        if (!OID(STR_CMS_OID_SIGNED).eq(all.contentType.value)) {
            throw Exception("Not supported.")
        } // if
        Logger.log("Extract encapsulated content information.")
        val cms = all.content as SignedData
        val text: ByteArray
        if (cms.encapContentInfo.eContent != null) {
            text = cms.encapContentInfo.eContent.value
        } // if
        else if (data != null) {
            text = data
        } // else
        else {
            throw Exception(
                "Signature has to " +
                        "include a content for verify."
            )
        } // else
        Logger.log("Source data: " + String(text))
        Logger.log("Extract digest OID.")
        var digestOid: OID? = null
        val digestAlgorithmIdentifier = DigestAlgorithmIdentifier(
            OID(algorithmSelector?.digestAlgorithmOid).value
        )
        for (i in cms.digestAlgorithms.elements.indices) {
            if ((cms.digestAlgorithms.elements[i].algorithm
                        == digestAlgorithmIdentifier.algorithm)
            ) {
                digestOid = OID(cms.digestAlgorithms.elements[i].algorithm.value)
                break
            } // if
        } // for
        if (digestOid == null) {
            throw Exception("Unknown digest OID.")
        } // if
        val eContTypeOID = OID(cms.encapContentInfo.eContentType.value)
        if (cms.certificates != null) {

            // Проверка на вложенных сертификатах.
            Logger.log("Validation on certificates founded in the signature.")
            for (i in cms.certificates.elements.indices) {
                val encBuf = Asn1BerEncodeBuffer()
                cms.certificates.elements[i].encode(encBuf)
                val cf = CertificateFactory.getInstance("X.509")
                val cert = cf
                    .generateCertificate(encBuf.inputStream) as X509Certificate
                for (j in cms.signerInfos.elements.indices) {
                    Logger.log("** Verify signer info $ $j **")
                    val info = cms.signerInfos.elements[j]
                    if (digestOid != OID(info.digestAlgorithm.algorithm.value)) {
                        throw Exception("It isn't signed on certificate.")
                    } // if
                    val checkResult = verifyOnCert(
                        cert,
                        cms.signerInfos.elements[j], text, eContTypeOID, true
                    )
                    writeLog(checkResult, j, i, cert)
                } // for
            } // for
        } // if
        else if (certs != null) {

            // Проверка на указанных сертификатах.
            Logger.log(
                "Certificates for validation not found in " +
                        "the signature.\nTry verify on specified certificates..."
            )
            for (i in certs.indices) {
                val cert = certs[i] as X509Certificate
                for (j in cms.signerInfos.elements.indices) {
                    Logger.log("** Verify signer info $ $j **")
                    val info = cms.signerInfos.elements[j]
                    if (digestOid != OID(info.digestAlgorithm.algorithm.value)) {
                        throw Exception("It isn't signed on certificate.")
                    } // if
                    val checkResult = verifyOnCert(
                        cert,
                        cms.signerInfos.elements[j], text, eContTypeOID, true
                    )
                    writeLog(checkResult, j, i, cert)
                } // for
            } // for
        } // else
        else {
            Logger.log("Certificates for validation are not found.")
        } // else
        if (validSignatureCount == 0) {
            Logger.log("Signatures are invalid: $validationResultError")
            return false
        } // if
        if (cms.signerInfos.elements.size > validSignatureCount) {
            Logger.log(
                ("Some signatures are invalid:" +
                        validationResultOk + validationResultError)
            )
            return false
        } // if
        Logger.log("All signatures are valid:$validationResultOk")
        return true
    }

    /**
     * Попытка проверки подписи на указанном сертификате.
     * Проверка может быть выполнена как по отсортированным
     * подписанным аттрибутам, так и по несортированным.
     *
     * @param cert Сертификат для проверки.
     * @param text Данные для проверки.
     * @param info ЭЦП.
     * @param eContentTypeOID Тип подписанного содержимого.
     * @param needSortSignedAttributes True, если необходимо проверить
     * подпись по отсортированным подписанным аттрибутам. По умолчанию
     * подписанные аттрибуты сортируются перед кодированием.
     * @return True, если подпись корректна.
     * @throws Exception ошибки
     */
    @Throws(Exception::class)
    private fun verifyOnCert(
        cert: X509Certificate,
        info: SignerInfo, text: ByteArray, eContentTypeOID: OID,
        needSortSignedAttributes: Boolean
    ): Boolean {

        // Подпись.
        val sign = info.signature.value

        // Данные для проверки подписи.
        val data: ByteArray
        if (info.signedAttrs == null) {
            // Аттрибуты подписи не присутствуют.
            // Данные для проверки подписи.
            data = text
        } // if
        else {
            Logger.log("Signed attributes are found.")

            // Присутствуют аттрибуты подписи (Signed Attributes).
            val signAttrElem = info.signedAttrs.elements

            // Проверка аттрибута signing-certificateV2.
            val signingCertificateV2Oid = Asn1ObjectIdentifier(
                (OID(ALL_PKIX1Explicit88Values.id_aa_signingCertificateV2)).value
            )
            var signingCertificateV2Attr: Attribute? = null
            for (r in signAttrElem.indices) {
                val oid = signAttrElem[r].type
                if ((oid == signingCertificateV2Oid)) {
                    signingCertificateV2Attr = signAttrElem[r]
                    break
                } // if
            } // for
            if (signingCertificateV2Attr != null) {
                val signingCertificateV2 =
                    signingCertificateV2Attr.values.elements[0] as SigningCertificateV2
                val essCertIDv2s = signingCertificateV2.certs
                for (s in essCertIDv2s.elements.indices) {
                    val essCertIDv2 = essCertIDv2s.elements[s]
                    val expectedCertHash = essCertIDv2.certHash
                    val expectedHashAlgorithm = essCertIDv2.hashAlgorithm
                    val expectedIssuerSerial = essCertIDv2.issuerSerial
                    val encodedExpectedIssuerSerial = Asn1BerEncodeBuffer()
                    expectedIssuerSerial.encode(encodedExpectedIssuerSerial)
                    val expectedHashAlgorithmOid = OID(expectedHashAlgorithm.algorithm.value)
                    val actualCertHash = CertHash(
                        digest(cert.encoded, expectedHashAlgorithmOid.toString())
                    )
                    val certificate = Certificate()
                    val decodeBuffer = Asn1BerDecodeBuffer(cert.encoded)
                    certificate.decode(decodeBuffer)
                    val issuerName = arrayOfNulls<GeneralName>(1)
                    issuerName[0] = GeneralName(
                        GeneralName._DIRECTORYNAME,
                        certificate.tbsCertificate.issuer
                    )
                    val issuerNames = GeneralNames(issuerName)
                    val actualIssuerSerial = IssuerSerial(
                        issuerNames,
                        certificate.tbsCertificate.serialNumber
                    )
                    val encodedActualIssuerSerial = Asn1BerEncodeBuffer()
                    actualIssuerSerial.encode(encodedActualIssuerSerial)
                    if (!(Arrays.equals(actualCertHash.value, expectedCertHash.value) &&
                                Arrays.equals(
                                    encodedActualIssuerSerial.msgCopy,
                                    encodedActualIssuerSerial.msgCopy
                                ))
                    ) {
                        Logger.log(
                            ("Certificate stored in signing-certificateV2 " +
                                    "is not equal to " + cert.subjectDN)
                        )
                        return false
                    } // if
                } // for
            } // if

            // Проверка аттрибута content-type.
            val contentTypeOid = Asn1ObjectIdentifier(
                (OID(
                    STR_CMS_OID_CONT_TYP_ATTR
                )).value
            )
            var contentTypeAttr: Attribute? = null
            for (r in signAttrElem.indices) {
                val oid = signAttrElem[r].type
                if ((oid == contentTypeOid)) {
                    contentTypeAttr = signAttrElem[r]
                    break
                } // if
            } // for
            if (contentTypeAttr == null) {
                throw Exception("content-type attribute isn't not presented.")
            } // if
            if (Asn1ObjectIdentifier(eContentTypeOID.value) != contentTypeAttr.values.elements.get(0)) {
                throw Exception(
                    "content-type attribute OID is not " +
                            "equal to eContentType OID."
                )
            } // if

            // Проверка аттрибута message-digest.
            val messageDigestOid = Asn1ObjectIdentifier(
                (OID(
                    STR_CMS_OID_DIGEST_ATTR
                )).value
            )
            var messageDigestAttr: Attribute? = null
            for (r in signAttrElem.indices) {
                val oid = signAttrElem[r].type
                if ((oid == messageDigestOid)) {
                    messageDigestAttr = signAttrElem[r]
                    break
                } // if
            } // for
            if (messageDigestAttr == null) {
                throw Exception("message-digest attribute is not presented.")
            } // if
            val open = messageDigestAttr.values.elements[0]
            val hash = open as Asn1OctetString
            val md = hash.value

            // Вычисление messageDigest.
            val dm = digest(
                text,
                algorithmSelector!!.digestAlgorithmName!!
            )
            if (ru.CryptoPro.JCP.tools.Array.toHexString(dm) != ru.CryptoPro.JCP.tools.Array.toHexString(
                    md
                )
            ) {
                throw Exception("Verification of message-digest attribute failed.")
            } // if

            // Проверка аттрибута signing-time.
            val signTimeOid = Asn1ObjectIdentifier(
                (OID(STR_CMS_OID_SIGN_TYM_ATTR)).value
            )
            var signTimeAttr: Attribute? = null
            for (r in signAttrElem.indices) {
                val oid = signAttrElem[r].type
                if ((oid == signTimeOid)) {
                    signTimeAttr = signAttrElem[r]
                    break
                } // if
            } // for
            if (signTimeAttr != null) {
                // Проверка (необязательно).
                val sigTime = signTimeAttr.values.elements[0] as Time
                val time = sigTime.element as Asn1UTCTime
                Logger.log("Signing Time: $time")
            }

            //данные для проверки подписи
            val encBufSignedAttr = Asn1BerEncodeBuffer()
            info.signedAttrs.needSortSignedAttributes = needSortSignedAttributes
            info.signedAttrs.encode(encBufSignedAttr)
            data = encBufSignedAttr.msgCopy
        }
        Logger.log("Verify signature.")

        // Проверяем подпись.
        val signature = Signature.getInstance(
            algorithmSelector?.signatureAlgorithmName,
            JCSP.PROVIDER_NAME
        )
        signature.initVerify(cert)
        signature.update(data)
        val verified = signature.verify(sign)
        Logger.log("Signature verified: $verified")

        // Если подпись некорректна, но нас есть подписанные аттрибуты,
        // то пробуем проверить подпись также, отключив сортировку аттрибутов
        // перед кодированием в байтовый массив.
        if (!verified && (info.signedAttrs != null) && needSortSignedAttributes) {
            Logger.log("Try to disable sort of the signed attributes.")
            return verifyOnCert(cert, info, text, eContentTypeOID, false)
        }
        return verified
    }

    /**
     * Составление сообщения о проверке.
     *
     * @param checkResult Флаг, прошла ли проверка.
     * @param signNum Номер подписи.
     * @param certNum Номер сертификата.
     * @param cert Сертификат.
     */
    private fun writeLog(
        checkResult: Boolean,
        signNum: Int, certNum: Int, cert: X509Certificate
    ) {
        if (checkResult) {
            validationResultOk!!.append("\n")
            validationResultOk!!.append("sign[")
            validationResultOk!!.append(signNum)
            validationResultOk!!.append("] - Valid signature on cert[")
            validationResultOk!!.append(certNum)
            validationResultOk!!.append("] (")
            validationResultOk!!.append(cert.subjectX500Principal)
            validationResultOk!!.append(")")
            validSignatureCount += 1
        } else {
            validationResultError!!.append("\n")
            validationResultError!!.append("sign[")
            validationResultError!!.append(signNum)
            validationResultError!!.append("] - Invalid signature on cert[")
            validationResultError!!.append(certNum)
            validationResultError!!.append("] (")
            validationResultError!!.append(cert.subjectX500Principal)
            validationResultError!!.append(")")
        }
    }

    /**
     * @param bytes Хэшируемые данные.
     * @param digestAlgorithmName Алгоритм хэширования.
     * @return хэш данных.
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun digest(bytes: ByteArray, digestAlgorithmName: String): ByteArray {
        val stream = ByteArrayInputStream(bytes)
        val digest = MessageDigest.getInstance(digestAlgorithmName)
        val digestStream = DigestInputStream(stream, digest)
        while (digestStream.available() != 0) {
            digestStream.read()
        } // while
        return digest.digest()
    }

    companion object {
        val STR_CMS_OID_SIGNED = "1.2.840.113549.1.7.2"
        val STR_CMS_OID_DATA = "1.2.840.113549.1.7.1"
        val STR_CMS_OID_CONT_TYP_ATTR = "1.2.840.113549.1.9.3"
        val STR_CMS_OID_DIGEST_ATTR = "1.2.840.113549.1.9.4"
        val STR_CMS_OID_SIGN_TYM_ATTR = "1.2.840.113549.1.9.5"
    }

    /**
     * Конструктор.
     *
     * @param signAttributes True, если требуется создать
     * подпись по атрибутам.
     * @param adapter Настройки примера.
     */
    init {
        needSignAttributes = signAttributes
    }
}