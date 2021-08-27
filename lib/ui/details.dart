import 'dart:math';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:signing/data/documents.dart';
import 'package:intl/intl.dart';
import 'package:syncfusion_flutter_pdfviewer/pdfviewer.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'native_signing_view.dart';

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

  Future<void> _sign() async {
    String message;
    try {
      ByteData data = await DefaultAssetBundle.of(context).load(doc.filePath);
      final Uint8List? result = await platform.invokeMethod('sign', data.buffer.asUint8List());
      message = 'Returned data size: ${result != null ? result.lengthInBytes : "empty"}';
    } on PlatformException catch (e) {
      message = "Error: '${e.message}'.";
    }

    setState(() {
      Fluttertoast.showToast(msg: message);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
          title: Text('Просмотр документа'),
          actions: [
            Padding(
              padding: EdgeInsets.only(right: 20),
              child: GestureDetector(
                onTap: () {
                  _sign();
                },
                child: Icon(
                  Icons.refresh,
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
            TextWithPadding("Номер: ${doc.docNum}"),
            TextWithPadding("Дата: ${DateFormat.yMMMd().format(doc.docDate)}"),
            TextWithPadding("Описание: ${doc.desc}"),
            if (doc.filePath.endsWith(".pdf"))
              Expanded(
                child: SfPdfViewer.asset(doc.filePath),
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