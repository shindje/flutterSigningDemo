import 'package:flutter/material.dart';
import 'package:signing/data/documents.dart';
import 'package:signing/ui/mylistview.dart';
import 'package:signing/ui/details.dart';

import 'data/files.dart';

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
  Document? _doc;

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
        onWillPop: () {
          // Navigator.pop(context);
          if (_doc == null)
            return Future.value(true);
          setState(() {
            _doc = null;
          });
          return Future.value(false);
        },
      child: Navigator(
        pages: [
          MaterialPage(
            key: ValueKey("listPage"),
              child: MyListView(mocked(), _handleTap)
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
      ),
    );
  }

  void _handleTap(Document doc) {
    setState(() {
      _doc = doc;
    });
  }
}