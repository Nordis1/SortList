package com.nordis.android.checklist;

import android.content.ContentResolver;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class File_XLS_Reader extends MainActivity {
    String filename;
    static int maxUsedColumnIs = 1;
    static int minUsedColumnIs = 1;
    static int firstUsedColumnIs;
    static int lastUsedColumnIs;
    static boolean booldate = false;
    ArrayList<String> arrayListFromXlsFile = new ArrayList<>();
    private static final String TAG = "File_XLS_Reader";

    public File_XLS_Reader(String filename) {
        this.filename = filename;
    }

    public ArrayList<String> readingXLS(Context context) throws IOException {
        Log.i(TAG, "readingXLS: Зашли в чтение XLS файла");

        /*File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(sdCard, filename);*/


        //row.getRowNum() - возвращает нормер данной ряда
        //row.getLastCellNum() - возвращает количесво последний использованной колонны
        //row.getFirstCellNum() - возвращает номер колонны где есть первое её спользование
        //if (row != null) { }  - Если в ряду вообще ничего нет возвращает null


        try {
            ContentResolver resolver = context.getContentResolver();
            String readOnlyMode = "r";
            ParcelFileDescriptor parcelFile = resolver.openFileDescriptor(uri, readOnlyMode);


            POIFSFileSystem poi_FileReader = new POIFSFileSystem(new FileInputStream(parcelFile.getFileDescriptor()));// переводим файл в файл Poi.
            HSSFWorkbook workBook = new HSSFWorkbook(poi_FileReader);// Делаем из него типо книги
            HSSFSheet sheet = workBook.getSheetAt(0); // из этой книги берём лист, 0
            HSSFRow row; // ряд
            HSSFCell cell;// Клетка
            Log.i(TAG, "readingXLS: Получилось получить лист");

            if (!arrayListFromXlsFile.isEmpty()) {
                arrayListFromXlsFile.clear();
            }

            int rows = 0;

            //rows = sheet.getPhysicalNumberOfRows();

            if (sheet.getPhysicalNumberOfRows() > 0)
                rows = sheet.getLastRowNum() - sheet.getFirstRowNum();

            Log.i(TAG, "readingXLS: Кол-во рядов: " + rows);
            StringBuilder stringBuilder = new StringBuilder();
            //Ряд и колонна

            for (int r = 0; r < rows; r++) { // Высчитываем максимальную
                row = sheet.getRow(r);
                if (row != null) {
                    lastUsedColumnIs = row.getLastCellNum(); //получаем последнюю и первую клетку в ряду.
                    firstUsedColumnIs = row.getFirstCellNum();
                    if (lastUsedColumnIs > maxUsedColumnIs) maxUsedColumnIs = lastUsedColumnIs;
                    if (firstUsedColumnIs < minUsedColumnIs) minUsedColumnIs = firstUsedColumnIs;
                }
            }
            mColumnmax = maxUsedColumnIs;
            mColumnmin = minUsedColumnIs;
            handler.sendEmptyMessage(hSetCreateDialogFromWhichToWhich);

            //ждём результата Диалога когда укажут с какой по какую коллону считывать
            while (!bool_xlsColumnsWasChosen ) {
                try {
                    if (bool_xlsExecutorCanceled){
                        thread.interrupt();
                    }
                    Log.i(TAG, "readingXLS:  Ждём Когда bool_xlsColumnsWasChosen будет true: " + bool_xlsColumnsWasChosen);
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    Log.i(TAG, "readingXLS: Поток был прерван");
                    bool_xlsExecutorCanceled = false;
                    bool_xlsColumnsWasChosen = false;
                    bool_neiser = false;
                    file_xls_reader = null;
                    handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
                    handler.sendEmptyMessage(hSetProgressBarGone);
                    mProgresscounter = 0;
                    e.printStackTrace();
                }
            }

            //$$$--  Выше мы обрабатывали значения Ниже мы их применяем Логика похожа будь акуратен ---$$$$
            bool_xlsColumnsWasChosen = false;
            Log.i(TAG, "readingXLS:   Внимание!!! Поток идёт дальше! ");
            maxUsedColumnIs = mColumnmax;
            minUsedColumnIs = mColumnmin;

            for (int r = 0; r < rows; r++) {
                row = sheet.getRow(r);
                if (row != null) {
                    lastUsedColumnIs = row.getLastCellNum(); //получаем последнюю и первую клетку в ряду.
                    firstUsedColumnIs = row.getFirstCellNum();


                    //тут устанавливаем максимальные и минимальное значения солонн
                    if (lastUsedColumnIs > maxUsedColumnIs) lastUsedColumnIs = maxUsedColumnIs;
                    if (firstUsedColumnIs < minUsedColumnIs) firstUsedColumnIs = minUsedColumnIs;

                    for (int c = firstUsedColumnIs; c < lastUsedColumnIs; c++) {
                        cell = row.getCell(c); // Тут начинаем считывать все ячейки с лева на право, или с первой по последнюю

                        if (cell != null) {
                            String string1 = cell.toString();
                            if (string1.isEmpty()) {
                                continue;
                            }
                            stringBuilder.append(string1 + " ");
                        }
                    }

                    arrayListFromXlsFile.add(stringBuilder.toString());
                    stringBuilder.setLength(0);

                }
            }
            workBook.close();
            poi_FileReader.close();

            //Закончили считывание и убираем пусты строки.
            for (int i = 0; i < arrayListFromXlsFile.size(); i++) {
                if (arrayListFromXlsFile.get(i).isEmpty()) {
                    arrayListFromXlsFile.remove(i);
                }
            }
        } catch (IOException e) {
            handler.sendEmptyMessage(hSetToastErrorOfFileReading);
            e.printStackTrace();
        }

        //Если файл для Neiser , тогда работаем с ним и возвращаем.
        if (bool_neiser){
            Log.d(TAG, "readingXLS: зашли в создание файла Neiser");
            return NeiserClass.main(arrayListFromXlsFile);
        }

        return arrayListFromXlsFile;

    }

}
