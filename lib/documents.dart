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