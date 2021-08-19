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
  Document("1.1", null, "Doc 1"),
  Document("1.2", null, "Doc 2"),
  Document("2", DateTime.parse("2012-02-27"), "Doc 2"),
];