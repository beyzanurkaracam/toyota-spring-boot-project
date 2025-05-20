package toyota.example.toyota_project.Helpers.Logging;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Farklı formatlardaki timestamp değerlerini standart ISO-8601 formatına dönüştürmek için 
 * kullanılan yardımcı sınıf.
 */
public class TimestampFormatter {
    private static final Logger logger = LogManager.getLogger(TimestampFormatter.class);
    
    // ISO-8601 formatında tam datetime formatter
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    // Kısa format için pattern (örn: 2025-05-19T14)
    private static final Pattern SHORT_FORMAT_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{1,2}$");
    
    // Yerel zaman damgası kullanımını kontrol eden flag
    private static boolean useLocalTimestamp = true;
    
    /**
     * Herhangi bir formattaki timestamp string'ini standart ISO-8601 formatına dönüştürür.
     * 
     * @param timestamp Dönüştürülecek timestamp string değeri
     * @return Standardize edilmiş ISO-8601 formatında timestamp
     */
    public static String standardizeTimestamp(String timestamp) {
        // Sunucu zaman damgaları yerine yerel zaman damgası kullan
        if (useLocalTimestamp) {
            return Instant.now().toString();
        }
        
        if (timestamp == null || timestamp.trim().isEmpty()) {
            logger.warn("Geçersiz timestamp değeri: boş veya null");
            return Instant.now().toString();
        }
        
        try {
            // Kısa format kontrolü (2025-05-19T14)
            if (SHORT_FORMAT_PATTERN.matcher(timestamp).matches()) {
                // Kısa formattaki timestamp'i tamamla
                return completeShortTimestamp(timestamp);
            }
            
            // Zaten ISO-8601 formatında mı kontrol et
            try {
                Instant instant = Instant.parse(timestamp);
                return instant.toString();
            } catch (DateTimeParseException e) {
                // Diğer format durumlarında mevcut zamanı kullan
                logger.warn("Tanınmayan timestamp formatı: {}, varsayılan zaman kullanılacak", timestamp);
                return Instant.now().toString();
            }
        } catch (Exception e) {
            logger.error("Timestamp düzenleme hatası: {}", e.getMessage());
            return Instant.now().toString();
        }
    }
    
    /**
     * Yerel zaman damgası kullanımını ayarlar
     * 
     * @param useLocal true ise tüm zaman damgaları yerel sistem saati ile değiştirilir
     */
    public static void setUseLocalTimestamp(boolean useLocal) {
        useLocalTimestamp = useLocal;
        logger.info("Yerel zaman damgası kullanımı: {}", useLocal);
    }
    
    /**
     * Kısa formattaki (2025-05-19T14) timestamp'i tam ISO-8601 formatına tamamlar
     * 
     * @param shortTimestamp Kısa formattaki timestamp
     * @return Tamamlanmış ISO-8601 formatındaki timestamp
     */
    private static String completeShortTimestamp(String shortTimestamp) {
        try {
            // Kısa format için pattern ve tamamlama
            String[] parts = shortTimestamp.split("T");
            String datePart = parts[0];
            String hourPart = parts[1];
            
            // Saat bilgisi eksik, tamamla
            LocalDateTime localDateTime = LocalDateTime.parse(
                String.format("%sT%s:00:00", datePart, hourPart),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );
            
            // LocalDateTime'i Instant'a çevir
            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
            Instant instant = zonedDateTime.toInstant();
            
            return instant.toString();
        } catch (Exception e) {
            logger.error("Kısa format timestamp tamamlama hatası: {}", e.getMessage());
            return Instant.now().toString();
        }
    }
}