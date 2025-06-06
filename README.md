# Proglamlama dilleri Prjesinin Genel hatları

Gerçek zamanlı olarak Java kodlarını renklendiren, sade ve optimize edilmiş bir **Swing tabanlı Syntax Highlighter**. Detaylı bilgi için pdf belgesi formatındaki makaleden yararlanabilirsiniz.

## ✨ Özellikler

- Gerçek zamanlı sözdizimi renklendirme
- Debounce mekanizması (gereksiz işlem tekrarlarını önler)
- Anahtar kelime, operatör, sayı, string, yorum (// ve /* */) desteği
- Temel söz dizimi kontrolü (örneğin parantez dengelemesi)

## 💡 Kullanılan Renkler

| Öğe         | Renk        | Stil     |
|-------------|-------------|----------|
| Anahtar kelimeler (`if`, `while`, `class`, vb.) | Mavi        | Kalın     |
| Operatörler (`+`, `-`, `=`, vb.)               | Kırmızı     | Normal    |
| Sayılar (`123`, `3.14`)                        | Yeşil       | Normal    |
| Yorumlar (`//`, `/* */`)                       | Gri         | Kalın     |
| String ifadeler (`"Merhaba"`)                  | Mor         | Normal    |
| Varsayılan metin                               | Siyah       | Normal    |

### Çalıştırmak için:

```bash
javac SyntaxHighlighter.java
java SyntaxHighlighter
