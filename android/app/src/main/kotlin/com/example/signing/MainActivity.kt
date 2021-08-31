package com.example.signing

import android.Manifest
import android.app.ActionBar
import android.app.AlertDialog
import android.app.FragmentTransaction
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import com.example.signing.base.FinalListener
import com.example.signing.interfaces.HashData
import com.example.signing.util.*
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.apache.xml.security.utils.resolver.ResourceResolver
import org.bouncycastle.jce.provider.BouncyCastleProvider
import ru.CryptoPro.AdES.AdESConfig
import ru.CryptoPro.JCPxml.XmlInit
import ru.CryptoPro.JCPxml.dsig.internal.dom.XMLDSigRI
import ru.CryptoPro.JCSP.CSPConfig
import ru.CryptoPro.JCSP.JCSP
import ru.CryptoPro.JCSP.JCSP.PROVIDER_NAME
import ru.CryptoPro.JCSP.support.BKSTrustStore
import ru.CryptoPro.reprov.RevCheck
import ru.CryptoPro.ssl.util.cpSSLConfig
import ru.cprocsp.ACSP.tools.common.AppUtils
import ru.cprocsp.ACSP.tools.common.CSPTool
import ru.cprocsp.ACSP.tools.wait_task.AsyncTaskManager
import ru.cprocsp.ACSP.tools.wait_task.OnTaskCompleteListener
import ru.cprocsp.ACSP.tools.wait_task.Task
import ru.cprocsp.ACSP.util.PermissionHelper
import java.io.File
import java.io.FileInputStream
import java.security.Provider
import java.security.Security
import java.util.ArrayList

class MainActivity: FlutterActivity(), AdapterView.OnItemSelectedListener {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory("ru.esoft/signingView", SigningViewFactory())
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.example/SigningView").setMethodCallHandler {
                call, result ->
                if (call.method == "sign") {

                    val builder = AlertDialog.Builder(this)
                    builder.setMessage("Выбор контейнера")
                        .setTitle("Подписание")

                    val dialogView = layoutInflater.inflate(R.layout.signing_types_dialog, null)
                    dialogInit(dialogView)

                    builder
                        .setView(dialogView)
                        .setPositiveButton("Подписать"
                        ) { _, id ->
                            val selected = spClientList.getSelectedItem() as String?
                            if (selected.isNullOrBlank())
                                result.error("cancelErrorCode", "Контейнер не выбран", null)
                            val bytes = call.arguments as ByteArray
                            doSign(bytes, object: FinalListener {
                                override fun onComplete(res: Any?) {
                                    runOnUiThread {
                                        if (res != null)
                                            result.success(res as ByteArray)
                                        else
                                            result.success(res)
                                    }
                                }
                            })
                        }
                            .setNegativeButton("Отмена"
                        ) { _, id ->
                            result.error("cancelErrorCode", "Подпись отменена", null)
                        }

                    val dialog = builder.create()
                    dialog.show()
                }
        }
    }

    private fun dialogInit(v: View) {
        // Тип контейнера.
        spKeyStoreType = v.findViewById(R.id.dlgSpKeyStore) as Spinner

        // Получение списка поддерживаемых типов хранилищ.
        val keyStoreTypeList: List<String> = KeyStoreType.keyStoreTypeList

        // Создаем ArrayAdapter для использования строкового массива
        // и способа отображения объекта.
        val keyStoreTypeAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item,
            keyStoreTypeList
        )

        // Способ отображения.
        keyStoreTypeAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spKeyStoreType.adapter = keyStoreTypeAdapter
        spKeyStoreType.onItemSelectedListener = this

        // Выбираем сохраненный ранее тип.
        keyStoreTypeIndex = keyStoreTypeAdapter.getPosition(KeyStoreType.currentType())
        spKeyStoreType.setSelection(keyStoreTypeIndex)
