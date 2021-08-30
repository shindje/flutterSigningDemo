import 'dart:typed_data';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:signing/data/documents.dart';
import 'package:intl/intl.dart';
import 'package:syncfusion_flutter_pdfviewer/pdfviewer.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';

class DetailsPage extends Page {
  final Document doc;

  DetailsPage(this.doc) : super(key: ValueKey(doc));

  @override
  Route createRoute(BuildContext context) {
    return MaterialPageRoute(
        settings: this,
        builder: (BuildContext context) {
          return DetailsScreen(doc);
        }
    );
  }
}

class DetailsScreen extends StatefulWidget {
  final Document doc;

  DetailsScreen(this.doc);

  @override
  State<StatefulWidget> createState() => _DetailsState(doc);

}

class _DetailsState extends State<DetailsScreen> {
  final Document doc;
  static const platform = MethodChannel('com.example/SigningView');
  Directory? extdir;

  _DetailsState(this.doc);

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    _loadFiles();
  }

  Future<void> _loadFiles() async {
    //TODO: check if platform == Android
    bool isGranted = await Permission.storage.isGranted;
    if (!isGranted) {
      isGranted = (await Permission.storage.request()).isGranted;
      if (!isGranted) {
        Fluttertoast.showToast(msg: "NO Storage Permission");
        return;
      }
    }
    if (doc.assetPath != null) {
      extdir = await getExternalStorageDirectory();
      var file = File(extdir!.path + "/" + doc.assetPath!);
      var exists = await file.exists();
      if (!exists) {
        var existsDir = await extdir!.exists();
        if (!existsDir)
          extdir!.create();
        file = await File(file.path).create();
        var bytes = await DefaultAssetBundle.of(context).load("attachments/${doc.assetPath!}");
        file.writeAsBytes(bytes.buffer.asUint8List());
      }
      doc.file = file;
      doc.fileLength = await file.length();
      setState(() {

      });

    }
  }

  Future<void> _sign() async {
    String? message;
    try {
      if (doc.file != null) {
        Uint8List data = await doc.file!.readAsBytes();
        final Uint8List? result = await platform.invokeMethod('sign', data.buffer.asUint8List());
        //message = 'Signed data size: ${result != null ? result.lengthInBytes : "empty"}';
        doc.signFile = File(extdir!.path + "/" + doc.assetPath! + ".sign");
        doc.signFile!.writeAsBytesSync(result!);
        doc.signFileLength = doc.signFile!.lengthSync();
      } else
        message = "No file";
    } on PlatformException catch (e) {
      message = "Error: '${e.message}'.";
    }

    setState(() {
      if (message != null)
        Fluttertoast.showToast(msg: message);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
          title: Text('Просмотр документа'),
          actions: [
            if (doc.file != null)
              Padding(
                padding: EdgeInsets.only(right: 20),
                child: GestureDetector(
                  onTap: () {
                    _sign();
                  },
                  child: Icon(
                    Icons.border_color,
                    size: 26,
                  ),
                ),
              )
          ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                TextWithPadding("Номер: ${doc.docNum}"),
                TextWithPadding("Дата: ${DateFormat.yMMMd().format(doc.docDate)}"),
              ]
            ),
            TextWithPadding("Описание: ${doc.desc}"),
            TextWithPadding("Файл: ${doc.file?.path ?? ""}"),
            TextWithPadding("Размер файла: ${doc.fileLength ?? ""}"),
            TextWithPadding("Подпись: ${doc.signFile?.path ?? ""}"),
            TextWithPadding("Размер подписи: ${doc.signFileLength ?? ""}"),
            if (doc.file != null)
              Expanded(
                child: SfPdfViewer.file(doc.file!),
              ),
              //Expanded(child: AndroidSigning()),
          ],
        ),
      ),
    );
  }
}


class TextWithPadding extends StatelessWidget {
  String _text;
  TextWithPadding(this._text);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Text(_text),
    );
  }

}