# ShopFast E-ticaret - V2 Basic Fixes  

> **Bu branch V1'deki kritik performans problemlerini çözen optimize edilmiş versiyondur.**

## 🎯 Bu Versiyonun Amacı

V1'de kasıtlı olarak oluşturduğumuz performans problemlerini temel optimizasyon teknikleri ile çözüyoruz. Büyük mimari değişiklikler yapmadan, mevcut kodda kritik düzeltmeler yaparak sistemi stabil hale getiriyoruz.  

## Kurulum ve Çalıştırma  
```bash
git checkout v2-basic-fixes
mvn clean compile
mvn spring-boot:run
```

## ✅ Tamamlanan İyileştirmeler  
### 1. Connection Pool & Server Optimizasyonu  
Problem: Düşük connection pool (3) ve thread sayısı (5) ile eşzamanlı isteklerde darboğaz  

Sonuç:  
⚡ 20 eşzamanlı istek süresi: 1.02s → 0.35s (%66 hızlanma)  
✅ Connection timeout hataları ortadan kalktı   
📈 Concurrent user kapasitesi arttı  

### 2. Race Condition Çözümü  
Problem: Stok güncellemelerinde thread safety problemi - eşzamanlı siparişlerde stok tutarsızlığı  

Test Sonuçları (50 eşzamanlı sipariş × 3 adet = 150 adet satış):  
V1 (Öncesi): 150 stok → 141 stok (sadece 9 adet azaldı, 141 adet kayıp)  
V2 (Sonrası): 150 stok → 0 stok (tam 150 adet satış, %100 doğru)  

Sonuç:  
✅ Stok tutarsızlığı tamamen çözüldü  
📊 %100 doğru stok hesaplaması  
🔒 Thread-safe sipariş işleme  

### 3. N+1 Query Problem Çözümü  
Problem: Her order item için ayrı Product ve Campaign sorgusu - 3 ürün için 6 ekstra query  

Test Sonuçları (3 farklı ürün siparişi):  
V1 (Öncesi): 13 SQL query (her ürün için ayrı Product + Campaign sorgusu)  
V2 (Sonrası): 9 SQL query (batch fetching ile tek sorgu)  

Sonuç:  
📉 SQL query sayısı %31 azaldı (13 → 9)  
⚡ Database round-trip sayısı azaldı  
🎯 Batch processing ile optimize edilmiş veri çekme  

### 4. User Fetching Optimizasyonu  
Problem: Her siparişte tüm User objesi database'den çekiliyor (8 field)  

Sonuç:  
⚡ User query overhead'i kaldırıldı  
💾 Memory kullanımı optimize edildi  
🎯 Sadece gerekli veri çekiliyor  

### 5. Slow Operations & Clean Architecture  
Problem: Simülasyon gecikmeleri performansı düşürüyor.  

Sonuç:  
⚡ 300ms simülasyon gecikmesi kaldırıldı  
🏗️ Clean architecture ile MapStruct  
📈 Enterprise-grade code structure  

## V1 → V2 Kapsamlı İyileştirme Tablosu  

| Problem | V1 Durumu | V2 Durumu | İyileştirme |
|---------|-----------|-----------|-------------|
| **Connection Pool** | Max 3 connection | Max 20 connection | %66 hızlanma |
| **Thread Pool** | Max 5 thread | Max 50 thread | 10x artış |
| **Race Conditions** | Stok tutarsızlığı | %100 doğru hesap | Tamamen çözüldü |
| **N+1 Query** | 13 SQL query | 9 SQL query | %31 azalma |
| **User Fetching** | Tüm User objesi | Sadece email | Query overhead kaldırıldı |
| **Slow Operations** | 300ms delay | 0ms delay | %100 hızlanma |
| **Architecture** | Manuel mapping | MapStruct | Enterprise-grade |  
 

### V2 Tamamlandı: Tüm temel performans problemleri çözüldü