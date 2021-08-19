class Document {
  String docNum = "";
  DateTime docDate = DateTime.now();
  String desc = "";

  Document(String docNum, DateTime? docDate, String desc) {
    this.docNum = docNum;
    if (docDate != null)
      this.docDate = docDate;
    this.desc = desc;
  }
}

List<Document> mocked() => [
  Document("1.1", null, "Документ о чем-то"),
  Document("1.2", null, "Документ о направлении чего-го куда-то"),
  Document("2", DateTime.parse("2012-02-27"), "Тоже документ"),
];