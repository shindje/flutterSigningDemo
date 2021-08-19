import 'package:flutter/material.dart';
import 'package:signing/data/documents.dart';
import 'package:intl/intl.dart';

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