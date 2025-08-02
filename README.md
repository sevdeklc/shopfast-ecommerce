# ShopFast E-ticaret - V1 (PROBLEMLI VERSİYON) ⚠️

> **Bu branch (main) kasıtlı olarak problemli kod içermektedir. Performans sorunlarını deneyimlemek ve analiz etmek için tasarlanmıştır.**

## 🎯 Proje Amacı

Bu proje, e-ticaret sistemlerinde **Black Friday** gibi yoğun dönemlerde yaşanan gerçek performans sorunlarını simüle eder ve bu problemlere çözüm üretmeyi öğretir.

### Senaryo: ShopFast Black Friday Felaketi 💥

ShopFast e-ticaret platformu iPhone 15 Pro için %50 indirim kampanyası başlattı. Binlerce kullanıcı aynı anda sipariş vermeye çalışınca sistem çöktü:

- **Race Condition**: Stok sayıları tutarsız hale geldi
- **N+1 Query Problem**: Database overload
- **Connection Pool Exhausted**: Sistem yanıt veremez hale geldi
- **Memory Leaks**: OutOfMemoryError

## 🛠️ Kullanılan Teknolojiler

- **Java 21** + **Spring Boot 3.x**
- **Spring Data JPA** (Hibernate)
- **H2 Database** (In-memory)
- **Spring Security** (Basic auth)
- **Maven**
- **Spring Boot Actuator** (Monitoring)

## 🚀 Uygulamayı Çalıştırma

### Adım 1: Projeyi İndirin
```bash
git clone https://github.com/sevdeklc/shopfast-ecommerce.git
cd shopfast-ecommerce
```

### Adım 2: Uygulamayı Başlatın
```bash
mvn clean spring-boot:run
```

### Adım 3: Temel Testler
```bash
# Ürünleri listeleyin
curl http://localhost:8080/api/products

# Kampanyaları görün
curl http://localhost:8080/api/campaigns/active

# İlk siparişi verin (yavaş olacak!)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [{"productId": 1, "quantity": 1}],
    "shippingAddress": "Ankara, Turkey"
  }'
```

## 💥 Kritik Problemleri Test Etme

### Test 1: Race Condition Kanıtlama
```bash
# 50 eşzamanlı iPhone siparişi - stok tutarsızlığı oluşturur
for i in {1..50}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{
      "userId": '$((i % 5 + 1))',
      "items": [{"productId": 1, "quantity": 3}],
      "shippingAddress": "Race Condition Test"
    }' &
done
```

### Test 2: Database Durumu Kontrolü
H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:shopfast`
- Username: `sa`, Password: `password`

```sql
-- iPhone stock kontrolü (başlangıç: 100 adet)
SELECT name, stock_quantity FROM products WHERE id = 1;

-- Toplam sipariş sayısı
SELECT COUNT(*) FROM orders;
```

## 🚨 Bu Versiyonda Karşılaşacağınız Kritik Hatalar

### 1. **Race Condition - Stok Tutarsızlığı**
- **Problem**: Eşzamanlı siparişlerde stok sayıları yanlış hesaplanır
- **Sonuç**: 50 sipariş × 3 adet = 150 adet satılmalı, ancak sadece ~30 adet azalır
- **Gerçek Etki**: Müşteriler ödeme yapar ama ürün alamaz

### 2. **N+1 Query Problem**
- **Problem**: Her sipariş için 6+ ayrı SQL sorgusu
- **Sonuç**: Database CPU %100'e çıkar
- **Gözlem**: Terminal'de SQL sorgularının akışını göreceksiniz

### 3. **Connection Pool Exhausted**
- **Problem**: Maksimum 3 database connection (kasıtlı düşük ayar)
- **Sonuç**: 5+ eşzamanlı request'te "Connection timeout"
- **Hata**: `HikariPool-1 - Connection is not available`

### 4. **Poor Performance**
- **Problem**: Sync processing, long transactions
- **Sonuç**: Response time 2-5+ saniye
- **Kullanıcı Deneyimi**: Sistem donmuş gibi görünür

### 5. **Memory Issues**
- **Problem**: Batch size = 1, inefficient processing
- **Sonuç**: Yüksek memory kullanımı, potential OutOfMemoryError

## 📊 Beklenen Test Sonuçları

| Metrik | Sonuç       | Problem |
|--------|-------------|------|
| **Response Time** | ⚠️ MAKUL | Henüz çökmedi ama... |
| **Data Integrity** | ❌ BOZUK     | Stock tutarsızlığı |
| **N+1 Query** | ✅ VAR        | SQL loglarında görüldü |
| **Race Conditions** | ✅ Var       | 126 adet kayıp     |

## 🔗 Optimizasyon Sürecinin Tamamı

Bu problemli versiyonu deneyimledikten sonra, adım adım optimizasyon sürecinin tamamını, çözüm yaklaşımlarını ve performans iyileştirmelerini detaylı Medium yazımda bulabilirsiniz:

### 📝 [Medium Yazısı: "Bir Developer Sohbetinden Doğan Performans Felaketi"](https://medium.com/@kilicsumeyyesevde/bir-developer-sohbetinden-doğan-performans-felaketi-c0f077409406)

Medium yazısında şunları bulacaksınız:
- **Problem Analizi**: Her hatanın detaylı açıklaması
- **Çözüm Stratejileri**: Adım adım optimizasyon yaklaşımları
- **Kod Örnekleri**: Before/After karşılaştırmaları
- **Performance Grafikleri**: Gerçek test sonuçları
- **Best Practices**: Production'da uygulanabilir çözümler
- **Architecture Patterns**: Scalable sistem tasarımı

## ⚠️ Uyarı

Bu kod **kesinlikle production'da kullanılmamalıdır!** Eğitim amaçlı olarak kasıtlı performans problemleri içermektedir.
