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
  Document? _doc;

  @override
  Widget build(BuildContext context) {
    return Navigator(
      pages: [
        MaterialPage(
          key: ValueKey("listPage"),
            child: MyListView(docs, _handleTap)
        ),
        if (_doc != null)
          DetailsPage(_doc!)
      ],
      onPopPage: (route, result) {
        if (!route.didPop(result))
          return false;

        setState(() {
          _doc = null;
        });
        return true;
      },
    );
  }

  void _handleTap(Document doc) {
    setState(() {
      _doc = doc;
    });
  }
}

class MyListView extends StatelessWidget {
  final List<Document> docList;
  final ValueChanged<Document> onTap;

  MyListView(this.docList,this.onTap);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: ListView.builder(
          itemCount: docList.length * 2,
          itemBuilder: (BuildContext _context, int i) {
            if (i.isOdd) {
              return Divider();
            }
            final int index = i ~/ 2;
            final Document doc = docList[index];
            return ListTile (
              title: Row (
                children: [
                  Expanded(child: Text(doc.docNum)),
                  Expanded(child: Text(DateFormat.yMMMd().format(doc.docDate))),
                ],
              ),
              onTap: () => onTap(doc),
            );
          }
      ),
    );
  }
}

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
