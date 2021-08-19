import 'package:flutter/material.dart';
import 'package:signing/data/documents.dart';
import 'package:intl/intl.dart';

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

class DetailsScreen extends StatelessWidget {
  final Document doc;

  DetailsScreen(this.doc);

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