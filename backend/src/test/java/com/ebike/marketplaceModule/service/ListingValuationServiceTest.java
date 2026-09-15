package com.ebike.marketplaceModule.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.junit.jupiter.api.Test;
class ListingValuationServiceTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final ListingValuationService service = new ListingValuationService(mock(MarketplaceListingService.class), mapper, new ClassPathResource("market-data/prices.json"));
    private final LocalDate today = LocalDate.of(2026,9,13);
    private ListingValuationService.Dataset data() throws Exception {
        try(var in = new ClassPathResource("market-data/prices.json").getInputStream()) {
            return mapper.readValue(in, ListingValuationService.Dataset.class);
        }
    }
    @Test void calculatesFromRealExternalSamplesAndYearInTitle() throws Exception {
        var result=service.calculate(new ListingValuationService.Vehicle("Toyota Camry 2.5Q 2022",null,"LIKE_NEW",null,null),data(),today);
        assertEquals("AVAILABLE",result.status()); assertEquals(8,result.sampleCount());
        assertEquals(new BigDecimal("983000000"),result.low());
        assertEquals(new BigDecimal("1051250000"),result.high());
        assertEquals("EXTERNAL_ASKING_PRICES",result.source());
        assertTrue(result.references().stream().allMatch(r -> r.sourceUrl().startsWith("https://xe.chotot.com/")));
    }
    @Test void rejectsWrongVariantYearFuelAndNewUsedMixing() throws Exception {
        for(var vehicle : List.of(
            new ListingValuationService.Vehicle("Hyundai Santa Fe Xang 2022",2022,"USED",null,null),
            new ListingValuationService.Vehicle("Toyota Camry Hybrid 2022",2022,"USED","HYBRID",null),
            new ListingValuationService.Vehicle("Toyota Camry 2.5Q 2022",2023,"USED",null,null),
            new ListingValuationService.Vehicle("Toyota Camry 2.5Q",null,"USED",null,null),
            new ListingValuationService.Vehicle("Toyota Corolla Cross Hybrid 2024",2024,"NEW","HYBRID",80),
            new ListingValuationService.Vehicle("Yamaha Exciter 150",2025,"USED","DIESEL",15))) {
            var result=service.calculate(vehicle,data(),today); assertNull(result.low()); assertNull(result.high());
        }
    }
    @Test void staleOrSparseSamplesNeverProduceInventedPrices() throws Exception {
        assertNull(service.calculate(new ListingValuationService.Vehicle("Toyota Camry 2.5Q 2022",2022,"USED",null,null),data(),today.plusDays(91)).low());
        var sparse=service.calculate(new ListingValuationService.Vehicle("Yamaha Exciter 150",2025,"USED","GASOLINE",null),data(),today);
        assertEquals(2,sparse.sampleCount()); assertNull(sparse.low()); assertEquals(2,sparse.references().size());
    }
    @Test void duplicateIdsDoNotIncreaseSampleCount() throws Exception {
        var dataset=data(); var repeated=new ArrayList<>(dataset.observations()); repeated.addAll(dataset.observations());
        var result=service.calculate(new ListingValuationService.Vehicle("Toyota Camry 2.5Q 2022",2022,"USED",null,null),
            new ListingValuationService.Dataset(1,dataset.groups(),repeated),today);
        assertEquals(8,result.sampleCount());
    }
}