///////////////////////////////////////////////////////////////////////////////////////

        // Тип провайдера.
        spProviderType = v.findViewById(R.id.dlgSpProviderType) as Spinner

        // Создаем ArrayAdapter для использования строкового массива
        // и способа отображения объекта.
        val providerTypeAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.providerTypes, android.R.layout.simple_spinner_item
        )

        // Способ отображения.
        providerTypeAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spProviderType.adapter = providerTypeAdapter
        spProviderType.onItemSelectedListener = this

        // Выбираем сохраненный ранее тип.
        providerTypeIndex = providerTypeAdapter.getPosition(ProviderType.currentType())
        spProviderType.setSelection(providerTypeIndex)
///////////////////////////////////////////////////////////////////////////////////////

        // Список клиентских алиасов.
        spClientList = v.findViewById(R.id.dlgSpExamplesClientList) as Spinner

        containerAliasAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item
        )

        // Способ отображения.


        // Способ отображения.
        containerAliasAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spClientList.adapter = containerAliasAdapter

        myLoadClientList(containerAliasAdapter)
///////////////////////////////////////////////////////////////////////////////////////
        val btnRefresh = v.findViewById(R.id.btnRefresh) as Button
        btnRefresh.setOnClickListener {
            myLoadClientList(containerAliasAdapter)
        }

//        val btnSign = findViewById(R.id.btnSign) as Button
//        btnSign.setOnClickListener {
//            doSign()
//        }

    }

    /**
     * Список контейнеров по заданному типу.
     */
    private val cacheAllAliases: MutableList<String> = ArrayList()

    /**
     * Список контейнеров по типу [UtilActivity.STORE_TYPE].
     */
    private val cacheHDAliases: MutableList<String> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(
            Constants.APP_LOGGER_TAG, "Load application: " +
                    packageName
        )

        // 1. Инициализация провайдеров: CSP и java-провайдеров
        // (Обязательная часть).
        if (!initCSPProviders()) {
            Log.i(Constants.APP_LOGGER_TAG, "Couldn't initialize CSP.")
            return
        } // if


        initJavaProviders()

        // 2. Копирование тестовых контейнеров для подписи,
        // проверки подписи, шифрования и TLS (Примеры и вывод
        // в лог).
