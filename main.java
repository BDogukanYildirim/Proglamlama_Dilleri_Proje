import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;

// Java kodunu renklendirmek için ana sınıfımız, JFrame'den miras alarak bir pencere oluşturuyoruz
public class SyntaxHighlighter extends JFrame {
    private final JTextPane textPane; // Kodun yazılacağı ve renklendirileceği metin alanı
    private final StyledDocument doc; // Metin ve stilleri yöneten belge modeli

    // Kodun farklı parçaları için stil ayarları (anahtar kelimeler, sayılar vs.)
    private final SimpleAttributeSet defaultStyle;
    private final SimpleAttributeSet keywordStyle;
    private final SimpleAttributeSet operatorStyle;
    private final SimpleAttributeSet numberStyle;
    private final SimpleAttributeSet commentStyle;
    private final SimpleAttributeSet stringStyle;

    // Vurgulama işlemlerini geciktirerek yapmak için bir yönetici
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> delayedJob; // Planlanmış vurgulama görevini tutar

    // Kullanıcı yazarken hemen renklendirme yapmamak için kısa bir gecikme (milisaniye)
    private static final int DELAY = 200;

    // Java'nın anahtar kelimeleri, bunları mavi ve kalın yapacağız
    private static final Set<String> keywords = new HashSet<>(Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "null", "package", "private", "protected", "public", "return", "short", "static",
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try",
        "void", "volatile", "while"
    ));

    // Kurucu metod: Pencereyi ve metin alanını hazırlar
    public SyntaxHighlighter() {
        // Pencere başlığı ve boyutunu ayarlıyoruz
        setTitle("Java Kod Vurgulayıcı");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Pencere kapandığında program bitsin
        setLocationRelativeTo(null); // Pencereyi ekranın ortasına hizala

        // Kodun yazılacağı metin alanını oluşturuyoruz
        textPane = new JTextPane();
        doc = textPane.getStyledDocument(); // Metni ve stilleri yönetmek için belgeyi al
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 14)); 

        // Farklı kod sınıfları için renk ve stil ayarları
        defaultStyle = style(Color.BLACK, false); // Normal metin, sade siyah
        keywordStyle = style(Color.BLUE, true); // Anahtar kelimeler (if, class gibi) mavi ve kalın
        operatorStyle = style(Color.RED, false); // Operatörler (+, -, *) kırmızı
        numberStyle = style(Color.GREEN.darker(), false); // Sayılar koyu yeşil
        commentStyle = style(Color.GRAY, true); // Yorum satırları gri ve kalın
        stringStyle = style(new Color(150, 0, 150), false); // Tırnak içindeki metinler mor

        // Vurgulama işlemlerini geciktirerek yapmak için tek iş parçacıklı bir yönetici
        // Böylece kullanıcı yazarken arayüz donmaz
        executor = Executors.newSingleThreadScheduledExecutor();

        // Metin değiştiğinde vurgulamayı tetikle
        doc.addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                delayHighlight(); // Yazarken vurgulamayı geciktir
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                delayHighlight(); 
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                delayHighlight(); // Stil değişimlerinde de aynısını yap
            }
        });

        // Metin alanını kaydırılabilir bir panele koy ve pencereye ekle
        add(new JScrollPane(textPane), BorderLayout.CENTER);
        setVisible(true); 
    }

    // Renk ve kalınlık ayarlarıyla bir stil nesnesi oluşturur
    private SimpleAttributeSet style(Color color, boolean bold) {
        var attr = new SimpleAttributeSet();
        StyleConstants.setForeground(attr, color); // Metnin rengini ayarla
        StyleConstants.setBold(attr, bold); // Gerekirse kalın yap
        return attr;
    }

    // Vurgulamayı hemen yapmamak için bir gecikme planlar
    // Böylece kullanıcı hızlı yazarken arayüz kasmaz
    //32 gb ram bile anlık zorlandı o yüzden bu kısımı aiye sordum bu tavsiyeyi verdi
    private void delayHighlight() {
        // Önceki planlanmış görev varsa iptal et, birikmesin
        if (delayedJob != null) {
            delayedJob.cancel(false);
        }
        // Yeni bir vurgulama görevini kısa bir gecikmeyle planla
        delayedJob = executor.schedule(this::highlightCode, DELAY, TimeUnit.MILLISECONDS);
    }

    // İşin en can alıcı kısmı: Kodun renklendirilmesini yapar
    private void highlightCode() {
        // Arayüzü güncellemek için Swing'in özel iş parçacığında çalış
        SwingUtilities.invokeLater(() -> {
            try {
                // Belgedeki tüm metni al
                String code = doc.getText(0, doc.getLength());

                // İlk olarak her şeyi varsayılan stile sıfırla
                doc.setCharacterAttributes(0, code.length(), defaultStyle, true);

                // Şimdi sırayla kodun farklı parçalarını renklendir
                matchAndStyle("\\b(" + String.join("|", keywords) + ")\\b", keywordStyle, code); // Anahtar kelimeler
                matchAndStyle("[+\\-*/=<>!&|%^]+", operatorStyle, code); // Operatörler
                matchAndStyle("\\b\\d+(\\.\\d+)?\\b", numberStyle, code); // Sayılar
                // Yorumlar için özel regex, hem tek satır (//) hem çok satırlı (/* */) yorumları yakalar
                matchAndStyle("//.*?$|(?s)/\\*.*?\\*/", commentStyle, code, Pattern.MULTILINE | Pattern.DOTALL);
                matchAndStyle("\"(\\\\.|[^\"\\\\])*\"", stringStyle, code); // Tırnak içindeki metinler

                // Süslü parantezlerin dengeli olup olmadığını kontrol et
                if (!bracesBalanced(code)) {
                    System.out.println("Uyarı: Süslü parantezler dengesiz, bir yerlerde hata olabilir!");
                }

            } catch (BadLocationException ex) {
                ex.printStackTrace(); // Hata olursa konsola gönderiyor
            }
        });
    }

    // Regex ile eşleşen metinlere stil uygular (varsayılan flags ile)
    private void matchAndStyle(String pattern, AttributeSet style, String text) {
        matchAndStyle(pattern, style, text, 0);
    }

    // Regex ile eşleşen metinlere stil uygular, özel flags ile
    private void matchAndStyle(String pattern, AttributeSet style, String text, int flags) {
        // Regex desenini derle ve metinde eşleşen yerleri bul
        Pattern p = Pattern.compile(pattern, flags);
        Matcher m = p.matcher(text);
        while (m.find()) {
            // Eşleşen her parçaya belirtilen stili uygula
            doc.setCharacterAttributes(m.start(), m.end() - m.start(), style, true);
        }
    }

    // Süslü parantezlerin ({}) dengeli olup olmadığını kontrol eder
    private boolean bracesBalanced(String code) {
        int open = 0; // Açık süslü parantez sayacı
        boolean inStr = false; // Tırnak içinde miyiz?
        boolean inBlockComment = false; // Çok satırlı yorumda mıyız?
        boolean inLineComment = false; // Tek satırlı yorumda mıyız?

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            // Tek satırlı yorum içindeysek, satır sonuna kadar atla
            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                continue;
            }

            // Çok satırlı yorum içindeysek, kapanış işaretini (*/) ara
            if (inBlockComment) {
                if (c == '*' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
                    inBlockComment = false;
                    i++; // */ için bir karakter daha atla
                }
                continue;
            }

            // kaçış karakteri kontrolü
            if (inStr) {
                if (c == '\\' && i + 1 < code.length()) {
                    i++; // Kaçış karakterinden sonrasını atla
                    continue;
                }
                if (c == '"') inStr = false; // Tırnak kapandı
                continue;
            }

            // Yeni bir tırnak başlıyor mu? (Kaçış karakteri değilse)
            if (c == '"' && (i == 0 || code.charAt(i - 1) != '\\')) {
                inStr = true;
                continue;
            }

            // Yorum başlangıcını kontrol et
            if (c == '/' && i + 1 < code.length()) {
                if (code.charAt(i + 1) == '/') {
                    inLineComment = true; // Tek satırlı yorum başladı
                    i++;
                    continue;
                }
                if (code.charAt(i + 1) == '*') {
                    inBlockComment = true; // Çok satırlı yorum başladı
                    i++;
                    continue;
                }
            }

            // Süslü parantezleri say
            if (c == '{') {
                open++; // Açık parantez
            } else if (c == '}') {
                open--; // Kapanış parantez
            }
        }

        // Parantez dengesine göre dönüt veriyorum
        return open == 0 && !inStr && !inBlockComment;
    }

    // Pencere kapanırken kaynakları temizle
    @Override
    public void dispose() {
        executor.shutdown(); // Yöneticimizi kapatıyoruz
        super.dispose(); // JFrame silinio
    }

    // Yukarıdakileri burada çağırıyoruz
    public static void main(String[] args) {
        // Arayüz işlemlerini Swing le yapıyoruz
        SwingUtilities.invokeLater(SyntaxHighlighter::new);
    }
}
