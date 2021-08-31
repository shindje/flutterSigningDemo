import 'dart:typed_data';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:signing/data/documents.dart';
import 'package:intl/intl.dart';
import 'package:signing/data/files.dart';
import 'package:syncfusion_flutter_pdfviewer/pdfviewer.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:open_file/open_file.dart';

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

  _DetailsState(this.doc);

  void _approve() {
    setState(() {
      doc.state = "Согласован";
    });
  }


  Future<void> _sign() async {
    String? message;
    try {
      if (doc.file != null) {
        Uint8List data = doc.file!.readAsBytesSync();
        final Uint8List? result = await platform.invokeMethod('sign', data.buffer.asUint8List());
        message = 'Signed data size: ${result != null ? result.lengthInBytes : "empty"}';
        doc.signFile = await checkAndWrite(doc.assetPath! + ".sign", result);
        doc.state = "Подписан";
      } else
        message = "No file";
    } on PlatformException catch (e) {
      message = "${e.message}";
    }

    setState(() {
      if (message != null)
        Fluttertoast.showToast(msg: message);
    });
  }

  Future<void> _rework() async {
    if (doc.signFile != null)
      doc.signFile!.deleteSync();
    doc.signFile = null;
    doc.state = "Новый";
    setState(() {});
  }

  Future<void> _openFile(String filePath) async {
    final _result = await OpenFile.open(filePath);
    if (_result.type != ResultType.done)
      Fluttertoast.showToast(msg: _result.message);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
          title: Text('Просмотр документа'),
          actions: [
            if (doc.state == "Новый")
              Padding(
                padding: EdgeInsets.only(right: 20),
                child: GestureDetector(
                  onTap: () {
                    _approve();
                  },
                  child: Icon(
                    Icons.check_circle,
                    color: Colors.greenAccent,
                    size: 26,
                  ),
                ),
              ),
            if (doc.file != null && doc.state == "Согласован")
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
              ),
            if (doc.state == "Подписан")
              Padding(
                padding: EdgeInsets.only(right: 20),
                child: GestureDetector(
                  onTap: () {
                    _rework();
                  },
                  child: Icon(
                    Icons.clear,
                    color: Colors.red,
                    size: 26,
                  ),
                ),
              ),
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
            TextWithPadding("Статус: ${doc.state}"),
            if (doc.file != null)
              Row(children: [
                TextWithPadding("Файл: "),
                Expanded(
                  child:
                    TextButton(
                        onPressed: () { _openFile(doc.file!.path); },
                        child: TextWithPadding("${doc.file!.path}"),
                    ),
                  ),
              ],),
            if (doc.signFile != null)
              Row(children: [
                TextWithPadding("Подпись: "),
                Expanded(
                  child:
                  TextButton(
                    onPressed: () { _openFile(doc.signFile!.path); },
                    child: TextWithPadding("${doc.signFile!.path}"),
                  ),
                ),
              ],),
            if (doc.file != null && doc.assetPath!.endsWith(".pdf"))
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