//        initLogger()
        installContainers()

        // 3. Инициируем объект для управления выбором типа
        // контейнера (Настройки).
        KeyStoreType.init(this)

        // 4. Инициируем объект для управления выбором типа
        // провайдера (Настройки).
        ProviderType.init(this)

        // 5. Вывод информации о тестовых контейнерах.
        logTestContainers()

        // 6. Вывод информации о провайдере и контейнерах
        // (Пример).
        logJCspServices()


        // 8. Логирование. Для логирования:
        // CSPConfig.setNeedLogBioStatistics(true);

        // 9. Запрос прав на запись.
        PermissionHelper.checkPermissions(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            PermissionHelper.PERMISSION_REQUEST_CODE_WRITE_STORAGE
        )

        containerAliasAdapter = ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item
        )
    }

    private lateinit var spKeyStoreType: Spinner
    private lateinit var spProviderType: Spinner
    private lateinit var spClientList: Spinner
    private lateinit var containerAliasAdapter: ArrayAdapter<String>

    private val EXAMPLE_PACKAGE = "com.example.signing.client."
    private fun doSign(data: ByteArray, finalListener: FinalListener) {

        val exampleClassName = "VerifyExample"


        // Поиск примера.
        val fullExampleClassName: String = EXAMPLE_PACKAGE + exampleClassName
        val exampleClass = Class.forName(fullExampleClassName)

        try {
            val exampleConstructor = exampleClass.getConstructor(
                ContainerAdapter::class.java
            )

            // Настройки примера.
            val adapter = ContainerAdapter(
                this,
                spClientList.getSelectedItem() as String,  /*clientPassword*/
                null,
                null, //spServerList.getSelectedItem() as String,
                null
            )

            adapter.providerType =ProviderType.currentProviderType()
            adapter.resources = resources // для примера установки сертификатов


            // По наличию в списке ниже данного примера определяем,
            // включена ли аутентификация. Только для TLS примеров!


            // По наличию в списке ниже данного примера определяем,
            // включена ли аутентификация. Только для TLS примеров!
            val clientAuth = false
            //val clientAuth = Arrays.asList<String>(*examplesRequireWrittenPin)
            //  .contains(exampleClassName) // для TLS примеров


            // Используется общее для всех хранилище корневых
            // сертификатов cacerts.


            // Используется общее для всех хранилище корневых
            // сертификатов cacerts.
            val trustStorePath: String = this.applicationInfo.dataDir +
                    File.separator + BKSTrustStore.STORAGE_DIRECTORY + File.separator +
                    BKSTrustStore.STORAGE_FILE_TRUST

            Logger.log("Example trust store: $trustStorePath")

            adapter.trustStoreProvider = BouncyCastleProvider.PROVIDER_NAME
            adapter.trustStoreType = BKSTrustStore.STORAGE_TYPE

            adapter.trustStoreStream = FileInputStream(trustStorePath)
            adapter.trustStorePassword = BKSTrustStore.STORAGE_PASSWORD

            // Настройки для подключения к удаленному хосту в зависимости
            // от алгоритма (чтобы охватить по возможности все алгоритмы)
            // для TLS примеров, примера построения цепочки и т.п.

            when (adapter.providerType) {
                AlgorithmSelector.DefaultProviderType.pt2001 -> {


                    // Для TLS примеров.
                    if (clientAuth) {
                        adapter.setConnectionInfo(RemoteConnectionInfo.host2001ClientAuth)
                    } // if
                    else {
                        adapter.setConnectionInfo(RemoteConnectionInfo.host2001NoAuth)
                    } // else
                }
                AlgorithmSelector.DefaultProviderType.pt2012Short -> {


                    // Для TLS примеров.
                    if (clientAuth) {
                        adapter.setConnectionInfo(RemoteConnectionInfo.host2012256ClientAuth)
                    } // if
                    else {
                        adapter.setConnectionInfo(RemoteConnectionInfo.host2012256NoAuth)
                    } // else
                }
                AlgorithmSelector.DefaultProviderType.pt2012Long -> {


                    // Для TLS примеров.
                    if (clientAuth) {
                        adapter.setConnectionInfo(RemoteConnectionInfo.host2012512ClientAuth)
                    } // if
                    else {
                        adapter.setConnectionInfo(RemoteConnectionInfo.host2012512NoAuth)
                    } // else
                }
            }

            // Выполнение примера.


            // Выполнение примера.
            val exampleImpl: HashData = exampleConstructor.newInstance(adapter) as HashData
            exampleImpl.getResult(data, finalListener)

        } catch (e: Exception) {
            Logger.log(e.message!!)
            Logger.setStatusFailed()
            Log.e(Constants.APP_LOGGER_TAG, e.message, e)
        }
    }


    private fun myLoadClientList(containerAliasAdapter: ArrayAdapter<String>) {
        val containerManager =
            AsyncTaskManager(this, ContainerListener(containerAliasAdapter))
        //?????containerManager.handleRetainedTask(lastCustomNonConfigurationInstance) // восстанавливаем задачу


        if (!containerManager.isWorking()) { // если нет - запускаем задачу
            containerManager.setupTask(ContainerTask())
        } // if
        else {
            AppUtils.alertToast(this, getString(R.string.PBMessageAlreadyRunning))
        } // else

    }

    /**
     * Номер выбранного типа хранилища в списке.
     */
    private var keyStoreTypeIndex = 0

    /**
     * Номер выбранного типа провайдера в списке.
     */
    private var providerTypeIndex = 0

    /**
     * Флаг, сообщающий что настройки изменились
     * (тип хранилища, алгоритм ключа, копирование
     * контейнера). Если true, то нужно сказать обновить
     * список контейнеров в примерах, когда придет запрос
     * от MainActivity.
     */
    private var settingsChanged = false

    override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
        when (adapterView.id) {
            R.id.dlgSpKeyStore -> {
                if (keyStoreTypeIndex != i) {
                    val keyStoreType = adapterView.getItemAtPosition(i) as String
                    KeyStoreType.saveCurrentType(keyStoreType)
                    keyStoreTypeIndex = i
                    settingsChanged = true // настройка изменилась
                } // if
            }
            R.id.dlgSpProviderType -> {
                if (providerTypeIndex != i) {
                    val provType = adapterView.getItemAtPosition(i) as String
                    ProviderType.saveCurrentType(provType)
                    providerTypeIndex = i
                    settingsChanged = true // настройка изменилась
                } // if
            }
        }
        myLoadClientList(containerAliasAdapter)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}


    /**
     * Задача обновления списка контейнеров.
     *
     */
    inner class ContainerTask
    /**
     * Конструктор (UI Thread).
     */
        : Task(getString(R.string.PBMessageWait)) {
        /**
         * Список алиасов контейнеров заданного типа.
         */
        val allAliases: MutableList<String> = ArrayList()

        /**
         * Список алиасов контейнеров типа [UtilActivity.STORE_TYPE].
         */
        val hdAliases: MutableList<String> = ArrayList()
        override fun doInBackground(vararg arg0: Void): Int {
            Log.d("<<ContainerTask>>", "${KeyStoreType.currentType()} ${ProviderType.currentProviderType()}")

            allAliases.addAll(
                KeyStoreUtil.aliases(
                    KeyStoreType.currentType(),
                    ProviderType.currentProviderType()
                )
            )
            hdAliases.addAll(
                KeyStoreUtil.aliases(
                    UtilActivity.STORE_TYPE,
                    ProviderType.currentProviderType()
                )
            )
            return RESULT_OK
        }
    }

    /**
     * Реакция на обновление.
     *
     */
    inner class ContainerListener(val containerAliasAdapter: ArrayAdapter<String>) :
        OnTaskCompleteListener {
        override fun onTaskComplete(task: Task) {
            if (task.isCancelled) {
                Log.d(Constants.APP_LOGGER_TAG, "Task cancelled.")
                return
            } // if
            try {
                task.get()
            } catch (e: Exception) {
                Log.d(Constants.APP_LOGGER_TAG, "Invalid task result.", e)
                return
            }

            // Обновляем элементы вкладок.
            synchronized(this@MainActivity) {
                this@MainActivity.cacheAllAliases.clear()
                this@MainActivity.cacheHDAliases.clear()
                this@MainActivity.cacheAllAliases.addAll((task as ContainerTask).allAliases)
                this@MainActivity.cacheHDAliases.addAll(task.hdAliases)
//                updateFragment(MainActivity.TAB_EXAMPLES)
//                updateFragment(MainActivity.TAB_UTILITIES)

                containerAliasAdapter.clear()
                containerAliasAdapter.addAll(cacheAllAliases)
                containerAliasAdapter.notifyDataSetChanged()
            }
        }
    }


    /************************ Инициализация провайдера ************************/

    /************************ Инициализация провайдера  */
    /**
     * Инициализация CSP провайдера.
     *
     * @return True в случае успешной инициализации.
     */
    private fun initCSPProviders(): Boolean {

        // Инициализация провайдера CSP. Должна выполняться
        // один раз в главном потоке приложения, т.к. использует
        // статические переменные.
        //
        // Далее может быть использована версия функции инициализации:
        // 1) расширенная - initEx(): она содержит init() и дополнительно
        // выполняет загрузку java-провайдеров (Java CSP, RevCheck, Java TLS)
        // и настройку некоторых параметров, например, Java TLS;
        // 2) обычная - init(): без загрузки java-провайдеров и настройки
        // параметров.
        //
        // Для совместного использования ГОСТ и не-ГОСТ TLS НЕ следует
        // переопределять свойства System.getProperty(javax.net.*) и
        // Security.setProperty(ssl.*).
        //
        // Ниже используется обычная версия init() функции инициализации
        // и свойства TLS переопределяются, т.к. в приложении имеется пример
        // работы с УЦ 1.5, который обращается к свойствам по умолчанию.
        //
        // 1. Создаем инфраструктуру CSP и копируем ресурсы
        // в папку. В случае ошибки мы, например, выводим окошко
        // (или как-то иначе сообщаем) и завершаем работу.
        val initCode = CSPConfig.init(this)
        val initOk = initCode == CSPConfig.CSP_INIT_OK

        // Если инициализация не удалась, то сообщим об ошибке.
        if (!initOk) {
            when (initCode) {
                CSPConfig.CSP_INIT_CONTEXT -> AppUtils.errorMessage(
                    this,
                    "Couldn't initialize context."
                )
                CSPConfig.CSP_INIT_CREATE_INFRASTRUCTURE -> AppUtils.errorMessage(
                    this,
                    "Couldn't create CSP infrastructure."
                )
                CSPConfig.CSP_INIT_COPY_RESOURCES -> AppUtils.errorMessage(
                    this,
                    "Couldn't copy CSP resources."
                )
                CSPConfig.CSP_INIT_CHANGE_WORK_DIR -> AppUtils.errorMessage(
                    this,
                    "Couldn't change CSP working directory."
                )
                CSPConfig.CSP_INIT_INVALID_LICENSE -> AppUtils.errorMessage(
                    this,
                    "Invalid CSP serial number."
                )
                CSPConfig.CSP_TRUST_STORE_FAILED -> AppUtils.errorMessage(
                    this,
                    "Couldn't create trust store for CAdES API."
                )
                CSPConfig.CSP_STORE_LIBRARY_PATH -> AppUtils.errorMessage(
                    this,
                    "Couldn't store native library path to config."
                )
                CSPConfig.CSP_INIT_INVALID_INTEGRITY -> AppUtils.errorMessage(
                    this,
                    "Integrity control failure."
                )
            }
        } // if
        return initOk
    }

    /**
     * Добавление нативного провайдера Java CSP,
     * SSL-провайдера и Revocation-провайдера в
     * список Security. Инициализируется JCPxml,
     * CAdES.
     *
     * Происходит один раз при инициализации.
     * Возможно только после инициализации в CSPConfig!
     *
     */
    private fun initJavaProviders() {

        // %%% Инициализация остальных провайдеров %%%

        //
        // Загрузка Java CSP (хеш, подпись, шифрование,
        // генерация контейнеров).
        //
        if (Security.getProvider(JCSP.PROVIDER_NAME) == null) {
            Security.addProvider(JCSP())
        } // if

        //
        // Загрузка Java TLS (TLS).
        //
        // Необходимо переопределить свойства, чтобы в случае HttpsURLConnection
        // + system-свойства использовались менеджеры из cpSSL, а не Harmony.
        //
        // Внимание!
        // Чтобы не мешать не-ГОСТовой реализации, ряд свойств внизу *.ssl и
        // javax.net.* НЕ следует переопределять! Но при этом не исключены проблемы
        // в работе с ГОСТом там, где TLS-реализация клиента обращается к дефолтным
        // алгоритмам реализаций этих factory (особенно: apache http client или
        // HttpsURLConnection без явной передачи предварительно созданного SSLContext
        // и его SSLSocketFactory).
        // То есть если используется HttpsURLConnection + свойства хранилищ javax.net.*,
        // заданные через System.setProperty(), то переопределения свойств *.ssl ниже
        // нужны.
        // Рекомендуемый вариант: использовать ok http и другие подобные реализации
        // с явным созданием SSLContext и передачей его SSLSocketFactory в клиент
        // Ok http.
        //
        // Здесь эти свойства включены, т.к. пример УЦ 1.5 использует алгоритмы
        // по умолчанию.
        //
        // Если инициализировать провайдер в CSPConfig с помощью initEx(), то
        // свойства будут включены там, поэтому выше используется упрощенная
        // версия инициализации.
        //
        Security.setProperty(
            "ssl.KeyManagerFactory.algorithm",
            ru.CryptoPro.ssl.Provider.KEYMANGER_ALG
        )
        Security.setProperty(
            "ssl.TrustManagerFactory.algorithm",
            ru.CryptoPro.ssl.Provider.KEYMANGER_ALG
        )
        Security.setProperty("ssl.SocketFactory.provider", "ru.CryptoPro.ssl.SSLSocketFactoryImpl")
        Security.setProperty(
            "ssl.ServerSocketFactory.provider",
            "ru.CryptoPro.ssl.SSLServerSocketFactoryImpl"
        )

        // Добавление TLS провайдера.
        if (Security.getProvider(ru.CryptoPro.ssl.Provider.PROVIDER_NAME) == null) {
            Security.addProvider(ru.CryptoPro.ssl.Provider())
        } // if

        //
        // Провайдер хеширования, подписи, шифрования
        // по умолчанию.
        //
        cpSSLConfig.setDefaultSSLProvider(JCSP.PROVIDER_NAME)

        //
        // Загрузка Revocation Provider (CRL, OCSP).
        //
        if (Security.getProvider(RevCheck.PROVIDER_NAME) == null) {
            Security.addProvider(RevCheck())
        } // if

        //
        // Отключаем проверку цепочки штампа времени (CAdES-T),
        // чтобы не требовать него CRL.
        //
        System.setProperty("ru.CryptoPro.CAdES.validate_tsp", "false")

        //
        // Таймауты для CRL на всякий случай.
        //
        System.setProperty("com.sun.security.crl.timeout", "5")
        System.setProperty("ru.CryptoPro.crl.read_timeout", "5")

        // Задание провайдера по умолчанию для CAdES.
        AdESConfig.setDefaultProvider(JCSP.PROVIDER_NAME)

        // Инициализация XML DSig (хеш, подпись).
        XmlInit.init()

        // Добавление реализации поиска узла по ID.
        ResourceResolver.registerAtStart(XmlInit.JCP_XML_DOCUMENT_ID_RESOLVER)

        // Добавление XMLDSigRI провайдера, так как его
        // использует XAdES.
        val xmlDSigRi: Provider = XMLDSigRI()
        Security.addProvider(xmlDSigRi)
        val provider = Security.getProvider("XMLDSig")
        if (provider != null) {
            provider["XMLSignatureFactory.DOM"] =
                "ru.CryptoPro.JCPxml.dsig.internal.dom.DOMXMLSignatureFactory"
            provider["KeyInfoFactory.DOM"] =
                "ru.CryptoPro.JCPxml.dsig.internal.dom.DOMKeyInfoFactory"
        } // if

        // Включаем возможность онлайновой проверки статуса
        // сертификата.
        //
        // Для TLS проверку цепочки сертификатов другой стороны
        // можно отключить, если создать параметр
        // Enable_revocation_default=false в файле android_pref_store
        // (shared preferences), см.
        // {@link ru.CryptoPro.JCP.tools.pref_store#AndroidPrefStore}.
        //
        // В случае создания подписей формата BES или T можно отключить
        // проверку цепочки сертификатов подписанта (и службы) с помощью
        // параметра:
        // cAdESSignature.setOptions((new Options()).disableCertificateValidation()); // CAdES
        // или
        // xAdESSignature.setOptions((new Options()).disableCertificateValidation()); // XAdES
        // перед добавлением подписанта.
        // По умолчанию проверка цепочки сертификатов подписанта всегда
        // включена.
        System.setProperty("com.sun.security.enableCRLDP", "true")
        System.setProperty("com.ibm.security.enableCRLDP", "true")

        // Свойство, препятствующее созданию дефолтного (static) контекста
        // при использовании HttpsURLConnection без system-свойств и
        // динамически создаваемого SSL контекста в TLS-примерах.
        // См. {@link ru.CryptoPro.ACSPClientApp.client.example.base.TLSData}
        // См. {@link ru.CryptoPro.ssl.JavaTLSKeyStoreParameter} в cpSSL-javadoc.jar
        System.setProperty("disable_default_context", "true")

        //
        // Настройки TLS для генерации контейнера и выпуска сертификата
        // в УЦ 2.0, т.к. обращение к УЦ 2.0 будет выполняться по протоколу
        // HTTPS и потребуется авторизация по сертификату. Указываем тип
        // хранилища с доверенным корневым сертификатом, путь к нему и пароль.
        //
        // Внимание!
        // Чтобы не мешать не-ГОСТовой реализации, ряд свойств внизу *.ssl и
        // javax.net.* НЕ следует переопределять! Но при этом не исключены проблемы
        // в работе с ГОСТом там, где TLS-реализация клиента обращается к дефолтным
        // алгоритмам реализаций этих factory (особенно: apache http client или
        // HttpsURLConnection без явной передачи предварительно созданного SSLContext
        // и его SSLSocketFactory).
        // То есть если используется HttpsURLConnection + свойства хранилищ javax.net.*,
        // заданные через System.setProperty(), то переопределения свойств *.ssl ниже
        // нужны.
        // Рекомендуемый вариант: использовать ok http и другие подобные реализации
        // с явным созданием SSLContext и передачей его SSLSocketFactory в клиент
        // Ok http.
        //
        // Здесь эти свойства включены, т.к. пример УЦ 1.5 использует алгоритмы
        // по умолчанию. Примеров УЦ 2.0 пока нет.
        //
        val trustStorePath = applicationInfo.dataDir + File.separator +
                BKSTrustStore.STORAGE_DIRECTORY + File.separator + BKSTrustStore.STORAGE_FILE_TRUST
        val trustStorePassword = String(BKSTrustStore.STORAGE_PASSWORD)
        Log.d(
            Constants.APP_LOGGER_TAG,
            "Default trust store: $trustStorePath"
        )
        System.setProperty("javax.net.ssl.trustStoreType", BKSTrustStore.STORAGE_TYPE)
        System.setProperty("javax.net.ssl.trustStore", trustStorePath)
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword)
    }

    /************************ Поле для вывода логов *************************/

    /************************ Поле для вывода логов  */
    /**
     * Инициализация объекта для отображения логов.
     *
     */
    private fun initLogger() {

        // Поле для вывода логов и метка для отображения
        // статуса.
        val etLog = findViewById<View>(R.id.etLog) as EditText
        etLog.minLines = 10
        val tvOpStatus = findViewById<View>(R.id.tvOpStatus) as TextView
        Logger.init(resources, etLog, tvOpStatus)
        Logger.clear()
    }

    /************************** Служебные функции ****************************/

    /************************** Служебные функции  */
    /**
     * Вывод списка поддерживаемых алгоритмов.
     *
     */
    private fun logJCspServices() {
        ProviderServiceInfo.logServiceInfo(KeyStoreUtil.DEFAULT_PROVIDER)
    }

    /**
     * Информация о тестовых контейнерах.
     *
     */
    private fun logTestContainers() {

        // Список алиасов контейнеров.
        val aliases = arrayOf<String>(
            Containers.CLIENT_CONTAINER_NAME,  // ГОСТ 34.10-2001
            Containers.SERVER_CONTAINER_NAME,  // ГОСТ 34.10-2001
            Containers.CLIENT_CONTAINER_2012_256_NAME,  // ГОСТ 34.10-2012 (256)
            Containers.SERVER_CONTAINER_2012_256_NAME,  // ГОСТ 34.10-2012 (256)
            Containers.CLIENT_CONTAINER_2012_512_NAME,  // ГОСТ 34.10-2012 (512)
            Containers.SERVER_CONTAINER_2012_512_NAME // ГОСТ 34.10-2012 (512)
        )

        // Список паролей контейнеров.
        val passwords = arrayOf<CharArray>(
            Containers.CLIENT_KEY_PASSWORD,
            Containers.SERVER_KEY_PASSWORD,
            Containers.CLIENT_KEY_2012_256_PASSWORD,
            Containers.CLIENT_KEY_2012_256_PASSWORD,
            Containers.CLIENT_KEY_2012_512_PASSWORD,
            Containers.CLIENT_KEY_2012_512_PASSWORD
        )
        val format = getString(R.string.ContainerAboutTestContainer)
        Logger.log("$$$ About test containers $$$")
        for (i in aliases.indices) {
            val aboutTestContainer = String.format(
                format,
                aliases[i], passwords[i].toString()
            )
            Logger.log("** $i) $aboutTestContainer")
        } // for
    }

    /**
     * Копирование тестовых контейнеров для подписи,
     * шифрования, обмена по TLS из архива в папку
     * keys приложения.
     *
     */
    private fun installContainers() {
        val cspTool = CSPTool(this)
        cspTool.appInfrastructure.copyContainerFromArchive(R.raw.keys)
    }

}
