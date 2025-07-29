# ShopFast - E-Ticaret Kampanya Sisteminde Trafik Yoğunluğu Yönetimi

Bu projede, trafik yoğunluğu olan sistemlerde yaşanan performans problemlerini ve bu problemlere yönelik çözüm yollarını öğreneceğiz.

## 🎯 Senaryo

**ShopFast E-ticaret Kampanya Sistemi**

ShopFast isimli e-ticaret platformu Black Friday kampanyası düzenliyor. iPhone 15 Pro için %50 indirim kampanyası başladığında binlerce kullanıcı aynı anda sipariş vermeye çalışıyor. Sistem bu ani trafik yükünü kaldıramayıp çöküyor. Bu durum müşteri kaybına ve prestij zedelenmesine yol açıyor.

Bu projede, bu gibi yüksek trafik senaryolarında sistemin çökmesini önlemek için **rate limiting** ve **caching** gibi stratejileri nasıl uygulayacağımızı adım adım öğreneceğiz.

## 🧰 Kullanılan Teknolojiler

### Backend

- **Java 21**
- **Spring Boot 3.x**
- **Spring Data JPA** (Hibernate)
- **Spring Security** (Temel authentication)
- **H2 Database** (Test ortamı için)
- **PostgreSQL** (Production ortamı için)
- **Redis** (Caching ve Rate Limiting için)
- **Maven**

### Test ve Monitoring

- **JMeter** (Load testing)
- **Spring Boot Actuator** (Uygulama metrikleri)
- **Micrometer + Prometheus** (Monitoring altyapısı)

### Containerization

- **Docker**
- **Docker Compose**

## 📌 Amaç

- Aniden artan kullanıcı yüklerine karşı sistemin **kararlılığını** ve **erişilebilirliğini** artırmak.
- **Rate Limiting** kullanarak kaynakların adil kullanımını sağlamak.
- Redis ile **cache ve token bucket algoritmaları** üzerinden sistemin tepkisini hızlandırmak.
- Yük testleriyle sistemin sınırlarını belirlemek ve metrik takibi ile davranışları analiz etmek.

