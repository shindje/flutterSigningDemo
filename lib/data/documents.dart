import 'dart:io';
import 'package:flutter/cupertino.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:fluttertoast/fluttertoast.dart';

import 'files.dart';

class Document {
  String docNum;
  DateTime docDate;
  String desc;
  String? assetPath;
  File? file;
  File? signFile;
  String state = "Новый";

  Document(this.docNum, this.docDate, this.desc, this.assetPath);

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

  Future<void> loadFiles(BuildContext context) async {
    if (!(await _checkStoragePermission()))
      return;

    if (assetPath != null) {
      File? check = await checkAndWrite(assetPath,
          (await DefaultAssetBundle
              .of(context)
              .load("attachments/${assetPath!}")
          ).buffer.asUint8List()
      );
      file = check;

      File? signCheck = await checkFile(assetPath! + ".sign");
      signFile = signCheck;
      if (signFile != null)
        state = "Подписан";
    }
  }
}

List<Document>? _docs;

List<Document> mocked() {
  if (_docs == null)
    _docs = [
      Document("1.1", DateTime.now(), "Документ о чем-то", "soprovod.pdf"),
      Document("1.2", DateTime.parse("2020-06-15"), "Документ о направлении чего-го куда-то", "soprovod_docx.docx"),
      Document("2", DateTime.parse("2012-02-27"), "Тоже документ", "fin_rep_4.20.xls"),
    ];
  return _docs!;
}