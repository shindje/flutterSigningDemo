import 'dart:io';
import 'dart:typed_data';
import 'package:path_provider/path_provider.dart';

Directory? _extDir;

Future<Directory> _getExtDir() async {
  if (_extDir != null)
    return _extDir!;

  Directory? dir = await getExternalStorageDirectory();
  if (!dir!.existsSync())
    dir.createSync();
  _extDir = dir;
  return dir;
}

Future<File?> checkFile(String? path) async{
  if (path == null)
    return null;

  var file = File((await _getExtDir()).path + "/" + path);
  if (file.existsSync())
    return file;
  else
    return null;
}


Future<File?> checkAndWrite(String? path, Uint8List? data) async{
  if (path == null)
    return null;

  var file = File((await _getExtDir()).path + "/" + path);
  if (!file.existsSync())
    File(file.path).createSync();

  file.writeAsBytes(data!);
  return file;
}

