package com.example.signing

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory("ru.esoft/signingView", SigningViewFactory())
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.example/SigningView").setMethodCallHandler {
                call, result ->
                if (call.method == "setNumber") {
                    result.success(SigningViewFactory.signingView?.setText(call.arguments as Int))
                }
        }
    }
}
