package bloodplus.repository;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 실행된 SQL, 프로시저 호출, 트리거 동작 기록.
 * 화면의 "SQL 로그" 페이지(/sql-log)에서 보여주며, 결과보고서 6.3~6.5 캡처에 쓸 수 있다.
 */
public final class SqlLog {
    public enum Kind { QUERY, CALL, CHANGE, TRIGGER, OK, ERROR, INFO }

    public static final class Entry {
        private final String time; private final Kind kind; private final String text;
        Entry(String time, Kind kind, String text) { this.time = time; this.kind = kind; this.text = text; }
        public String getTime() { return time; }
        public String getKind() { return kind.name().toLowerCase(); }
        public String getText() { return text; }
    }

    private static final List<Entry> entries = new ArrayList<>();
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX = 500;
    private static volatile boolean muted = false;

    private SqlLog() {}

    public static synchronized void add(Kind kind, String text) {
        if (muted) return;
        entries.add(new Entry(LocalTime.now().format(TF), kind, text));
        if (entries.size() > MAX) entries.remove(0);
    }
    public static void query(String s)   { add(Kind.QUERY, s); }
    public static void call(String s)    { add(Kind.CALL, s); }
    public static void change(String s)  { add(Kind.CHANGE, s); }
    public static void trigger(String s) { add(Kind.TRIGGER, s); }
    public static void ok(String s)      { add(Kind.OK, s); }
    public static void error(String s)   { add(Kind.ERROR, s); }
    public static void info(String s)    { add(Kind.INFO, s); }

    /** 최근 것이 위로 오도록 */
    public static synchronized List<Entry> recent() { List<Entry> l = new ArrayList<>(entries); Collections.reverse(l); return l; }
    public static synchronized void clear() { entries.clear(); }
    public static void setMuted(boolean m) { muted = m; }
}
