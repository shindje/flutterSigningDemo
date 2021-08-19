import 'dart:typed_data';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:signing/data/documents.dart';
import 'package:intl/intl.dart';
import 'package:flutter_full_pdf_viewer/full_pdf_viewer_scaffold.dart';

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
  _DetailsState(this.doc);

  Future<String> preparePdf() async {
    if (doc.filePath.isEmpty)
      return "";

    final ByteData bytes = await DefaultAssetBundle.of(context).load(doc.filePath);
    final Uint8List list = bytes.buffer.asUint8List();
    final tempDir = await getTemporaryDirectory();
    final tempDocPath = '${tempDir.path}/${doc.filePath}';
    final file = await File(tempDocPath).create(recursive: true);
    file.writeAsBytesSync(list);
    return tempDocPath;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Padding(
        padding: const EdgeInsets.all(8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextWithPadding("Номер: ${doc.docNum}"),
            TextWithPadding("Дата: ${DateFormat.yMMMd().format(doc.docDate)}"),
            TextWithPadding("Описание: ${doc.desc}"),
            TextButton(onPressed: () => {
              preparePdf().then((path) =>
                Navigator.push(context,
                  MaterialPageRoute(builder:
                    (context) => PDFViewerScaffold(path: path)
                  )  
                )    
              )
            }, child: Text("Открыть файл"))
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