import 'dart:io';

class Document {
  String docNum;
  DateTime docDate;
  String desc;
  String? assetPath;
  File? file;
  int? fileLength;
  File? signFile;
  int? signFileLength;

  Document(this.docNum, this.docDate, this.desc, this.assetPath);
}

List<Document> mocked() => [
  Document("1.1", DateTime.now(), "Документ о чем-то", "soprovod.pdf"),
  Document("1.2", DateTime.parse("2020-06-15"), "Документ о направлении чего-го куда-то", ""),
  Document("2", DateTime.parse("2012-02-27"), "Тоже документ", ""),
];