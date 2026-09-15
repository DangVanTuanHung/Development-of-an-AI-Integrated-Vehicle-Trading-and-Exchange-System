# External vehicle asking-price reference data

Collected 2026-09-13 from public Chotot Xe search/index pages. No AI-generated prices.
25 observations in 5 model groups. These are asking prices, not completed transactions.
Only factual model/year/ODO/price data is retained, with source URLs; no photos or contact data.

## Collection limitations
Direct HTTP fetching returned 403. Do not bypass it. This first snapshot was reviewed
using publicly readable web results/pages. There is NO unattended crawler or automatic
refresh configured. Some page links show several listings; title and mileage identify
the observed listing. Search caches can differ from the live page.
sourceDate records the source snapshot date (conservatively first day of month where
only a month is known); collectedOn is our collection date, NOT the publication date.
Do not call this a complete sample of listings posted within the last 90 days.

## Updating
Default file: classpath:market-data/prices.json (rebuild backend after editing).
For data updates without rebuilding, configure app.market-prices.resource to a mounted
file: resource (environment APP_MARKET_PRICES_RESOURCE). The service reads it per request.
Use an authorized API/feed or independently review public pages to replace/add records.
Never advance sourceDate/collectedOn unless the data was actually obtained again.
Retain unique vehicle identities across duplicate ads; deduplicate cross-posts before import.
Rebuild with docker compose build backend to run regression checks.

## Selection and statistics
Exact model regex and year (or one unambiguous year in title).
Fuel must agree when provided. USED and LIKE_NEW share the used pool; no synthetic adjustment.
NEW, RESTORED, DAMAGED are never mixed into used data.
Known ODO: within 15,000 km for motorcycles / 30,000 km for cars; unknown ODO remains
unadjusted. No area, registration fee, or trim adjustment. Santa Fe diesel pool explicitly
contains multiple trim levels; users see this in the group label.
Sources older than 90 days or future-dated are excluded; expired/down-payment ads omitted.
IQR 1.5 fences plus half/double-median filter exclude extreme observations (4+ rows).
Require 3 unique remaining observations. Range = interpolated 25th to 75th percentile.
Sparse pools show source references without a range. Never fall back to AI or asking price.

## Review notes
- Camry: 2.5Q only, not 2.0Q or Hybrid. Duplicate phuongneo white 31k/33k ads count once.
- Santa Fe: diesel only; duplicate Tuan Minh and Tan Loi posts count once.
- Exciter: 150 only, not 155 or 50cc. 12xxx mileage left null, not invented.
- Air Blade: 160 only; contradictory 2024/2025 and ADV/Air Blade ads omitted.
- Corolla Cross: external samples are USED and cannot value the application's NEW listing.
- Internal Exciter with DIESEL fuel is rejected until the listing's data is corrected.
