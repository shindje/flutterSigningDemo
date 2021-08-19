import 'package:flutter/material.dart';
import 'package:signing/documents.dart';
import 'package:intl/intl.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Signing Demo',
      home: Scaffold(
        body: MyHomePage(title: 'Signing Demo'),
      )
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key? key, required this.title}) : super(key: key);
  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  List<Document> docs = [Document("1.1", null, "Doc 1"), Document("1.2", null, "Doc 2"),
                         Document("2", DateTime.parse("2012-02-27"), "Doc 2"),];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: ListView.builder(
          itemCount: docs.length * 2,
          itemBuilder: (BuildContext _context, int i) {
            if (i.isOdd) {
              return Divider();
            }
            final int index = i ~/ 2;
            final Document doc = docs[index];
            return ListTile (
              title: Row (
                children: [
                  Expanded(child: Text(doc.docNum)),
                  Expanded(child: Text(DateFormat.yMMMd().format(doc.docDate))),
                ],
              ),
            );
          }
        ),
      ),
    );
  }
}
