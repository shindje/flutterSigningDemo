import 'dart:io';

import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:intl/intl.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:signing/data/documents.dart';
import 'package:signing/data/files.dart';

class MyListView extends StatefulWidget {
  final List<Document> docList;
  final ValueChanged<Document> onTap;

  MyListView(this.docList,this.onTap);

  @override
  State<StatefulWidget> createState() => _MyListViewState(docList, onTap);
}

class _MyListViewState extends State<MyListView> {
  final List<Document> docList;
  final ValueChanged<Document> onTap;

  _MyListViewState(this.docList,this.onTap);

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    _loadFiles();
  }

  Future<bool> _checkStoragePermission() async {
    //TODO: check if platform == Android
    bool isGranted = await Permission.storage.isGranted;
    if (!isGranted) {
      isGranted = (await Permission.storage.request()).isGranted;
      if (!isGranted) {
        Fluttertoast.showToast(msg: "NO Storage Permission");
        return false;
      }
      return true;
    }
    return true;
  }

  Future<void> _loadFiles() async {
    if (!(await _checkStoragePermission()))
      return;

    for (var doc in docList) {

      if (doc.assetPath != null) {
        File? file = await checkAndWrite(doc.assetPath,
            (await DefaultAssetBundle
                .of(context)
                .load("attachments/${doc.assetPath!}")
            ).buffer.asUint8List()
        );
        doc.file = file;

        File? signFile = await checkFile(doc.assetPath! + ".sign");
        doc.signFile = signFile;
        if (signFile != null)
          doc.state = "Подписан";
      }
    }

    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Список документов'),),
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
                  Expanded(child: Text("№ ${doc.docNum}")),
                  Expanded(child: Text("от ${DateFormat.yMMMd().format(doc.docDate)}")),
                  Expanded(child: Text("${doc.state}", textAlign: TextAlign.right,)),
                ],
              ),
              onTap: () => onTap(doc),
            );
          }
      ),
    );
  }
}