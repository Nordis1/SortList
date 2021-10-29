package com.example.android.sqlitekod_dev_test;

import android.os.Environment;
import android.util.Log;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class File_XLS_Reader extends MainActivity {
    String filename;
    static int firstUsedColumnIs;
    static int lastUsedColumnIs;
    static boolean booldate = false;
    ArrayList<String> arrayListFromXlsFile = new ArrayList<>();
    private static final String TAG = "File_XLS_Reader";

    public File_XLS_Reader(String filename) {
        this.filename = filename;
    }

    public ArrayList<String> readingXLS() {
        Log.i(TAG, "readingXLS: Зашли в чтение XLS файла");

        File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(sdCard, filename);
        //row.getRowNum() - возвращает нормер данной ряда
        //row.getLastCellNum() - возвращает количесво последний использованной колонны
        //row.getFirstCellNum() - возвращает номер колонны где есть первое её спользование
        //if (row != null) { }  - Если в ряду вообще ничего нет возвращает null


        try {
            POIFSFileSystem poi_FileReader = new POIFSFileSystem(new FileInputStream(file));// переводим файл в файл Poi.
            HSSFWorkbook workBook = new HSSFWorkbook(poi_FileReader);// Делаем из него типо книги
            HSSFSheet sheet = workBook.getSheetAt(0); // из этой книги берём лист, 0
            HSSFRow row; // ряд
            HSSFCell cell;// Клетка
            Log.i(TAG, "readingXLS: Получилось получить лист");

            if (!arrayListFromXlsFile.isEmpty()){
                arrayListFromXlsFile.clear();
            }

            int rows = 0;

            //rows = sheet.getPhysicalNumberOfRows();

            if(sheet.getPhysicalNumberOfRows()>0)
                rows = sheet.getLastRowNum() - sheet.getFirstRowNum();

            Log.i(TAG, "readingXLS: Кол-во рядов: " + rows);
            StringBuilder stringBuilder = new StringBuilder();

            for (int r = 0; r < rows; r++) {
                row = sheet.getRow(r);
                if (row != null) {
                    lastUsedColumnIs = row.getLastCellNum(); //получаем последнюю и первую клетку в ряду.
                    firstUsedColumnIs = row.getFirstCellNum();

/*                    for (int c = firstUsedColumnIs; c < lastUsedColumnIs; c++) {
                        cell = row.getCell(c); // Тут начинаем считывать все ячейки с лева на право, или с первой по последнюю
                        if (cell != null) {
                            stringBuilder.append(cell + " ");
                        }
                    }
                    arrayListFromXlsFile.add(stringBuilder.toString());
                    stringBuilder.setLength(0);*/

                    //Это раздел  custom
                    if (lastUsedColumnIs == 11) lastUsedColumnIs = 7; // Убираем лишние Колонны 4 штуки в Xls
                    if (firstUsedColumnIs == 9) lastUsedColumnIs = 7; // убираем ненужную строку в 2 ряду в плане
                    if (lastUsedColumnIs == 1) booldate = true; // Для определения даты


                    for (int c = firstUsedColumnIs; c < lastUsedColumnIs; c++) {
                        cell = row.getCell(c); // Тут начинаем считывать все ячейки с лева на право, или с первой по последнюю

                        if (cell != null ) {
                            String string1 = cell.toString();
                            if (string1.isEmpty()) {
                                continue;
                            }
                                if (!booldate) {
                                    stringBuilder.append(string1);
                                    stringBuilder.append(" ");
                                } else {
                                    stringBuilder.append(string1);
                                }

                            // Your code here
                        }else {
                            continue;
                        }
                    }

                    booldate = false;
                    arrayListFromXlsFile.add(stringBuilder.toString());
                    stringBuilder.setLength(0);
                    //System.out.println();
                    //Раздел Custom


                }
            }
            workBook.close();
            poi_FileReader.close();
            for (int i = 0; i < arrayListFromXlsFile.size(); i++) {
                if (arrayListFromXlsFile.get(i).isEmpty()){
                    arrayListFromXlsFile.remove(i);
                }
            }
        } catch (IOException e) {
            handler.sendEmptyMessage(hSetErrorFileReading);// 13 выводит тост об ошибке чтения файла
            e.printStackTrace();
        }

        return arrayListFromXlsFile;

    }


}
