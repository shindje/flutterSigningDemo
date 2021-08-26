/**
 * $RCSfileUtilActivity.java,v $
 * version $Revision: 36379 $
 * created 13.06.2017 16:49 by afevma
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
package com.example.signing

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import cmsutil.CMSMain
import com.example.signing.util.Constants
import com.example.signing.util.Logger
import org.bouncycastle.jce.provider.BouncyCastleProvider
import ru.CryptoPro.JCP.tools.Array
import ru.CryptoPro.JCSP.JCSP
import ru.CryptoPro.JCSP.support.BKSTrustStore
import ru.cprocsp.ACSP.tools.common.AppUtils
import ru.cprocsp.ACSP.util.FileExplorerActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.*

/**
 * Вкладка для проверки шифрования сообщений с помощью
 * утилиты CMSUtil (cmsutil.jar). Работа осуществляется
 * только с контейнерами HDIMAGE.
 *
 * @author Copyright 2004-2017 Crypto-Pro. All rights reserved.
 * @.Version
 */
class UtilActivity() : Fragment(), AdapterView.OnItemSelectedListener,
    Observer {
    /**
     * Список получателей. Соответствует списку
     * сертификатов в хранилище сертификатов
     * получателей и контейнерам.
     */
    private var spRecipient: Spinner? = null

    /**
     * Пароль получателя сообщения. Используется при
     * расшифровании для доступа к контейнеру.
     */
    private var etRecipientPassword: EditText? = null

    /**
     * Поля ввода путей к входящему и исходщему
     * файлам.
     */
    private var etInFile: EditText? = null
    private var etOutFile: EditText? = null

    /**
     * Группа выбора действия - зашифрование или
     * расшифрование.
     */
    private var rgEncryptDecrypt: RadioGroup? = null

    /**
     * Адаптер списка алиасов контейнеров.
     */
    private var recipientAdapter: ArrayAdapter<String>? = null

    /**
     * Задача сохранения файла хранилища в папке
     * приложения.
     */
    private var saveCertStoreTask: SaveCertStoreTask? = null

    /**
     * Задача выполнения зашифрования или расшифрования.
     */
    private var encryptOrDecryptTask: EncryptOrDecryptTask? = null

    /**
     * Получение папки приложения для записи хранилища
     * сертификатов получателей.
     *
     * @return папка.
     */
    private val appDir: String
        private get() = requireActivity().applicationInfo.dataDir

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val page: View = inflater.inflate(R.layout.util, container, false)

        // Осуществляемое действие.
        rgEncryptDecrypt = page.findViewById<View>(R.id.rgEncryptDecrypt) as RadioGroup

        // Список алиасов получателей.
        spRecipient = page.findViewById<View>(R.id.spRecipient) as Spinner

        // Адаптер списка алиасов получателей.
        recipientAdapter = ArrayAdapter(
            page.context,
            android.R.layout.simple_spinner_item
        )
        recipientAdapter!!.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spRecipient!!.adapter = recipientAdapter
        spRecipient!!.onItemSelectedListener = this

        // Пароль к контейнеру получателя.
        etRecipientPassword = page.findViewById<View>(R.id.etPassword) as EditText

        // Если будем шифровать в адрес получателя, то пароль не нужен.
        rgEncryptDecrypt!!.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rbEncrypt -> etRecipientPassword!!.isEnabled = false
                R.id.rbDecrypt -> etRecipientPassword!!.isEnabled = true
            }
        }
        rgEncryptDecrypt!!.check(R.id.rbEncrypt) // по умолчанию - зашифрование

        // Пути к входящему и исходящему файлам.
        etInFile = page.findViewById<View>(R.id.etInFile) as EditText
        etOutFile = page.findViewById<View>(R.id.etOutFile) as EditText

        // Кнопки выбора файлов.
        val btOpenInFile = page.findViewById<View>(R.id.btOpenInFile) as Button
        btOpenInFile.setOnClickListener {
            val intent = Intent(activity, FileExplorerActivity::class.java)
            intent.putExtra(FileExplorerActivity.INTENT_EXTRA_IN_ONLY_DIRS, false)
            intent.putExtra(FileExplorerActivity.INTENT_EXTRA_IN_HIDE_EXTERNAL_STORAGE, true)
            startActivityForResult(intent, FILE_SELECT_CODE_IN)
        }
        val btOpenOutFile = page.findViewById<View>(R.id.btOpenOutFile) as Button
        btOpenOutFile.setOnClickListener {
            val intent = Intent(activity, FileExplorerActivity::class.java)
            intent.putExtra(FileExplorerActivity.INTENT_EXTRA_IN_ONLY_DIRS, false)
            intent.putExtra(FileExplorerActivity.INTENT_EXTRA_IN_HIDE_EXTERNAL_STORAGE, true)
            intent.putExtra(
                FileExplorerActivity.INTENT_EXTRA_IN_FILE_FILTER,
                FILE_SELECT_CODE_OUT
            )
            startActivityForResult(intent, FILE_SELECT_CODE_OUT)
        }

        // Кнопка зашифрования/расшифрования.
        val btExecute = page.findViewById<View>(R.id.btExecute) as Button
        btExecute.setOnClickListener {
            val encrypt = (rgEncryptDecrypt!!.checkedRadioButtonId == R.id.rbEncrypt)
            val alias = spRecipient!!.selectedItem as String
            val password = etRecipientPassword!!.text.toString()
            val inFile = etInFile!!.text.toString()
            val outFile = etOutFile!!.text.toString()
            encryptOrDecryptTask = EncryptOrDecryptTask(
                encrypt,
                appDir,
                alias, password, inFile, outFile
            )
            encryptOrDecryptTask!!.execute()
        }

        // Кнопка сравнения файлов.
        val btCompare = page.findViewById<View>(R.id.btCompare) as Button
        btCompare.setOnClickListener { compareFiles() }
        AppUtils.setupUI(page.findViewById(R.id.llUtilMain))
        return page
    }

    override fun update(observable: Observable, data: Any) {
        synchronized(this@UtilActivity) {

            // Комаанда из MainActivity при смене вкладки.
            // Обновляем список контейнеров.Работа
            // осуществляется только с контейнерами
            // {@link #STORE_TYPE}.
            if (recipientAdapter != null) {
                if (data is List<*>) {
                    val aliasesList: List<String> =
                        data as List<String>
                    recipientAdapter!!.clear()
                    recipientAdapter!!.addAll(aliasesList)
                    recipientAdapter!!.notifyDataSetChanged()

                    // Сохранение хранилища с сертификатами получателей
                    // в папку приложения.
                    saveCertStoreTask = SaveCertStoreTask(
                        aliasesList,
                        appDir
                    )
                    saveCertStoreTask!!.execute()
                }
            } // if
        } // synchronized
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FILE_SELECT_CODE_IN -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val chosenFilePath = data.getStringExtra("chosenObject")
                    etInFile!!.setText(chosenFilePath)

                    // Допишем путь к исходящему файлу.
                    val encrypt = rgEncryptDecrypt!!.checkedRadioButtonId == R.id.rbEncrypt
                    etOutFile!!.setText(chosenFilePath + if (encrypt) ".encrypted" else ".decrypted")
                } // if
            }
            FILE_SELECT_CODE_OUT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val chosenPath = data.getStringExtra("chosenObject")
                    etOutFile!!.setText(chosenPath)
                } // if
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Задача сохранения файла хранилища в папке
     * приложения. Работа осуществляется
     * только с контейнерами HDIMAGE.
     *
     */
    private class SaveCertStoreTask(private val aliases: List<String>, private val appDir: String) :
        AsyncTask<Void?, Void?, Void?>() {

        override fun doInBackground(vararg params: Void?): Void? {
            var storeInput: FileInputStream? = null
            var storeOutput: FileOutputStream? = null
            try {
                val path = appDir + File.separator + CERT_STORE_NAME
                val file = File(path)
                val recipientStore = KeyStore.getInstance(
                    BKSTrustStore.STORAGE_TYPE,
                    BouncyCastleProvider.PROVIDER_NAME
                )
                if (file.exists()) {
                    storeInput = FileInputStream(file)
                    recipientStore.load(storeInput, CERT_STORE_PASSWORD)
                } // if
                else {
                    recipientStore.load(null, null)
                } // else
                val keyStore = KeyStore.getInstance(
                    STORE_TYPE,
                    JCSP.PROVIDER_NAME
                )
                keyStore.load(null, null)
                var saved = false

                // Сохраняем сертификаты в хранилище,
                // добавляя новые или обновляя старые.
                for (alias: String? in aliases) {
                    val cert: X509Certificate? = keyStore.getCertificate(alias) as X509Certificate
                    if (cert != null) {
                        recipientStore.setCertificateEntry(alias, cert)
                        saved = true
                    } // if
                } // for

                // Если была запись - сохраняем.
                if (saved) {
                    storeOutput = FileOutputStream(file)
                    recipientStore.store(storeOutput, CERT_STORE_PASSWORD)
                } // if
            } catch (e: Exception) {
                Log.e(Constants.APP_LOGGER_TAG, e.message, e)
                e.message?.let { Logger.log(it) }
            } finally {
                if (storeInput != null) {
                    try {
                        storeInput.close()
                    } catch (e: IOException) {
                        // ignore
                    }
                } // if
                if (storeOutput != null) {
                    try {
                        storeOutput.close()
                    } catch (e: IOException) {
                        // ignore
                    }
                } // if
            }
            return null
        }
    }

    /**
     * Задача выполнения зашифрования или расшифрования.
     *
     */
    private class EncryptOrDecryptTask(
        private val encrypt: Boolean,
        private val appDir: String,
        private val alias: String,
        private val password: String,
        private val inFile: String,
        private val outFile: String
    ) :
        AsyncTask<Void?, Void?, Void?>() {

        override fun doInBackground(vararg params: Void?): Void? {
            Logger.clear()
            Logger.log(
                "*** CMSUtil (cmsutil.jar, " +
                        (if (encrypt) "encrypt" else "decrypt") + " ***"
            )
            val certStorePath = appDir + File.separator + CERT_STORE_NAME
            val args = if (encrypt) arrayOf(
                "-encrypt",
                "-certstore",
                certStorePath,
                "-certstoretype",
                BKSTrustStore.STORAGE_TYPE,
                "-certstoreprovider",
                BouncyCastleProvider.PROVIDER_NAME,
                "-pass", String(CERT_STORE_PASSWORD),
                "-alias",
                alias,
                "-in",
                inFile,
                "-out",
                outFile,
                "-provider",
                JCSP.PROVIDER_NAME
            ) else arrayOf(
                "-decrypt",
                "-keystore",
                STORE_TYPE,
                "-pass",
                password,
                "-alias",
                alias,
                "-in",
                inFile,
                "-out",
                outFile,
                "-provider",
                JCSP.PROVIDER_NAME
            )
            for (arg: String in args) {
                Logger.log("* $arg")
            } // for
            try {
                CMSMain.main(args)
                Logger.setStatusOK()
                Logger.log("Completed.")
            } catch (e: Exception) {
                e.message?.let { Logger.log(it) }
                Logger.setStatusFailed()
                Log.e(Constants.APP_LOGGER_TAG, e.message, e)
            }
            return null
        }
    }

    /**
     * Сравнение файлов по содержимому.
     *
     */
    private fun compareFiles() {
        Logger.clear()
        Logger.log("*** CMSUtil, compare files ***")
        val inFile = etInFile!!.text.toString()
        val outFile = etOutFile!!.text.toString()
        Logger.log("* In file: $inFile")
        Logger.log("* Out file: $outFile")
        try {
            val inFileContent = Array.readFile(inFile)
            val outFileContent = Array.readFile(outFile)
            val equal = Array.compare(inFileContent, outFileContent)
            if (!equal) {
                Logger.setStatusFailed()
                Logger.log("Decrypted and source files are NOT equal.")
            } else {
                Logger.setStatusOK()
                Logger.log("Decrypted and source files are equal.")
            }
        } catch (e: Exception) {
            e.message?.let { Logger.log(it) }
            Logger.setStatusFailed()
            Log.e(Constants.APP_LOGGER_TAG, e.message, e)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onDestroy() {
        if (saveCertStoreTask != null) {
            Log.d(Constants.APP_LOGGER_TAG, "Cancel save certificate store task.")
            saveCertStoreTask!!.cancel(true)
        } // if
        if (encryptOrDecryptTask != null) {
            Log.d(Constants.APP_LOGGER_TAG, "Cancel encrypt/decrypt task.")
            encryptOrDecryptTask!!.cancel(true)
        }
        super.onDestroy()
    }

    companion object {
        /**
         * Файл хранилища из ресурсов.
         */
        private val CERT_STORE_NAME = "recipient.store"

        /**
         * Пароль к хранилищу сертификатов получателей
         * [.CERT_STORE_NAME].
         */
        private val CERT_STORE_PASSWORD = "1234".toCharArray()

        /**
         * Идентификатор запроса выбора входящего файла.
         */
        private val FILE_SELECT_CODE_IN = 0

        /**
         * Идентификатор запроса выбора папки для исходящего
         * файла.
         */
        private val FILE_SELECT_CODE_OUT = 1

        /**
         * Тип контейнера.
         */
        val STORE_TYPE = JCSP.HD_STORE_NAME
    }
}