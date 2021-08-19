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
            Text("Номер: ${doc.docNum}"),
            Text("Дата: ${DateFormat.yMMMd().format(doc.docDate)}"),
            Text("Описание: ${doc.desc}"),
          ],
        ),
      ),
    );
  }
}
