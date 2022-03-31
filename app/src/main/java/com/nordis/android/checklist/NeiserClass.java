package com.nordis.android.checklist;

import android.util.Log;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeiserClass {
    private static final String TAG = "NeiserClass";
    static int pittB80 = 0;
    static int pittB95 = 0;
    static int bogart115 = 0;
    static int bogart95 = 0;
    static int bogart80 = 0;
    static int lounge95 = 0;
    static int lounge110 = 0;
    static int lounge125 = 0;
    static int lounge_k1 = 0;
    static ArrayList<String> mainList = new ArrayList<>();
    static ArrayList<String> mainSupport = new ArrayList<>();
    static int countDate = 0;

    public static ArrayList<String> main(ArrayList<String> comingList) throws IOException {
        if (!mainList.isEmpty() || !mainSupport.isEmpty()) {
            mainList.clear();
            mainSupport.clear();
            pittB95 = 0;
            bogart115 = 0;
            bogart95 = 0;
            bogart80 = 0;
            lounge95 = 0;
            lounge110 = 0;
            lounge125 = 0;
            lounge_k1 = 0;
            countDate = 0;
        }
        origon(comingList);


        Pattern patternName = Pattern.compile("\\d{0,3}? - [a-zA-Z]+?\\s[a-zA-Z]+?"); // для поиска имени  10 - Janek Reemann
        Pattern patternReplay = Pattern.compile("([BD])\\d{0,2}([RL])/"); // для удаления лишнего B90R/
        Pattern patternIns = Pattern.compile("(INSPIRA)"); // для нахождения INSPIRA и укорачивания до INS
        Pattern patternExtra = Pattern.compile("1R/|(?<=\\s)25/|(?<=\\s)3/|(?<=\\s)2/|(?<=\\s)15/|[AB]\\d{1,3}[RL]/|35/|[025][LR]/");
        Pattern patternProto = Pattern.compile("(PROTO PROTO kombinatsioon 1tk;)");
        Pattern patternDate = Pattern.compile("^\\d{1,2}[.]\\d{1,2}[.]\\d{2,4}");
        Matcher m_nameSearching = null;
        Matcher m_B90R_L_Searching = null;
        Matcher m_INSPIRA_Searching = null;
        Matcher m_1R_25_3_2_15_serching = null;
        Matcher m_Proto_searching = null;
        Matcher m_date_Searshing = null;


        String day = "";
        String name = "";
        String line;
        for (int i = 0; i < mainSupport.size(); i++) {
            line = mainSupport.get(i);
            //Log.d(TAG, "main:" + line);
            // Забиваем переменные на совпадения

            m_date_Searshing = patternDate.matcher(line.trim());
            m_nameSearching = patternName.matcher(line.trim()); // c помощью trim убераем пробелы с переди и с зади.
            m_B90R_L_Searching = patternReplay.matcher(line.trim());
            m_INSPIRA_Searching = patternIns.matcher(line.trim());
            m_1R_25_3_2_15_serching = patternExtra.matcher(line.trim());
            m_Proto_searching = patternProto.matcher(line.trim());

            if (m_date_Searshing.find()) {
                day = daysDeterminate(line);        // находим дату и формируем её в удобную форму
                //Log.d(TAG, "main: нашли дату :"+ day +" "+ name);
            } else if (m_nameSearching.find()) {     // ищем Имена
                name = namesDeterminate(line);
            } else {                                 //Работа с регуляками
                line = workWithRegularExpresions(line, m_B90R_L_Searching, m_INSPIRA_Searching, m_1R_25_3_2_15_serching, m_Proto_searching);

                //Удаляем пробелы и подготавливаем строку к записи

                line = deletingExtraSpaces(line.substring(2)); // поддержка со 2 символа, что бы весь номер не выводить.
                //Log.d(TAG, "main:" + line);
                line = name + " " + line + " | | " + day;
                mainList.add(line);
            }
        }


        mainList.add("NB----->>> Padi Pitt/uma/living/Lux A80 - " + pittB80 + " tk; " + "A95 - " + pittB95 + " tk; "); // добавляю количество люксовых подушек
        mainList.add("NB----->>> Padi Bogart/Huurre/Lux A80 - " + bogart80 + " tk; " +
                "A95 - " + bogart95 + " tk; " +
                "A115 - " + bogart115 + " tk"); // добавляю количество люксовых подушек
        mainList.add("NB----->>> Padi LOUNGE/Lux A95 - " + lounge95 + " tk; " +
                "A110 - " + lounge110 + " tk; " +
                "A125 - " + lounge125 + " tk; " +
                "K1 - " + lounge_k1 + " tk");


        String s = mainList.get(0);
        Log.d(TAG, "Кол-во в конце " + mainList.size());
        if (s.contains("TOOTMISPLAAN")) {
            //s = s.substring(17, s.length() - 4);
            s = s.replace("TOOTMISPLAAN","");
            s = s.replace("} :","");
            mainList.set(0, s);
        }


        return mainList;
    }

    private static String workWithRegularExpresions(String line, Matcher m_B90R_L_Searching, Matcher m_INSPIRA_Searching, Matcher m_1R_25_3_2_15_serching, Matcher m_Proto_searching1) {
        while (m_B90R_L_Searching.find()) {
            line = line.replaceAll(m_B90R_L_Searching.group(), "");
        }
        if (m_INSPIRA_Searching.find()) {
            line = line.replace(m_INSPIRA_Searching.group(), "INS");
        }
        while (m_1R_25_3_2_15_serching.find()) {
            line = line.replace(m_1R_25_3_2_15_serching.group(), "");
        }
        if (m_Proto_searching1.find()) {
            line = line.replace(m_Proto_searching1.group(), "");
        }


        return line;
    }

    private static String namesDeterminate(String line) {
        String name = "";
        if (line.contains("Oleksandr Kyselov")) {
            name = "Oleks";
        } else {
            String[] splitName = line.split("\\s"); //с помощью split делим на массив
            name = splitName[2] + "." + splitName[3].charAt(0);
        }
        return name;
    }

    private static String daysDeterminate(String line) {
        countDate++;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String dateString = line.substring(0, 10); // поддержка строки от 0 до 10
        LocalDate localDate = LocalDate.parse(dateString, formatter);
        String b = localDate.getDayOfWeek().toString();
        String day1 = "Day?";
        if (b.contains("MONDAY")) {
            day1 = "Esm";
        } else if (b.contains("TUESDAY")) {
            day1 = "Tei";
        } else if (b.contains("WEDNESDAY")) {
            day1 = "Kol";
        } else if (b.contains("THURSDAY")) {
            day1 = "Nel";
        } else if (b.contains("FRIDAY")) {
            day1 = "Reede";
        } else if (b.contains("SATURDAY")) {
            day1 = "Lau";
        }

        return day1;
    }

    private static String deletingExtraSpaces(String extraLine) {
        extraLine = extraLine.replaceAll("\\s+", " ");
        return extraLine;
    }

    public static void origon(ArrayList<String> comeList) throws IOException {

        Pattern patternLounge = Pattern.compile("(A|K)\\d{1,3}(R|L)?");
        Matcher m_loungeSearching = null;
        Pattern p = Pattern.compile("[a-zA-Z]");
        String ln = null;
        String previousString = null;


        // создаём новый аррай лист
        ArrayList<String> mainLoadList = new ArrayList<>(comeList);

        try {
            Iterator<String> ln1 = mainLoadList.iterator();
            while (ln1.hasNext()) {
                ln = ln1.next();
                if (ln.contains("kokku")) {
                    //Log.d(TAG, "origon: kokku " + ln);
                    ln1.remove();
                }
            }
            for (int i = 0; i < mainLoadList.size(); i++) {
                ln = mainLoadList.get(i);
                if (ln.substring(0, 1).matches(p.toString())) { //Если начинаеться не с номера а с модели
                    Log.d(TAG, "origon: Begins with word " + ln);
                    ln = "   1 " + ln;
                    mainLoadList.set(i,ln);
                }
                if (ln.substring(0, 2).contains("OR")) {   //Origon совмещаем Вариант 3
                    previousString = mainLoadList.get(i - 1);
                    String previos_number = previousString.substring(0, 8);
                    ln = previos_number + ln + previousString.substring(15);
                    mainLoadList.set(i, ln);
                    mainLoadList.remove(i - 1);
                }else if (ln.contains("SHADOW")|| ln.contains("SILVER")){
                    ln = ln+ " {Sterling}";
                    mainLoadList.set(i, ln);
                }else if (ln.contains("STUART")){
                    ln = ln+ " {Stratos}";
                    mainLoadList.set(i, ln);
                }else if (ln.contains("KOLTON")){
                    ln = ln+ " {Samba}";
                    mainLoadList.set(i, ln);
                }else if (ln.contains("CHICAGO")){
                    ln = ln+ " {California}";
                    mainLoadList.set(i, ln);
                }else if (ln.contains("MIAM")) {
                    String lp = ln.substring(18, ln.length()).replaceAll("(MIAM/)([NX])(/k-|/p)", ""); // уберает лишние MIAM/N/k-B85-H6R  1tk;  MIAM/N/k-QL  1tk
                    ln = ln.substring(0, 18);
                    mainLoadList.set(i, ln + lp);
                } else if (ln.contains("FOOTSTOOL") || ln.contains("Footstool")) {
                    ln = ln.replaceAll("FOOTSTOOL|Footstool", "");
                    mainLoadList.set(i, ln);
                } else if (ln.contains("ALFA")) {
                    ln = ln.substring(18, ln.length()).replaceAll("(ALF/)([NX])(/k-|/p)", ""); // уберает лишние ALF/N/k
                    String lp = ln.substring(0, 17);
                    mainLoadList.set(i, lp + ln);
                } else if (ln.contains("IDAHO")) {
                    ln = ln + " { ASPEN }";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("FEATHER")) {
                    ln = ln + " {plan-kr: A95-115; A118-138; A95V-141; B75V-196}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("OREGON")) {
                    ln = ln + " { LIFE }";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("JONAS") || ln.contains("Jonas")) {
                    ln = ln + " { Valmont }";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("HUURRE") || ln.contains("HUURRE/LUX")) {
                    ln = ln + " { BOGART }";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("NASHVILLE B80V")) {
                    ln = ln + " {Aero 80v= 25; 90v= 3}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("NASHVILLE B90V")) {
                    ln = ln + " {Aero 80v= 25; 90v= 3}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("JACKSON")) {
                    ln = ln + " {P = Doug}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("CLARA")) {
                    ln = ln + " {P = Clara + Douglas }";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("EDITIONS")) {
                    ln = ln + " {KR- pitt}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("SANTA FE")) {
                    ln = ln + " {КТ 1H = Vanc}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("OTTAWA")) {
                    ln = ln + " {Montreal}";
                    mainLoadList.set(i, ln);
                }
                if (ln.contains("FOREST") || ln.contains("NICOL") || ln.contains("STOCKHOLM")
                        || ln.contains("BASEL")) {
                    //Log.d(TAG, "origon: зашли менять "+ ln);
                    ln = ln + " { LOUNGE }";
                    mainLoadList.set(i, ln);
                }
                if (ln.contains("LOUNGE")) {

                    m_loungeSearching = patternLounge.matcher(ln);
                    while (m_loungeSearching.find()) {
                        if (m_loungeSearching.group().contains("A95")) {
                            lounge95++;
                        } else if (m_loungeSearching.group().contains("K1")) {
                            lounge_k1++;
                        } else if (m_loungeSearching.group().contains("A125")) {
                            lounge125++;
                        } else if (m_loungeSearching.group().contains("A110")) {
                            lounge110++;
                        }
                        //Log.d(TAG, "Lounge Find " + m_loungeSearching.group());
                    }
                }
                if (ln.contains("BOGART/LUX") || ln.contains("HUURRE/LUX")) {
                    if (ln.contains("B80")) {
                        bogart80 += 2;
                    }
                    if (ln.contains("B95")) {
                        bogart95 += 2;
                    }
                    if (ln.contains("B115")) {
                        bogart115 += 2;
                    }
                }
                if (ln.contains("PITTSBURGH/LUX") || ln.contains("UMA/LUX") || ln.contains("LIVINGSTON/LUX")) {
                    if (ln.contains("B80")) {
                        pittB80 += 2;
                    }
                    if (ln.contains("B95")) {
                        pittB95 += 2;
                    }
                }
                if (ln.contains("F62")) {
                    ln = ln + " {F68}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F82")) {
                    ln = ln + " { F78 }";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F84")) {
                    ln = ln + " {p-F79}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F90")) {
                    ln = ln + " {p-F85}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F96")) {
                    ln = ln + " {p-F86}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F97")) {
                    ln = ln + " {p-F87}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F112")) {
                    ln = ln + " {p_lux-F80; p_soft-F79}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F116")) {
                    ln = ln + " {F-114}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F117")) {
                    ln = ln + " {F-115}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F121")) {
                    ln = ln + " {p-F110}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F122")) {
                    ln = ln + " {p-F111}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F124")) {
                    ln = ln + " {p-F114}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F125")) {
                    ln = ln + " {F-123}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F126")) {
                    ln = ln + " {kr-F124; p-F114}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F127")) {
                    ln = ln + " {F-123}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F128")) {
                    ln = ln + " {kr-F124; p-F114}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F130")) {
                    ln = ln + " {kr-F121; p-F110}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F134")) {
                    ln = ln + " {p-F81}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F136") || ln.contains("F137")) {
                    ln = ln + " {kr-F75; p-F80}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F142")) {
                    ln = ln + " {kr-F121; p-F110}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F155")) {
                    ln = ln + " {p-F138}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F156")) {
                    ln = ln + " {p-F95}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F158")) {
                    ln = ln + " {F157}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F145")) {
                    ln = ln + " {F138}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F146")) {
                    ln = ln + " {F78}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F148")) {
                    ln = ln + " {kr-F75; p-F80}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F151")) {
                    ln = ln + " {F143}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F152")) {
                    ln = ln + " {F149}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F153")) {
                    ln = ln + " {kr-F31; p-F153}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F165")) {
                    ln = ln + " {p-F138}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F139")) {
                    ln = ln + " { F129 }";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F141")) {
                    ln = ln + " { kr-F122; p-111}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F133")) {
                    ln = ln + " { F111 }";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F135")) {
                    ln = ln + " { F104 }";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F80")) {
                    ln = ln + " {Kr - F75; P = F80}";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F132")) {
                    ln = ln + " { F110 }";
                    mainLoadList.set(i, ln);
                } else if (ln.contains("F131")) {
                    ln = ln + " {kr-F122; p-111}";
                    mainLoadList.set(i, ln);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


   /*     for (String s :
                mainLoadList) {
            Log.d(TAG, "origon: s" + s);
        }*/
        mainSupport.addAll(mainLoadList);
    }


}
